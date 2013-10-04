package sg.edu.nus.omronhealth;

import com.jwetherell.quick_response_code.DecoderActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	protected final boolean D = true;
	
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

}
