package sg.edu.nus.omronhealth;

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import sg.edu.nus.omronhealth.spp.BloodPressureMon;
import sg.edu.nus.omronhealth.spp.BPMeasurementData;
import sg.edu.nus.omronhealth.spp.OmronBaseClass;
import sg.edu.nus.omronhealth.spp.OmronMeasurementData;
import sg.edu.nus.omronhealth.spp.WeighingScale;
import sg.edu.nus.omronhealth.UpdateMeasurementActivity;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothSPPService {
    // Debugging
    private static final String TAG = "BluetoothSPPService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_HEALTH_MON = "HealthMonSPP";

    // Unique UUID for this application
    private static final UUID MY_UUID = 
    		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    
    
    //Omron
	public static final int UNKNOWN_DEVICE = 0;
	public static final int BLOOD_PRESSURE_MON = 1;
	public static final int WEIGHING_SCALE = 2;

	private int deviceType;
    
    private static final int MALE = 0;
    private static final int FEMALE = 1;
    
    private static final int UNIT_METRIC = 0;
    private static final int UNIT_IMPERIAL = 1;
    
    private String modelNum;
    private String deviceSerialNum;

    private OmronMeasurementData[] measurementData;
    

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothSPPService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        /*if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }*/
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        /*if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }*/
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(UpdateMeasurementActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        /*if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }*/

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(UpdateMeasurementActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothSPPService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(UpdateMeasurementActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothSPPService.this.start();
    }
    
    private void restartService(String description) {
    	// Send a description to the UI activity
        Message msg = mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(UpdateMeasurementActivity.TOAST, description);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothSPPService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
               
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    		NAME_HEALTH_MON, MY_UUID);
                
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothSPPService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                            	if (D) Log.w(TAG, "closing the BluetoothSocket by AcceptThread");
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID);
                    
                } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSPPService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        //volatile boolean running = true;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
        	//while (running) {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            
            
            

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    
                    // TODO: extend to omron device
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                    //mHandler.ob
                    String msg = new String(buffer).substring(0, bytes);
                    Log.d(TAG,"run ready: " + msg);
                    try {
						this.handleMessage(msg);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //connectionLost();
                    //Start the service over to restart listening mode
                    BluetoothSPPService.this.start();
                    break;
                }
            }
        	//}
        }
        
        private void handleMessage(String msg) throws Exception{
        	//handleMessage_fake_server(msg);
        	handleMessage_actual(msg);
        }
   
        
        private void handleMessage_actual(String msg) throws Exception{
        	byte[] buffer = new byte[1024];
        	int len;
        	OmronBaseClass dev;
        	OmronMeasurementData measurementData;
        	

            
        	
        	Log.d(TAG, "handleMsg2: " + msg);
        	Log.d(TAG, "handleMsg2. compareto: "+ msg.compareTo("READY"));
        	
        	if(msg.compareTo("READY") == 0){
        		Log.d(TAG, "READY");
        		
        		//getversion
        		sendCmd("VER00");        		
        		len = mmInStream.read(buffer);
        		String readMsg = new String(buffer).substring(0, len);
                Log.d(TAG,"handleMsg: " + readMsg);
        		if(checkChecksum(buffer, len)) {
        			modelNum = readMsg.substring(3, len-1);
        			Log.d(TAG,"ModelNum:" + modelNum);
        			if(modelNum.compareTo("M7081-IT200") == 0){
        				deviceType = BLOOD_PRESSURE_MON;
        				Log.d(TAG,"type=BP");
        				dev = new BloodPressureMon();
        				 
        			} else if (modelNum.compareTo("XXXXXX") == 0) { //TODO: 
        				deviceType = WEIGHING_SCALE;
        				Log.d(TAG, "type=weighing scale");
        				//dev = new BloodPressureMon();
        				dev = new WeighingScale();
        			} else {
        				throw new Exception("no such model");
        			}
        			
        			// get device serial
        			sendCmd(dev.cmdSerial());
        			len = mmInStream.read(buffer);
        			dev.handleSerialNum(buffer, len);
        			

    				// get user profile
        			sendCmd(dev.cmdProfile());
	        		len = mmInStream.read(buffer);
	        		dev.handleProfileData(buffer, len);
	        		String profileStr = dev.profileString();
	        		
	        		Log.d(TAG, "profile: " + profileStr);
	        		
	        		// get data num
	        		sendCmd(dev.cmdDataNum());
	        		len = mmInStream.read(buffer);
	        		dev.handleDataNum(buffer, len);
	        		
	        		// get measurement data 
	        		
	        		//DatabaseHandler db = new DatabaseHandler();
	        		//for(int i=0; i < dev.getD2(); i++) { // XXX: suppose to be d1
						int i = 0;
						Message mmsg = mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_VERBATIM);
						Bundle bundle = new Bundle();
		            
	        			sendCmd(dev.cmdMeasurementData(i));
	        			len = mmInStream.read(buffer);
	        			measurementData = dev.handleMeasurementData(buffer, len, 0);
	        		
	        			Log.w(TAG, "measurementData " + i + ":" + measurementData);

	                	bundle.putString(UpdateMeasurementActivity.VERBATIM_TEXT, "Measurement " + i + ":" + measurementData.toString());
	                	bundle.putParcelable("testK", measurementData);
	                	mmsg.setData(bundle);
	                	mHandler.sendMessage(mmsg);
	                	
	                	//Log.d(TAG, "adding to db");
	                	//MeasurementDataSource dbSrc = new MeasurementDataSource(null);
	                	//dbSrc.createBPRecord((BPMeasurementData) measurementData);
	                	//db.addStrData(measurementData);
	                //}
	        		  				
    				//TODO: to close the connection nice and fast here
                	//Send TOK command
	                //This has never worked so far, the BPM always response "NO" indicating that it's busy in communication mode
            		sendCmd(OmronBaseClass.cmdTOK());
            		len = mmInStream.read(buffer);
            		OmronBaseClass.handleCmdTOK(buffer, len);
            		
            		//Close the BluetoothSocket
            		cancel();
            		
            		//Inform UpdateMeasurementActitivy that connection is terminated
            		restartService("Connection terminated");
            		
            		//Thread.currentThread().interrupt();
            		//Prevent this thread from running afterwards
            		//running = false;
            		
        		}
        	}
        }
        
        

        
        private void sendCmd(String cmd){
    		byte[] byteCmd = cmd.getBytes();
    		write(byteCmd);
    		try {
				sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        private boolean checkChecksum(byte[] msg, int len){
        	//TODO
        	return true;
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(UpdateMeasurementActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}

