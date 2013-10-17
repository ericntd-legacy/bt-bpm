package sg.edu.dukenus.bpmomron;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import sg.edu.dukenus.securesms.crypto.MyKeyUtils;
import sg.edu.dukenus.securesms.sms.SmsReceiver;
import sg.edu.dukenus.securesms.sms.SmsSender;
import sg.edu.dukenus.securesms.utils.MyUtils;
import sg.edu.nus.omronhealth.spp.BPMeasurementData;
import sg.edu.nus.omronhealth.spp.OmronMeasurementData;

import sg.edu.nus.omronhealth.spp.OmronMeasurementData;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class UploadActivity extends Activity {
	// debugging
	private static final String TAG = "UploadActivity";
	private final boolean D = true;

	private SmsSender smsSender;

	private static final int MAX_SMS_MESSAGE_LENGTH = 160;

	// Layout Views
	private ListView mConversationView;

	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	private ArrayList<OmronMeasurementData> measurementDataList;

	// intent
	final String UPLOAD_FROM_DB = "UploadFromDB";

	private SmsReceiver mSmsReceiver;
	private SmsReceiver sentReportReceiver;
	private SmsReceiver deliveredReportReceiver;

	// actions/ intent filters
	private final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private final String SMS_SENT = "SMS_SENT";
	private final String SMS_DELIVERED = "SMS_DELIVERED";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.w(TAG, "onCreate");
		setContentView(R.layout.activity_upload);

		/*
		 * Check existing public keys of the server +6584781395
		 */
		/*
		 * SharedPreferences prefs1 = getSharedPreferences(
		 * MyKeyUtils.DEFAULT_CONTACT_NUM, Context.MODE_PRIVATE); String
		 * contactPubMod = prefs1.getString(MyKeyUtils.PREF_PUBLIC_MOD,
		 * MyKeyUtils.DEFAULT_PREF); String contactPubExp =
		 * prefs1.getString(MyKeyUtils.PREF_PUBLIC_EXP,
		 * MyKeyUtils.DEFAULT_PREF); if (!contactPubMod.isEmpty() &&
		 * !contactPubExp.isEmpty()) { Log.i(TAG, "public key stored for " +
		 * MyKeyUtils.DEFAULT_CONTACT_NUM + " with mod: " + contactPubMod +
		 * " and exp: " + contactPubExp); } else { Log.w(TAG,
		 * "public key not found for " + MyKeyUtils.DEFAULT_CONTACT_NUM +
		 * " so where did it go?"); }
		 */

		// SharedPreferences preferences =
		// PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences prefs = getSharedPreferences(
				SettingsActivity.PREF_BPM, Context.MODE_PRIVATE);

		String desNumStored = prefs
				.getString(SettingsActivity.PREF_DES_NUM, "");

		if (!desNumStored.isEmpty()) {
			EditText phoneNum = (EditText) findViewById(R.id.phoneNum);
			phoneNum.setText(desNumStored);
		}

		// TODO check if keys available for such number

		prepareMeasurements();

		mSmsReceiver = new SmsReceiver();
		sentReportReceiver = new SmsReceiver();
		deliveredReportReceiver = new SmsReceiver();
	}

	private void prepareMeasurements() {
		// Get the message from the intent
		Intent intent = getIntent();
		String act = intent.getAction();
		/*
		 * if the measurement is retrieved directly from the app's
		 * sharedpreferences
		 */
		if (act.equals(UpdateMeasurementActivity.UPLOAD_FROM_DB)) {
			Log.w(TAG,
					"getting the latest measurement directly from the app's sharedpreferences");
			Button btnSendSecureSMS = (Button) findViewById(R.id.BtnSendSecureSMS);
			btnSendSecureSMS.setEnabled(true);
			Button btnSendPlainText = (Button) findViewById(R.id.BtnSendPlainText);
			btnSendPlainText.setEnabled(true);

			// TODO: manually construct measurement data from the app's
			// sharedpreferences
			measurementDataList = new ArrayList<OmronMeasurementData>();

			BPMeasurementData measurement = retrieveLatestMeasurement();

			measurementDataList.add(measurement);
			setup();
			mConversationArrayAdapter.add(measurement.toString());

		} else {

			measurementDataList = getIntent().getParcelableArrayListExtra(
					UpdateMeasurementActivity.MEASUREMENT_DATA_ARRAY);

			setup();

			if (!measurementDataList.isEmpty()) {
				// enable send btns
				Button btnSendPlainText = (Button) findViewById(R.id.BtnSendPlainText);
				Button btnSendSecureSMS = (Button) findViewById(R.id.BtnSendSecureSMS);

				btnSendPlainText.setEnabled(true);
				btnSendSecureSMS.setEnabled(true);

			}
			for (OmronMeasurementData measurementData : measurementDataList) {

				mConversationArrayAdapter.add(measurementData.toString());
			}
		}
	}

	private BPMeasurementData retrieveLatestMeasurement() {
		SharedPreferences prefs = getSharedPreferences("LatestMeasurement",
				Context.MODE_PRIVATE);

		char UNo = prefs.getString("UNo", "A").charAt(0);
		int YY = prefs.getInt("YY", 12);
		int MM = prefs.getInt("MM", 10);
		int DD = prefs.getInt("DD", 16);
		int hh = prefs.getInt("hh", 17);
		int mm = prefs.getInt("mm", 57);
		int ss = prefs.getInt("ss", 11);
		int unit = prefs.getInt("unit", 0);
		int sys = prefs.getInt("sys", 120);
		int dia = prefs.getInt("dia", 80);
		int pulse = prefs.getInt("pulse", 70);
		int bodyMovementFlag = prefs.getInt("bodyMovementFlag", 0);
		int irregPulseFlag = prefs.getInt("irregPulseFlag", 0);
		BPMeasurementData measurement = new BPMeasurementData(UNo, YY, MM, DD,
				hh, mm, ss, unit, sys, dia, pulse, bodyMovementFlag,
				irregPulseFlag); // all values such as
		// UNo, systolic or
		// diastolic are
		// supposed to be
		// stored in the
		// app's
		// sharedpreferences
		// everytime a
		// measurement is
		// grabbed from the
		// BPM via Bluetooth

		return measurement;
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.w(TAG, "onStart");
		
		/*
		 * Checking the phone's key as well as server's key
		 */
		Log.w(TAG, "checking the server's key as well as the phone's key");
		MyKeyUtils.checkKeys(SettingsActivity.PREF_BPM, SettingsActivity.PREF_DES_NUM, getApplicationContext());
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.w(TAG, "onResume");
		/*
		 * receiver that receives SMSs
		 */
		IntentFilter iff = new IntentFilter();
		iff.addAction(SMS_RECEIVED);
		this.registerReceiver(this.mSmsReceiver, iff);

		// to receive delivery report of sms

		IntentFilter if1 = new IntentFilter();
		if1.addAction(SMS_SENT);
		this.registerReceiver(this.sentReportReceiver, if1);

		IntentFilter if2 = new IntentFilter();
		if2.addAction(SMS_DELIVERED);
		this.registerReceiver(this.deliveredReportReceiver, if2);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.w(TAG, "onPause");
		this.unregisterReceiver(this.mSmsReceiver);
		this.unregisterReceiver(this.sentReportReceiver);
		this.unregisterReceiver(this.deliveredReportReceiver);
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.w(TAG, "onStop");
	}

	@Override
	public void onDestroy() {
		Log.w(TAG, "onDestroy");
		super.onDestroy();
	}

	private void setup() {
		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Enable menu options which includes
		// 1. Settings
		getMenuInflater().inflate(R.menu.activity_upload, menu);
		return true;
	}

	public void handlePlainText(View view) {

		// SharedPreferences preferences =
		// PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences prefs = getSharedPreferences(
				SettingsActivity.PREF_BPM, Context.MODE_PRIVATE);
		boolean legacySMS = prefs.getBoolean(SettingsActivity.PREF_LEGACY_SMS,
				SettingsActivity.DEFAULT_LEGACY_SMS);

		// String idStored = preferences.getString("deviceId", "NO_ID");
		// String macAddr = prefs.getString(SettingActivity.PREF_MAC_ADDR,
		// SettingActivity.DEFAULT_MAC_ADDR);

		// String msg = new
		// StringBuilder(SettingActivity.APP_CODE).append(" ").append("@MAC="+macAddr+"@ ").toString();
		BPMeasurementData tmp = null;
		String measurementStr = "";
		String prefix = "";

		for (OmronMeasurementData measurementData : measurementDataList) {
			tmp = (BPMeasurementData) measurementData;
			// String dateTime = tmp.getDateTime();
			// msg = new
			// StringBuilder(msg).append("@datetime="+dateTime+"@ ").toString();
			// String measurement =
			// "@systolic="+tmp.getSys()+"@ @diastolic="+tmp.getDia()+"@ @HR="+tmp.getPulse()+"@";
			// msg = new
			// StringBuilder(msg).append(measurement).append(" ").toString();
		}

		if (tmp != null) {
			// msg = constructSMS(SettingActivity.PREF_BPM,-1, tmp.getSys(),
			// tmp.getDia(),
			// tmp.getPulse(), -1, tmp.getDateTimeMySQL());
			if (legacySMS) {
				measurementStr = constructLegacySMS(SettingsActivity.PREF_BPM, -1,
						tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1,
						tmp.getDateTimeMySQL());
				prefix = "From ";
		
			} else {
				measurementStr = constructSMS(SettingsActivity.PREF_BPM, -1, tmp.getSys(),
						tmp.getDia(), tmp.getPulse(), -1,
						tmp.getDateTimeMySQL());
				prefix = "gmstelehealth ";
			}
		}

		Toast.makeText(this,
				"Sending measurement string: '" + measurementStr + "' - length: " + measurementStr.length(),
				Toast.LENGTH_LONG).show();
		if (D)
			Log.w(TAG, "Final measurement string is: '" + measurementStr + "'");

		EditText phoneNumberField = (EditText) findViewById(R.id.phoneNum);
		String contactNum = phoneNumberField.getText().toString();
		//sendSMS(phoneNumber, msg);
		
		if (contactNum != null || !contactNum.isEmpty()) {
			smsSender = new SmsSender(contactNum);
			smsSender.sendPlainText(getApplicationContext(), measurementStr, prefix);
		}
	}

	// Format of the SMS:
	// "gmstelehealth @MAC=[mac address of the pulse oximeter]@ @datetime=[yyyy-mm-dd HH:mm:ss]@ @systolic=[systolic]@ @diastolic=[diastolic]@ @weight=[weight]@ @hr=[hr]@ @spo2=[spo2]@"
	private String constructSMS(String pref, int weight, int systolic,
			int diastolic, int pulse, int spo2, String measurementDate) {
		// Code for this app
		String msg = "";

		// MAC address of the health device
		String macAddr = SettingsActivity.DEFAULT_MAC_ADDR;

		SharedPreferences tmp = getSharedPreferences(pref, Context.MODE_PRIVATE);
		if (tmp != null) {
			macAddr = tmp.getString(SettingsActivity.PREF_MAC_ADDR,
					SettingsActivity.DEFAULT_MAC_ADDR);
			if (D)
				Log.w(TAG, "mac address of the pulse oximeter is " + macAddr);
		}
		msg = msg + "@mac=" + macAddr + "@ ";

		// Date and time of the measurement
		String dt = new String();
		if (measurementDate == null) {
			// Grab the Android system date and time
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			dt = dateFormat.format(date);
		} else
			dt = measurementDate;
		msg = msg + "@datetime=" + dt + "@ ";

		// Measurement data e.g. weight, systolic, pulse
		if (weight >= 0)
			msg = msg + "@weight=" + weight + "@ ";
		if (systolic >= 0)
			msg = msg + "@systolic=" + systolic + "@ ";
		if (diastolic >= 0)
			msg = msg + "@diastolic=" + diastolic + "@ ";
		if (pulse >= 0)
			msg = msg + "@hr=" + pulse + "@ ";
		if (spo2 >= 0)
			msg = msg + "@spo2=" + spo2 + "@ ";

		// msg = "gmstelehealth @MAC="+ macAddr
		// +"@ @datetime="+dt+"@ @HR="+HR+"@ @spO2="+SPO2+"@";
		msg = msg.trim();

		if (D)
			Log.w(TAG, "The exact SMS message was '" + msg + "'");
		return msg;
	}

	// From MAC; @systolic@ = 114; @diastolic@ = 83; @HR@ = 81;
	private String constructLegacySMS(String pref, int weight, int systolic,
			int diastolic, int pulse, int spo2, String measurementDate) {
		String msg = "";

		// MAC address of the health device
		String macAddr = SettingsActivity.DEFAULT_MAC_ADDR;

		SharedPreferences tmp = getSharedPreferences(pref, Context.MODE_PRIVATE);
		if (tmp != null) {
			macAddr = tmp.getString(SettingsActivity.PREF_MAC_ADDR,
					SettingsActivity.DEFAULT_MAC_ADDR);
			if (D)
				Log.w(TAG, "mac address of the health device is " + macAddr);
		}
		msg = msg + macAddr + "; ";

		// Measurement data e.g. weight, systolic, pulse
		// if (weight>=0) msg = msg + "@weight="+weight+"@ ";
		if (systolic >= 0)
			msg = msg + "@systolic@ = " + systolic + "; ";
		if (diastolic >= 0)
			msg = msg + "@diastolic@ = " + diastolic + "; ";
		if (pulse >= 0)
			msg = msg + "@HR@ = " + pulse + "; ";
		if (spo2 >= 0)
			msg = msg + "@spO2@ = " + spo2 + "; ";

		// msg = "gmstelehealth @MAC="+ macAddr
		// +"@ @datetime="+dt+"@ @HR="+HR+"@ @spO2="+SPO2+"@";
		msg = msg.trim();

		if (D)
			Log.w(TAG, "The exact SMS message was '" + msg + "'");

		return msg;
	}

	public void handleSecureSMS(View view) {
		EditText phoneNumberField = (EditText) findViewById(R.id.phoneNum);
		String contactNum = phoneNumberField.getText().toString();
		RSAPublicKeySpec pubKeySpec = MyKeyUtils.getRecipientsPublicKey(
				contactNum, getApplicationContext());
		if (pubKeySpec == null) {
			Toast.makeText(
					getApplicationContext(),
					"Key not found, please double-check server's number. Server's number must contain the plus sign, country code and no space in between",
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "Key not found, please double-check server's number");
		} else {
			sendSecureSMS();
		}
	}

	public void sendSecureSMS() {
		// SharedPreferences preferences =
		// PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences prefs = getSharedPreferences(
				SettingsActivity.PREF_BPM, Context.MODE_PRIVATE);
		boolean legacySMS = false;

		EditText phoneNumberField = (EditText) findViewById(R.id.phoneNum);
		String contactNum = phoneNumberField.getText().toString();
		Log.w(TAG, "server's number is " + contactNum);

		// String idStored = preferences.getString("deviceId", "NO_ID");
		BPMeasurementData tmp = null;
		String measurementStr = "";

		for (OmronMeasurementData measurementData : measurementDataList) {
			tmp = (BPMeasurementData) measurementData;
			// String dateTime = tmp.getDateTime();
			// msg = new
			// StringBuilder(msg).append("@datetime="+dateTime+"@ ").toString();
			// String measurement =
			// "@systolic="+tmp.getSys()+"@ @diastolic="+tmp.getDia()+"@ @HR="+tmp.getPulse()+"@";
			// msg = new
			// StringBuilder(msg).append(measurement).append(" ").toString();
		}

		if (tmp != null) {
			// msg = constructSMS(SettingActivity.PREF_BPM,-1, tmp.getSys(),
			// tmp.getDia(),
			// tmp.getPulse(), -1, tmp.getDateTimeMySQL());
			if (legacySMS)
				measurementStr = constructLegacySMS(SettingsActivity.PREF_BPM,
						-1, tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1,
						tmp.getDateTimeMySQL());
			else
				measurementStr = constructSMS(SettingsActivity.PREF_BPM, -1,
						tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1,
						tmp.getDateTimeMySQL());
		}

		Log.w(TAG, "measurement string: " + measurementStr);

		// Toast.makeText(this, msg + "  length: " + msg.length(),
		// Toast.LENGTH_LONG).show();
		if (contactNum != null || !contactNum.isEmpty()) {
			smsSender = new SmsSender(contactNum);
			smsSender.sendSecureSMS(getApplicationContext(), measurementStr);
		}

	}

	// http://mobiforge.com/developing/story/sms-messaging-android
	private void sendSMS(String phoneNumber, String message) {
		Log.d(TAG, "SendSMS :: phone: " + phoneNumber);
		Log.d(TAG, "SendSMS :: msg: " + message);

		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
				SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
				new Intent(DELIVERED), 0);

		int length = message.length();
		ArrayList<PendingIntent> sentPIList = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliveredPIList = new ArrayList<PendingIntent>();
		sentPIList.add(sentPI);
		deliveredPIList.add(deliveredPI);

		if (length > MAX_SMS_MESSAGE_LENGTH) {
			int numOfSms = length / MAX_SMS_MESSAGE_LENGTH + 1; // int maths
			for (int i = 1; i < numOfSms; i++) {
				PendingIntent sentPIi = PendingIntent.getBroadcast(this, 0,
						new Intent(SENT), 0);
				PendingIntent deliveredPIi = PendingIntent.getBroadcast(this,
						0, new Intent(DELIVERED), 0);

				sentPIList.add(sentPIi);
				deliveredPIList.add(deliveredPIi);

			}
		}

		/*
		 * //---when the SMS has been sent--- registerReceiver(new
		 * BroadcastReceiver(){
		 * 
		 * @Override public void onReceive(Context arg0, Intent arg1) { switch
		 * (getResultCode()) { case Activity.RESULT_OK:
		 * Toast.makeText(getBaseContext(), "SMS sent",
		 * Toast.LENGTH_SHORT).show(); break; case
		 * SmsManager.RESULT_ERROR_GENERIC_FAILURE:
		 * Toast.makeText(getBaseContext(), "Generic failure",
		 * Toast.LENGTH_SHORT).show(); break; case
		 * SmsManager.RESULT_ERROR_NO_SERVICE: Toast.makeText(getBaseContext(),
		 * "No service", Toast.LENGTH_SHORT).show(); break; case
		 * SmsManager.RESULT_ERROR_NULL_PDU: Toast.makeText(getBaseContext(),
		 * "Null PDU", Toast.LENGTH_SHORT).show(); break; case
		 * SmsManager.RESULT_ERROR_RADIO_OFF: Toast.makeText(getBaseContext(),
		 * "Radio off", Toast.LENGTH_SHORT).show(); break; } } }, new
		 * IntentFilter(SENT));
		 * 
		 * //---when the SMS has been delivered--- registerReceiver(new
		 * BroadcastReceiver(){
		 * 
		 * @Override public void onReceive(Context arg0, Intent arg1) { switch
		 * (getResultCode()) { case Activity.RESULT_OK:
		 * Toast.makeText(getBaseContext(), "SMS delivered",
		 * Toast.LENGTH_SHORT).show(); break; case Activity.RESULT_CANCELED:
		 * Toast.makeText(getBaseContext(), "SMS not delivered",
		 * Toast.LENGTH_SHORT).show(); break; } } }, new
		 * IntentFilter(DELIVERED));
		 */

		SmsManager smsManager = SmsManager.getDefault();
		// sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

		// int length = message.length();
		if (length > MAX_SMS_MESSAGE_LENGTH) {
			ArrayList<String> messagelist = smsManager.divideMessage(message);
			smsManager.sendMultipartTextMessage(phoneNumber, null, messagelist,
					sentPIList, deliveredPIList);
		} else
			smsManager.sendTextMessage(phoneNumber, null, message, sentPI,
					deliveredPI);
	}

	/*
	 * a receiver here to receive key exchange message from server something
	 * like a service or asynctask has to request for the key from the server
	 * first of course
	 */
	public void receivedBroadcast(Intent i, String contactNum) {

		/*SharedPreferences prefs = getSharedPreferences(contactNum,
				Context.MODE_PRIVATE);
		String pubMod = prefs.getString(MyKeyUtils.PREF_PUBLIC_MOD,
				MyKeyUtils.DEFAULT_PREF);

		Log.w(TAG, "public modulus updated to " + pubMod);
		Log.w(TAG,
				"public key found for contact "
						+ contactNum
						+ " "
						+ (MyKeyUtils.getRecipientsPublicKey(contactNum,
								getApplicationContext()) == null));
		Toast.makeText(getApplicationContext(), "ready to send secure message",
				Toast.LENGTH_LONG).show();*/
	}

}
