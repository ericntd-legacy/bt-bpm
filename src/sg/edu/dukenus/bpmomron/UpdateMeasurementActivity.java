package sg.edu.dukenus.bpmomron;

import java.util.ArrayList;

import sg.edu.dukenus.bpmomron.BluetoothSPPService;
import sg.edu.nus.omronhealth.R;
import sg.edu.nus.omronhealth.db.BP_DAO;
import sg.edu.nus.omronhealth.spp.BPMeasurementData;
import sg.edu.nus.omronhealth.spp.OmronMeasurementData;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class UpdateMeasurementActivity extends MainActivity {
	
    // Debugging
    private static final String TAG = "BtMeasurement";
    private static final boolean D = true;
    
    // Message types sent from the BluetoothSPPService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_VERBATIM = 6;

    public static String MEASUREMENT_DATA = "measurementData";
    public static String MEASUREMENT_DATA_ARRAY = "measurementDataArray";

    // Key names received from the BluetoothSPPService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String VERBATIM_TEXT = "verbatim_text";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
    // Layout Views
    private ListView mConversationView;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothSPPService mSPPService = null;
    
    //DAO
    private BP_DAO bp_dao;
    ArrayList<OmronMeasurementData> measurementDataList;
    
    // BT discoveable flag
    private boolean isBtDiscoverableDialogueOpen = false;
    private boolean hasConnected = false;
    
    private String deviceMacAddress = "00:22:58:35:C2:5E";
    
    private AnimationDrawable guideAnimation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(D) Log.e(TAG, "++onCreate++");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_update_measurement);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        bp_dao = new BP_DAO(this);
        
		
	}
	
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mSPPService == null) setupChat();
        }
        
        // go into discoverable mode
        ensureDiscoverable();
        // init
        measurementDataList = new ArrayList<OmronMeasurementData>();
        
        ImageView guideImage = (ImageView) findViewById(R.id.guideImage);
        guideImage.setBackgroundResource(R.drawable.guide);
        guideAnimation = (AnimationDrawable) guideImage.getBackground();
        guideAnimation.start();

        
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mSPPService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mSPPService.getState() == BluetoothSPPService.STATE_NONE) {
              // Start the Bluetooth chat services
              mSPPService.start();
            }
        }
        ensureDiscoverable();
    }
    

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        /*
        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });
        */

        // Initialize the BluetoothSPPService to perform bluetooth connections
        mSPPService = new BluetoothSPPService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }
    
    

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mSPPService != null) mSPPService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE && 
            isBtDiscoverableDialogueOpen == false) {
        	isBtDiscoverableDialogueOpen = true; // fix race cond, st. it will not display it twice
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mSPPService.getState() != BluetoothSPPService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothSPPService to write
            byte[] send = message.getBytes();
            mSPPService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
        // also displace it in the status line
        TextView view = (TextView) findViewById(R.id.statusText);
        view.setText(getString(resId));
    }
    private final void setStatus(CharSequence subTitle) {
    	Log.d(TAG, "setStatus here");
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
        
        // also displace it in the status line
        TextView view = (TextView) findViewById(R.id.statusText);
        view.setText(subTitle);
        
    }
    

    // The Handler that gets information back from the BluetoothSPPService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
                case BluetoothSPPService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    mConversationArrayAdapter.clear();
                    hasConnected = true;
                    ImageView guideImage = (ImageView) findViewById(R.id.guideImage);
                    guideImage.getVisibility();
                    guideImage.setVisibility(ImageView.GONE);
                    
                    break;
                case BluetoothSPPService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothSPPService.STATE_LISTEN:
                case BluetoothSPPService.STATE_NONE:
                	if(hasConnected == true){
                		// gotten all data
                		setStatus(R.string.title_ok);
                		Log.d(TAG, "Success. Disconnect");
                		onSuccessDisconnect();
                	} else {
                		setStatus(R.string.title_not_connected);
                	}
                    break;
                }
                break;
            case MESSAGE_WRITE:
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG,"readMessage: " + readMessage);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
                
            case MESSAGE_VERBATIM:
            	// display just as it is
            	String verbatimMessage = msg.getData().getString(VERBATIM_TEXT);
            	OmronMeasurementData measurementData = msg.getData().getParcelable("testK");
            	BPMeasurementData test = (BPMeasurementData) measurementData;
            	Log.w(TAG, "parcelable: " + measurementData.toSmsHumanString()+" systolic is "+test.getSys());
            	//Log.e(TAG, "parcelable machine: "  + measurementData.toSmsMachineString());
            	//msg.getData().get
            	mConversationArrayAdapter.add(verbatimMessage);
            	
            	
            	Log.d(TAG, "add to db in activity");
            	
            	measurementDataList.add(measurementData);
            	
            	//bp_dao.createBPRecord(verbatimMessage);
            	bp_dao.createBPRecord((BPMeasurementData) measurementData);

        		//DatabaseHandler db = new DatabaseHandler(null);
            	//db.addStrData(verbatimMessage);
            	
            	break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        /*
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
            */
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /*
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mSPPService.connect(device, secure);
    }
    */
    
    
    
	private void onSuccessDisconnect() {
		// Trigger upload button
		Button btn = (Button) findViewById(R.id.btn_uploadOrFirstTime);
		btn.setText(R.string.updateMeasurement_upload);
		
		// start upload screen
		//TODO
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_update_measurement, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
        case R.id.secure_connect_scan:
        	//connectDevice(deviceMacAddress, false);
        	connectToStoredDevice();
            return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void connectToStoredDevice(){
		//SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences preferences = getSharedPreferences(PREF_BPM, Context.MODE_PRIVATE);
		String macId = preferences.getString("macAddr", "00:00:00:00:00");
		connectDevice(macId, false);
		
	}
	
    private void connectDevice(String macAddress, boolean secure) {
        
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
        // Attempt to connect to the device
        mSPPService.connect(device, secure);
    }
	
	   public void onFirstTimeClick(View view){
	    	
	    	if(hasConnected == true){
	    		Intent intent = new Intent(this, UploadActivity.class);
	    		intent.putExtra(MEASUREMENT_DATA_ARRAY, measurementDataList );
		    	startActivity(intent);
	    	} else {
	    		Toast.makeText(this, "Connecting to device...", Toast.LENGTH_SHORT).show(); 
		    	connectToStoredDevice();
	    	}
	    	
	    }

}
