package sg.edu.nus.omronhealth;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.CheckBox;

public class SettingActivity extends MainActivity {

	private static final String TAG = "SettingsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		//SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		//prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences prefs = getSharedPreferences(PREF_BPM, Context.MODE_PRIVATE);
		final SharedPreferences.Editor pref_editor = prefs.edit();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String qrcode = extras.getString("qrcode");
			String[] parts = qrcode.split(",");
			
			//If we really want to use "id" we have to define it more specifically, is it unique for each user or each app install or each Omron BPM device
			String deviceId = parts[0];//why would you have underscore at the beginning of the varibale name? Some people think it's a good practice that makes code more readable
			
			//desNum was originally designed as integer by Rui Boon so parsing is necessary
			//int desNum = Integer.parseInt(parts[1]);
			String desNum = parts[1];
			String macAddr = parts[2];
			/*
			 * if(parts.length >3){ String _macId2 = parts[3]; }
			 */
			// Store in pref
			
			pref_editor.putString(PREF_DEVICE_NAME, deviceId);
			pref_editor.putString(PREF_DES_NUM, desNum);
			pref_editor.putString(PREF_MAC_ADDR, macAddr);
			
			pref_editor.commit();
		}
		
		EditText idField = (EditText) findViewById(R.id.inputDeviceId);
		EditText serverNumField = (EditText) findViewById(R.id.inputDesNum);
		EditText macAddrField = (EditText) findViewById(R.id.inputMacAddr);
			
		//populate stored settings
		String idStored = prefs.getString(PREF_DEVICE_NAME, "NO_ID");
		String desNumStored = prefs.getString(PREF_DES_NUM, DEFAULT_NUM);
		String macAddrStored = prefs.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR);

		idField.setText(idStored);
		// need to cast as string. otherwise, it will interpret it as a res id
		serverNumField.setText(desNumStored);
		macAddrField.setText(macAddrStored);
		
		Button btnSave = (Button) findViewById(R.id.btnSave);
		btnSave.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				onSaveClick();
            }
		});
		
		CheckBox legacySMS = (CheckBox) findViewById(R.id.CheckboxEnableLegacySMS);
		//checking the current status of legacy sms format
		//boolean flag = prefs.getBoolean(PREF_LEGACY_SMS, DEFAULT_LEGACY_SMS);
		//if (flag) legacySMS.setChecked(true);
		
		// Listen to the event the checkbox is checked/ unchecked and enable/ disable the legacy SMS format accordingly 
		legacySMS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		    {
		        if ( isChecked ) {
		            //Toast.makeText(getApplicationContext(), "Legacy SMS format is enabled", Toast.LENGTH_SHORT).show();
		        	if (D) Log.i(TAG, "Legacy SMS format is enabled");
		        	pref_editor.putBoolean(PREF_LEGACY_SMS, true);
		        	pref_editor.commit();
		        } else {
		        	Toast.makeText(getApplicationContext(), "Legacy SMS format is disabled", Toast.LENGTH_SHORT).show();
		        	if (D) Log.i(TAG, "Legacy SMS format is disabled");
		        	pref_editor.putBoolean(PREF_LEGACY_SMS, false);
		        	pref_editor.commit();
		        }

		    }
		});
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			SharedPreferences prefs = getSharedPreferences(PREF_BPM, Context.MODE_PRIVATE);
			CheckBox legacySMS = (CheckBox) findViewById(R.id.CheckboxEnableLegacySMS);
			
			//checking the current status of legacy sms format
			boolean flag = prefs.getBoolean(PREF_LEGACY_SMS, DEFAULT_LEGACY_SMS);
			if (D) Log.i(TAG, "checkbox is checked "+flag);
			if (flag) {
				legacySMS.setChecked(true);
			}
		}
	}
	
	public void onSaveClick(){

		EditText idField = (EditText) findViewById(R.id.inputDeviceId);
		EditText serverNumField = (EditText) findViewById(R.id.inputDesNum);
		EditText macAddrField = (EditText) findViewById(R.id.inputMacAddr);
		
		String deviceId = idField.getText().toString();
		String desNum = serverNumField.getText().toString();
		String macAddr = macAddrField.getText().toString();
		
		Log.d(TAG, "id: " + deviceId);

		//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences prefs = getSharedPreferences(PREF_BPM, Context.MODE_PRIVATE);
		// Store in pref
		SharedPreferences.Editor pref_editor = prefs.edit();
		pref_editor.putString(PREF_DEVICE_NAME, deviceId);
		pref_editor.putString(PREF_DES_NUM, desNum);
		pref_editor.putString(PREF_MAC_ADDR, macAddr);
		
		pref_editor.commit();
					
		Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();    	
	    Intent intent = new Intent(this, UpdateMeasurementActivity.class);
	    startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.setting, menu);
		return true;
	}

}
