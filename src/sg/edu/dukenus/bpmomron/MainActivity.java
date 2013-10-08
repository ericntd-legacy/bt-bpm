package sg.edu.dukenus.bpmomron;

import sg.edu.dukenus.securesms.crypto.MyKeyUtils;
import sg.edu.nus.omronhealth.R;

import com.jwetherell.quick_response_code.DecoderActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// debugging
	protected final boolean D = true;
	private final String TAG = "MainActivity";
	
	//application's preferences including settings
	protected final String PREF_BPM = "BPM preferences";
	protected final String PREF_MAC_ADDR = "macAddr";
	protected final String PREF_DES_NUM = "desNum";
	protected final String PREF_DEVICE_NAME = "deviceName";
	protected final String PREF_LEGACY_SMS = "Legacy SMS Format";
			;
	protected final String DEFAULT_MAC_ADDR = "00:00:00:00:00:00";
	protected final String APP_CODE = "gmstelehealth";
	protected final String DEFAULT_NUM = "85577008";
	protected final boolean DEFAULT_LEGACY_SMS = false;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/*
		 * register the receiver of "sent" and "delivered" statuses of sms
		 */
		registerReceivers();
		
		/*
		 * Check existing public keys of the server +6584781395
		 */
		SharedPreferences prefs = getSharedPreferences(MyKeyUtils.DEFAULT_CONTACT_NUM, Context.MODE_PRIVATE);
		String contactPubMod = prefs.getString(MyKeyUtils.PREF_PUBLIC_MOD, MyKeyUtils.DEFAULT_PREF);
		String contactPubExp = prefs.getString(MyKeyUtils.PREF_PUBLIC_EXP, MyKeyUtils.DEFAULT_PREF);
		if (!contactPubMod.isEmpty()&&!contactPubExp.isEmpty()) {
			Log.i(TAG, "public key stored for "+MyKeyUtils.DEFAULT_CONTACT_NUM+" with mod: "+contactPubMod+" and exp: "+contactPubExp);
		} else {
			Log.w(TAG, "public key not found for "+MyKeyUtils.DEFAULT_CONTACT_NUM+" so where did it go?");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
    public void onMyButtonClick(View view)  
    {  
        Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show();  
    }  

    public void onUpdateClick(View view){
    	Toast.makeText(this, "Update clicked!", Toast.LENGTH_SHORT).show();    	
    	Intent intent = new Intent(this, UpdateMeasurementActivity.class);
    	startActivity(intent);
    }
    public void onMyMeasurementClick(View view){
    	Toast.makeText(this, "Measurement clicked!", Toast.LENGTH_SHORT).show();    	
    }
    public void onUploadClick(View view){
    	Toast.makeText(this, "Upload clicked!", Toast.LENGTH_SHORT).show();    	
    }
    public void onInitSetupClick(View view){
    	Toast.makeText(this, "Init Setup clicked!", Toast.LENGTH_SHORT).show();    	
    	Intent intent = new Intent(this, DecoderActivity.class);
    	startActivity(intent);  	
    }
    public void onSettingsClick(View view){
    	Toast.makeText(this, "Settings clicked!", Toast.LENGTH_SHORT).show();    	
    	Intent intent = new Intent(this, SettingActivity.class);
    	startActivity(intent);  	
    }
    
    private void registerReceivers() {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";
		// ---when the SMS has been sent---
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS sent",
							Toast.LENGTH_SHORT).show();
					Log.i(TAG, "SMS sent");
					//TextView debug = (TextView) findViewById(R.id.DebugMessages);
					//debug.append("SMS sent");
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(), "Generic failure",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "Generic failure");
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "No service",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "No service");
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Null PDU",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "Null PDU");
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Radio off",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "Radio off");
					break;
				}
			}
		}, new IntentFilter(SENT));

		// ---when the SMS has been delivered---
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS delivered",
							Toast.LENGTH_SHORT).show();
					Log.i(TAG, "SMS delivered");
					//TextView debug = (TextView) findViewById(R.id.DebugMessages);
					//debug.append("SMS sent");
					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(getBaseContext(), "SMS not delivered",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "SMS not delivered");
					break;
				}
			}
		}, new IntentFilter(DELIVERED));
	}

}
