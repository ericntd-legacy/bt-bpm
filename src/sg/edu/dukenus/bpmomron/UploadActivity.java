package sg.edu.dukenus.bpmomron;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import sg.edu.dukenus.securesms.crypto.MyKeyUtils;
import sg.edu.dukenus.securesms.sms.SmsSender;
import sg.edu.dukenus.securesms.utils.MyUtils;
import sg.edu.nus.omronhealth.R;
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
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class UploadActivity extends MainActivity {
	// debugging
	private static final String TAG = "UploadActivity";
	
	private SmsSender smsSender;

	private static final int MAX_SMS_MESSAGE_LENGTH = 160;

	// Layout Views
    private ListView mConversationView;
    
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
	private ArrayList<OmronMeasurementData> measurementDataList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);
		//SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences prefs = getSharedPreferences(PREF_BPM, Context.MODE_PRIVATE);
		
		String desNumStored = prefs.getString(PREF_DES_NUM, DEFAULT_NUM);
		EditText phoneNum = (EditText) findViewById(R.id.phoneNum);
		
		phoneNum.setText(desNumStored);
		measurementDataList =  getIntent().getParcelableArrayListExtra(
						UpdateMeasurementActivity.MEASUREMENT_DATA_ARRAY);

		setup();
		if( ! measurementDataList.isEmpty() ) {
			// enable send btns
			Button normalSms = (Button) findViewById(R.id.btn_normalSms);
			Button encodedSms = (Button) findViewById(R.id.btn_encodedSms);
			
			normalSms.setEnabled(true);
			encodedSms.setEnabled(true);
			
		}
		for(OmronMeasurementData measurementData : measurementDataList) {

        	mConversationArrayAdapter.add(measurementData.toString());
		}
		
		
		
	}
	
	private void setup(){
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Enable menu options which includes
		//1. Settings
		getMenuInflater().inflate(R.menu.activity_upload, menu);
		return true;
	}

	public void onNormalSms(View view){
		
		//SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences prefs = getSharedPreferences(PREF_BPM, Context.MODE_PRIVATE);
		boolean legacySMS = prefs.getBoolean(PREF_LEGACY_SMS, DEFAULT_LEGACY_SMS);
		
		//String idStored = preferences.getString("deviceId", "NO_ID");
		//String macAddr = prefs.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR);
	
		//String msg = new StringBuilder(APP_CODE).append(" ").append("@MAC="+macAddr+"@ ").toString();
		BPMeasurementData tmp = null;
		String msg = "";
		
		for(OmronMeasurementData measurementData : measurementDataList) {
			tmp = (BPMeasurementData) measurementData;
			//String dateTime = tmp.getDateTime();
			//msg = new StringBuilder(msg).append("@datetime="+dateTime+"@ ").toString();
			//String measurement = "@systolic="+tmp.getSys()+"@ @diastolic="+tmp.getDia()+"@ @HR="+tmp.getPulse()+"@";
			//msg = new StringBuilder(msg).append(measurement).append(" ").toString();
		}
		
		if (tmp!=null) {
			//msg = constructSMS(PREF_BPM,-1, tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1, tmp.getDateTimeMySQL());
			if (legacySMS) msg = constructLegacySMS(PREF_BPM,-1, tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1, tmp.getDateTimeMySQL());
			else msg = constructSMS(PREF_BPM,-1, tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1, tmp.getDateTimeMySQL());
		}
		
		Toast.makeText(this, "Sending a normal sms " + msg + "  length: " + msg.length(),
				Toast.LENGTH_LONG).show();
		if (D) Log.w(TAG, "the exact SMS message was '"+msg+"'");
		
		EditText phoneNumberField = (EditText) findViewById(R.id.phoneNum);
		String phoneNumber = phoneNumberField.getText().toString();
		sendSMS(phoneNumber, msg);
	}
	
	//Format of the SMS: "gmstelehealth @MAC=[mac address of the pulse oximeter]@ @datetime=[yyyy-mm-dd HH:mm:ss]@ @systolic=[systolic]@ @diastolic=[diastolic]@ @weight=[weight]@ @hr=[hr]@ @spo2=[spo2]@"
    private String constructSMS(String pref,int weight, int systolic, int diastolic, int pulse, int spo2, String measurementDate) {
    	//Code for this app
    	String msg = APP_CODE+" ";
    	
    	//MAC address of the health device
    	String macAddr = DEFAULT_MAC_ADDR;
    	
    	SharedPreferences tmp = getSharedPreferences(pref, Context.MODE_PRIVATE);
    	if (tmp!=null) {
    		macAddr = tmp.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR);
    		if (D) Log.w(TAG, "mac address of the pulse oximeter is "+macAddr);
    	}
    	msg = msg + "@mac="+macAddr+"@ ";
    	
    	//Date and time of the measurement
    	String dt = new String();
    	if (measurementDate==null) {
    		//Grab the Android system date and time 
        	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        	Date date = new Date();
        	dt = dateFormat.format(date);
    	} else dt = measurementDate;
    	msg = msg + "@datetime="+dt+"@ ";
    	
    	//Measurement data e.g. weight, systolic, pulse
    	if (weight>=0) msg = msg + "@weight="+weight+"@ ";
    	if (systolic>=0) msg = msg + "@systolic="+systolic+"@ ";
    	if (diastolic>=0) msg = msg + "@diastolic="+diastolic+"@ ";
    	if (pulse>=0) msg = msg + "@hr="+pulse+"@ ";
    	if (spo2>=0) msg = msg + "@spo2="+spo2+"@ ";
    	
    	//msg = "gmstelehealth @MAC="+ macAddr +"@ @datetime="+dt+"@ @HR="+HR+"@ @spO2="+SPO2+"@";
    	msg = msg.trim();
    	
    	if (D) Log.w(TAG, "The exact SMS message was '"+msg+"'");
    	return msg;
    }
    
    //From MAC; @systolic@ = 114; @diastolic@ = 83; @HR@ = 81;
    private String constructLegacySMS(String pref,int weight, int systolic, int diastolic, int pulse, int spo2, String measurementDate) {
    	String msg = "From ";
    	
    	//MAC address of the health device
    	String macAddr = DEFAULT_MAC_ADDR;
    	
    	SharedPreferences tmp = getSharedPreferences(pref, Context.MODE_PRIVATE);
    	if (tmp!=null) {
    		macAddr = tmp.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR);
    		if (D) Log.w(TAG, "mac address of the health device is "+macAddr);
    	}
    	msg = msg+macAddr+"; ";
    	
    	//Measurement data e.g. weight, systolic, pulse
    	//if (weight>=0) msg = msg + "@weight="+weight+"@ ";
    	if (systolic>=0) msg = msg + "@systolic@ = "+systolic+"; ";
    	if (diastolic>=0) msg = msg + "@diastolic@ = "+diastolic+"; ";
    	if (pulse>=0) msg = msg + "@HR@ = "+pulse+"; ";
    	if (spo2>=0) msg = msg + "@spO2@ = "+spo2+"; ";
    	
    	//msg = "gmstelehealth @MAC="+ macAddr +"@ @datetime="+dt+"@ @HR="+HR+"@ @spO2="+SPO2+"@";
    	msg = msg.trim();
    	
    	if (D) Log.w(TAG, "The exact SMS message was '"+msg+"'");
    	
    	return msg;
    }
	
	public void sendSecureSMS(View view){
		//SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences prefs = getSharedPreferences(PREF_BPM, Context.MODE_PRIVATE);
		boolean legacySMS = false;
		
		EditText phoneNumberField = (EditText) findViewById(R.id.phoneNum);
		String contactNum = phoneNumberField.getText().toString();
				
		//String idStored = preferences.getString("deviceId", "NO_ID");
		BPMeasurementData tmp = null;
		String measurementStr = "";
		
		for(OmronMeasurementData measurementData : measurementDataList) {
			tmp = (BPMeasurementData) measurementData;
			//String dateTime = tmp.getDateTime();
			//msg = new StringBuilder(msg).append("@datetime="+dateTime+"@ ").toString();
			//String measurement = "@systolic="+tmp.getSys()+"@ @diastolic="+tmp.getDia()+"@ @HR="+tmp.getPulse()+"@";
			//msg = new StringBuilder(msg).append(measurement).append(" ").toString();
		}
		
		if (tmp!=null) {
			//msg = constructSMS(PREF_BPM,-1, tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1, tmp.getDateTimeMySQL());
			if (legacySMS) measurementStr = constructLegacySMS(PREF_BPM,-1, tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1, tmp.getDateTimeMySQL());
			else measurementStr = constructSMS(PREF_BPM,-1, tmp.getSys(), tmp.getDia(), tmp.getPulse(), -1, tmp.getDateTimeMySQL());
		}
		
		Log.w(TAG, "measurement string: "+measurementStr);
		
			//Toast.makeText(this, msg + "  length: " + msg.length(), Toast.LENGTH_LONG).show();
			smsSender = new SmsSender(contactNum);
			smsSender.sendSecureSMS(getApplicationContext(), measurementStr);
		
	}


	// http://mobiforge.com/developing/story/sms-messaging-android
    private void sendSMS(String phoneNumber, String message)
    {        
        Log.d(TAG, "SendSMS :: phone: " + phoneNumber);
        Log.d(TAG, "SendSMS :: msg: " + message);
        
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
 
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            new Intent(SENT), 0);
 
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
            new Intent(DELIVERED), 0);
        
        int length = message.length();
        ArrayList <PendingIntent> sentPIList = new ArrayList<PendingIntent>();
        ArrayList <PendingIntent> deliveredPIList = new ArrayList<PendingIntent>();
        sentPIList.add(sentPI);
        deliveredPIList.add(deliveredPI);
        
        if(length > MAX_SMS_MESSAGE_LENGTH){
        	int numOfSms = length / MAX_SMS_MESSAGE_LENGTH + 1 ; //int maths
        	for(int i=1; i<numOfSms; i++){
        		PendingIntent sentPIi = PendingIntent.getBroadcast(this, 0,
        	            new Intent(SENT), 0);
                PendingIntent deliveredPIi = PendingIntent.getBroadcast(this, 0,
                        new Intent(DELIVERED), 0);

                sentPIList.add(sentPIi);
                deliveredPIList.add(deliveredPIi);
                
        	}
        }
        
        /*
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", 
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));
 
        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;                        
                }
            }
        }, new IntentFilter(DELIVERED));
        */   
 
        SmsManager smsManager = SmsManager.getDefault();
        //sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);  
        
        //int length = message.length();          
        if(length > MAX_SMS_MESSAGE_LENGTH) {
            ArrayList<String> messagelist = smsManager.divideMessage(message);          
            smsManager.sendMultipartTextMessage(phoneNumber, null,
            		messagelist, sentPIList, deliveredPIList);
        }
        else
            smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
        }

}
