package sg.edu.dukenus.bpmomron;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import sg.edu.dukenus.securesms.crypto.MyKeyUtils;
import sg.edu.dukenus.securesms.sms.SmsReceiver;
import sg.edu.dukenus.securesms.utils.MyUtils;
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
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// debugging
	protected final boolean D = true;
	private final static String TAG = "MainActivity";

	// broadcast receivers
	private SmsReceiver mSmsReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.w(TAG, "onCreate");

		/*
		 * register the receiver of "sent" and "delivered" statuses of sms
		 */
		registerReceivers();
		
		/*
		 * clear the server's key for testing purpose
		 */
		/*SharedPreferences prefs = getSharedPreferences("+6584781395", Context.MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.clear();
		prefsEditor.commit();*/
		
		mSmsReceiver = new SmsReceiver();

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
		Log.w(TAG, "registering SMS receiver");
		IntentFilter iff = new IntentFilter();
		iff.addAction("android.provider.Telephony.SMS_RECEIVED");
		this.registerReceiver(this.mSmsReceiver, iff);
		
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

	}

	@Override
	public void onStop() {
		super.onStop();
		Log.w(TAG, "onStop");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onMyButtonClick(View view) {
		//Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show();
	}

	public void onUpdateClick(View view) {
		//Toast.makeText(this, "Update clicked!", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(this, UpdateMeasurementActivity.class);
		startActivity(intent);
	}

	public void onMyMeasurementClick(View view) {
		//Toast.makeText(this, "Measurement clicked!", Toast.LENGTH_SHORT).show();
	}

	public void onUploadClick(View view) {
		//Toast.makeText(this, "Upload clicked!", Toast.LENGTH_SHORT).show();
	}

	public void onInitSetupClick(View view) {
		//Toast.makeText(this, "Init Setup clicked!", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(this, DecoderActivity.class);
		startActivity(intent);
	}

	public void onSettingsClick(View view) {
		//Toast.makeText(this, "Settings clicked!", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(this, SettingsActivity.class);
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
					// TextView debug = (TextView)
					// findViewById(R.id.DebugMessages);
					// debug.append("SMS sent");
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
					// TextView debug = (TextView)
					// findViewById(R.id.DebugMessages);
					// debug.append("SMS sent");
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

	/*
	 * a receiver here to receive key exchange message from server something
	 * like a service or asynctask has to request for the key from the server
	 * first of course
	 */
	public static void onPublicKeyReceived(Intent i, String contact, Context context) {
		Log.w(TAG, ">> onPublicKeyReceived()");
		// SmsReceiver will try to trigger this
		// Log.w(TAG, "it goes here but did the activity restart?");
		/*SharedPreferences prefs = context.getSharedPreferences(contact,
				Context.MODE_PRIVATE);
		String pubMod = prefs.getString(MyKeyUtils.PREF_PUBLIC_MOD,
				MyKeyUtils.DEFAULT_PREF);

		Log.w(TAG,
				"public key updated for contact "
						+ contact
						+ " "
						+ (MyKeyUtils.getRecipientsPublicKey(contact,
								context.getApplicationContext()) != null));
		Toast.makeText(context.getApplicationContext(), "Ready to send secure message to "+contact,
				Toast.LENGTH_SHORT).show();*/
	}

	/*private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		// SharedPreferences
		//private final String PREFS = "MyKeys";
		//private final String PREF_PUBLIC_MOD = "PublicModulus";
		//private final String PREF_PUBLIC_EXP = "PublicExponent";
		//private final String PREF_PRIVATE_MOD = "PrivateModulus";
		//private final String PREF_PRIVATE_EXP = "PrivateExponent";

		// private final String PREF_PHONE_NUMBER = "PhoneNumber";
		// private final String PREF_RECIPIENT_NUM = "PhoneNumber";


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

		
		 * the sender here is actually the recipient of future encrypted text
		 * messages the recipient's public key will be used to encrypt the
		 * future text messages so that the recipient can use his/ her private
		 * key to decrypt the messages upon receiving them
		 
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

				
				 * ================================ for testing only - to be
				 * removed later
				 
				// verifyRecipientsPublicKey(recipientPubModBase64Str,recipientPubExpBase64Str,
				// context);
				
				 * ================================
				 

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
						.putString(MyKeyUtils.PREF_PUBLIC_MOD, recipientPubModBase64Str);
				prefsEditor
						.putString(MyKeyUtils.PREF_PUBLIC_EXP, recipientPubExpBase64Str);
				// prefsEditor.putString(PREF_PHONE_NUMBER, recipient);
				prefsEditor.commit();

				Log.i(TAG,
						"successfully remembered the contact "
								+ contactNum
								+ " and its public key module "
								+ prefs.getString(MyKeyUtils.PREF_PUBLIC_MOD, MyKeyUtils.DEFAULT_PREF)
								+ " and exponent "
								+ prefs.getString(MyKeyUtils.PREF_PUBLIC_EXP, MyKeyUtils.DEFAULT_PREF));
				Toast.makeText(context, "Got public key for " + contactNum,
						Toast.LENGTH_LONG).show();

				// TODO inform the UI Activity that public key is received
				MainActivity.onPublicKeyReceived(i, contactNum, getApplicationContext());

				// TODO reload MainActivity so that it can read updated
				// sharedpreferences
				
				 * Log.w(TAG, "restarting MainActivity"); Intent intent = new
				 * Intent(); intent.setClassName("sg.edu.dukenus.securesms",
				 * "sg.edu.dukenus.securesms.MainActivity");
				 * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				 * context.startActivity(intent);
				 

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
				SharedPreferences prefs = context.getSharedPreferences(MyKeyUtils.PREFS_MY_KEYS,
						Context.MODE_PRIVATE);

				String privateMod = prefs.getString(MyKeyUtils.PREF_PRIVATE_MOD,
						MyKeyUtils.DEFAULT_PREF);
				String priavteExp = prefs.getString(MyKeyUtils.PREF_PRIVATE_EXP,
						MyKeyUtils.DEFAULT_PREF);
				// String recipient = prefs.getString(PREF_RECIPIENT_NUM,
				// MyKeyUtils.DEFAULT_PREF);
				if (!privateMod.equals(MyKeyUtils.DEFAULT_PREF)
						&& !priavteExp.equals(MyKeyUtils.DEFAULT_PREF)) {
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
	};*/

}
