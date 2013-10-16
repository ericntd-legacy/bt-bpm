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
			Button encodedSms = (Button) findViewById(R.id.btn_encodedSms);
			encodedSms.setEnabled(true);

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
				Button normalSms = (Button) findViewById(R.id.btn_normalSms);
				Button encodedSms = (Button) findViewById(R.id.btn_encodedSms);

				normalSms.setEnabled(true);
				encodedSms.setEnabled(true);

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

		/*
		 * Checking the phone's key as well as server's key
		 */
		Log.w(TAG, "checking the server's key as well as the phone's key");
		MyKeyUtils.checkKeys(getApplicationContext());
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

	public void onNormalSms(View view) {

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
		String msg = "";

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
				msg = constructLegacySMS(SettingsActivity.PREF_BPM, -1,
						tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1,
						tmp.getDateTimeMySQL());
			else
				msg = constructSMS(SettingsActivity.PREF_BPM, -1, tmp.getSys(),
						tmp.getDia(), tmp.getPulse(), -1,
						tmp.getDateTimeMySQL());
		}

		Toast.makeText(this,
				"Sending a normal sms " + msg + "  length: " + msg.length(),
				Toast.LENGTH_LONG).show();
		if (D)
			Log.w(TAG, "the exact SMS message was '" + msg + "'");

		EditText phoneNumberField = (EditText) findViewById(R.id.phoneNum);
		String phoneNumber = phoneNumberField.getText().toString();
		sendSMS(phoneNumber, msg);
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
		String msg = "From ";

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

		SharedPreferences prefs = getSharedPreferences(contactNum,
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
				Toast.LENGTH_LONG).show();
	}

	private BroadcastReceiver smsListenerUploadActivity = new BroadcastReceiver() {
		// SharedPreferences
		private final String PREFS = "MyKeys";
		private final String PREF_PUBLIC_MOD = "PublicModulus";
		private final String PREF_PUBLIC_EXP = "PublicExponent";
		private final String PREF_PRIVATE_MOD = "PrivateModulus";
		private final String PREF_PRIVATE_EXP = "PrivateExponent";

		// private final String PREF_PHONE_NUMBER = "PhoneNumber";
		// private final String PREF_RECIPIENT_NUM = "PhoneNumber";

		private final String DEFAULT_PREF = "";

		// sms codes
		private final String KEY_EXCHANGE_CODE = "keyx";
		private final String HEALTH_SMS = "gmstelehealth";

		@Override
		public void onReceive(Context context, Intent intent) {
			// intent.putExtra(INTENT_SOURCE,
			// "this comes from the sms receiver");

			// updating a sharedpreferences boolean value, hopefully the
			// activity can see the updated value after that
			SharedPreferences prefs = getSharedPreferences("prefs",
					Context.MODE_PRIVATE);
			SharedPreferences.Editor prefseditor = prefs.edit();
			prefseditor.putBoolean("receivedsms", true);
			prefseditor.commit();

			// MainActivity.this.receivedBroadcast(intent);

			Map<String, String> msg = retrieveMessages(intent);

			Log.i(TAG, "we received " + msg.size() + " messages in total");
			if (msg != null) {
				for (String sender : msg.keySet()) {
					String message = msg.get(sender);

					Log.i(TAG, "message received is " + message);

					handleMessage(message, sender, context, intent);
				}
			}
		}

		private void handleMessage(String message, String sender,
				Context context, Intent i) {
			if (message.startsWith(KEY_EXCHANGE_CODE)) {
				Log.i(TAG, "message received is a key exchange message");
				handleKeyExchangeMsg(message, sender, context, i);
			} else if (message.startsWith(HEALTH_SMS)) {
				Log.i(TAG, "received a secure text message");
				// TODO handle secure text message
				handleEncryptedMsg(message, sender, context);
			} else {
				Log.i(TAG, "Message not recognised, not doing anything");
			}
		}

		/*
		 * the sender here is actually the recipient of future encrypted text
		 * messages the recipient's public key will be used to encrypt the
		 * future text messages so that the recipient can use his/ her private
		 * key to decrypt the messages upon receiving them
		 */
		private void handleKeyExchangeMsg(String message, String sender,
				Context context, Intent i) {
			Toast.makeText(context, "got a key exchange message",
					Toast.LENGTH_LONG).show();
			// call MainActivitiy
			// MainActivity.this.receivedBroadcast(i);

			// TODO get the modulus and exponent of the public key of the sender
			// &
			// reconstruct the public key
			String contactNum = sender;
			String[] parts = message.split(" "); // expected structure of the
													// key exchange message:
													// "keyx modBase64Encoded expBase64Encoded"
			if (parts.length == 3) {
				String recipientPubModBase64Str = parts[1];
				String recipientPubExpBase64Str = parts[2];

				/*
				 * ================================ for testing only - to be
				 * removed later
				 */
				// verifyRecipientsPublicKey(recipientPubModBase64Str,recipientPubExpBase64Str,
				// context);
				/*
				 * ================================
				 */

				byte[] recipientPubModBA = Base64.decode(
						recipientPubModBase64Str, Base64.DEFAULT); // TODO to
																	// decide
																	// whether
																	// to use
																	// NO_WRAP
																	// or
																	// NO_PADDING
																	// here
				byte[] recipientPubExpBA = Base64.decode(
						recipientPubExpBase64Str, Base64.DEFAULT);
				BigInteger recipientPubMod = new BigInteger(recipientPubModBA);
				BigInteger recipientPubExp = new BigInteger(recipientPubExpBA);

				Log.i(TAG, "the recipient's public key modulus is "
						+ recipientPubMod + " and exponent is "
						+ recipientPubExp);

				// TODO store the intended recipient's public key in the app's
				// SharedPreferences
				SharedPreferences prefs = context.getSharedPreferences(
						contactNum, Context.MODE_PRIVATE);
				SharedPreferences.Editor prefsEditor = prefs.edit();

				prefsEditor
						.putString(PREF_PUBLIC_MOD, recipientPubModBase64Str);
				prefsEditor
						.putString(PREF_PUBLIC_EXP, recipientPubExpBase64Str);
				// prefsEditor.putString(PREF_PHONE_NUMBER, recipient);
				prefsEditor.commit();

				Log.i(TAG,
						"successfully remembered the contact "
								+ contactNum
								+ " and its public key module "
								+ prefs.getString(PREF_PUBLIC_MOD, DEFAULT_PREF)
								+ " and exponent "
								+ prefs.getString(PREF_PUBLIC_EXP,
										PREF_PUBLIC_EXP));
				Toast.makeText(context, "Got public key for " + contactNum,
						Toast.LENGTH_LONG).show();

				// TODO inform the UI Activity that public key is received
				UploadActivity.this.receivedBroadcast(i, contactNum);

				// TODO reload MainActivity so that it can read updated
				// sharedpreferences
				/*
				 * Log.w(TAG, "restarting MainActivity"); Intent intent = new
				 * Intent(); intent.setClassName("sg.edu.dukenus.securesms",
				 * "sg.edu.dukenus.securesms.MainActivity");
				 * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				 * context.startActivity(intent);
				 */

				// TODO handle a pending list of message to be sent securely due
				// to lack of key

			} else {
				Log.e(TAG,
						"something is wrong with the key exchange message, it's supposed to have 3 parts: the code 'keyx', the modulus and the exponent");
			}

		}

		private void handleEncryptedMsg(String message, String sender,
				Context context) {
			String contactNum = sender;
			String[] parts = message.split(" ");
			if (parts.length == 2) {

				// TODO get the private key of the intended recipient
				SharedPreferences prefs = context.getSharedPreferences(PREFS,
						Context.MODE_PRIVATE);

				String privateMod = prefs.getString(PREF_PRIVATE_MOD,
						DEFAULT_PREF);
				String priavteExp = prefs.getString(PREF_PRIVATE_EXP,
						DEFAULT_PREF);
				// String recipient = prefs.getString(PREF_RECIPIENT_NUM,
				// DEFAULT_PREF);
				if (!privateMod.equals(DEFAULT_PREF)
						&& !priavteExp.equals(DEFAULT_PREF)) {
					byte[] recipientPrivateModBA = Base64.decode(privateMod,
							Base64.DEFAULT);
					byte[] recipientPrivateExpBA = Base64.decode(priavteExp,
							Base64.DEFAULT);
					BigInteger recipientPrivateMod = new BigInteger(
							recipientPrivateModBA);
					BigInteger recipientPrivateExp = new BigInteger(
							recipientPrivateExpBA);
					RSAPrivateKeySpec recipientPrivateKeySpec = new RSAPrivateKeySpec(
							recipientPrivateMod, recipientPrivateExp);

					// TODO decrypt the encrypted message
					decryptMsg(parts[1], recipientPrivateKeySpec);
				} else {
					Log.e(TAG, "private key could not be retrieved");
				}
			} else {
				Log.e(TAG,
						"message has incorrect format, it's suppose to be 'gmstelehealth [measurements]'");
			}
		}

		private void decryptMsg(String msg, RSAPrivateKeySpec privateKey) {
			try {
				KeyFactory fact = KeyFactory.getInstance("RSA");

				PrivateKey privKey = fact.generatePrivate(privateKey);

				// TODO encrypt the message and send it
				// first decode the Base64 encoded string to get the encrypted
				// message
				byte[] encryptedMsg = Base64.decode(msg, Base64.DEFAULT);
				Log.i(TAG, "We got a message: " + msg
						+ " and after decode we got the encrypted message : "
						+ new String(encryptedMsg));

				Cipher cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.DECRYPT_MODE, privKey);
				// byte[] msgByteArray = msg.getBytes();

				byte[] cipherData = cipher.doFinal(encryptedMsg);

				String decryptedMsg = new String(cipherData);
				Log.i(TAG, "After decryption, we got the original message '"
						+ decryptedMsg + "'");

			} catch (NoSuchAlgorithmException e) {
				Log.e(TAG, "RSA algorithm not available", e);
			} catch (InvalidKeySpecException e) {
				Log.e(TAG, "", e);
			} catch (NoSuchPaddingException e) {
				Log.e(TAG, "", e);
			} catch (InvalidKeyException e) {
				Log.e(TAG, "", e);
			} catch (BadPaddingException e) {
				Log.e(TAG, "", e);
			} catch (IllegalBlockSizeException e) {
				Log.e(TAG, "", e);
			}
		}

		private Map<String, String> retrieveMessages(Intent intent) {
			Map<String, String> msg = null;
			SmsMessage[] msgs = null;
			Bundle bundle = intent.getExtras();

			if (bundle != null && bundle.containsKey("pdus")) {
				Object[] pdus = (Object[]) bundle.get("pdus");

				if (pdus != null) {
					int nbrOfpdus = pdus.length;
					msg = new HashMap<String, String>(nbrOfpdus);
					msgs = new SmsMessage[nbrOfpdus];

					// There can be multiple SMS from multiple senders, there
					// can be
					// a maximum of nbrOfpdus different senders
					// However, send long SMS of same sender in one message
					for (int i = 0; i < nbrOfpdus; i++) {
						msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

						String originatinAddress = msgs[i]
								.getOriginatingAddress();

						// Check if index with number exists
						if (!msg.containsKey(originatinAddress)) {
							// Index with number doesn't exist
							// Save string into associative array with sender
							// number
							// as index
							msg.put(msgs[i].getOriginatingAddress(),
									msgs[i].getMessageBody());

						} else {
							// Number has been there, add content but consider
							// that
							// msg.get(originatinAddress) already contains
							// sms:sndrNbr:previousparts of SMS,
							// so just add the part of the current PDU
							String previousparts = msg.get(originatinAddress);
							String msgString = previousparts
									+ msgs[i].getMessageBody();
							msg.put(originatinAddress, msgString);
						}
					}
				}
			}

			return msg;
		}
	};

}
