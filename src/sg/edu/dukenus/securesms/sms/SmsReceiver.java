package sg.edu.dukenus.securesms.sms;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import sg.edu.dukenus.bpmomron.MainActivity;

//import org.apache.commons.codec.binary.Base64;
import android.util.Base64;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {
	// debugging
	private final String TAG = "SmsReceiver";

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

	// actions/ intent filters
	private final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private final String SMS_SENT = "SMS_SENT";
	private final String SMS_DELIVERED = "SMS_DELIVERED";
	
	//private int msgProcessedCount = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(SMS_RECEIVED)) {
			//Log.w(TAG, "messages processed count "+msgProcessedCount);
			/*
			 * updating a sharedpreferences boolean value, hopefully the UI
			 * activity can see the updated value after that
			 */

			SharedPreferences prefs = context.getSharedPreferences("prefs",
					Context.MODE_PRIVATE);
			SharedPreferences.Editor prefseditor = prefs.edit();
			prefseditor.putBoolean("receivedsms", true);
			prefseditor.commit();

			Map<String, String> msg = retrieveMessages(intent);

			Log.w(TAG, "we received " + msg.size() + " messages in total");
			/*if (msgProcessedCount==msg.size()) {
				Log.w(TAG, "already processed the multipart message(s)");
				msgProcessedCount = 0;
				return;
			}*/
			if (msg != null) {
				String sender = "";
				String message = "";
				for (String tmp : msg.keySet()) {
					message = msg.get(tmp);

					Log.w(TAG, "message received is " + message);
					// Toast.makeText(context, "Received message: "+message,
					// Toast.LENGTH_SHORT);
					sender = tmp;
					
					handleMessage(message, sender, context, intent);
				}
				//msgProcessedCount++;
			}
		} else if (action.equals(SMS_SENT)) {
			Log.w(TAG, "received sent sms report");
			handleSentSms(context);
		} else if (action.equals(SMS_DELIVERED)) {
			Log.w(TAG, "received delivered sms report");
			handleDeliveredSms(context);
		}

	}

	private void handleSentSms(Context context) {
		switch (getResultCode()) {
		case Activity.RESULT_OK:
			Log.w(TAG, "SMS sent");
			Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show();

			// TextView debug = (TextView) findViewById(R.id.DebugMessages);
			// debug.append("SMS sent");
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			Toast.makeText(context, "Generic failure", Toast.LENGTH_SHORT)
					.show();
			Log.w(TAG, "Generic failure");
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			Toast.makeText(context, "No service", Toast.LENGTH_SHORT).show();
			Log.w(TAG, "No service");
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
			Log.w(TAG, "Null PDU");
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			Toast.makeText(context, "Radio off", Toast.LENGTH_SHORT).show();
			Log.w(TAG, "Radio off");
			break;
		}
	}

	private void handleDeliveredSms(Context context) {
		switch (getResultCode()) {
		case Activity.RESULT_OK:
			Log.w(TAG, "SMS delivered");
			Toast.makeText(context, "SMS delivered", Toast.LENGTH_SHORT).show();

			// TextView debug = (TextView) findViewById(R.id.DebugMessages);
			// debug.append("SMS sent");
			break;
		case Activity.RESULT_CANCELED:
			Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT)
					.show();
			Log.w(TAG, "SMS not delivered");
			break;
		}
	}

	private void handleMessage(String message, String sender, Context context, Intent i) {
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
	 * messages the recipient's public key will be used to encrypt the future
	 * text messages so that the recipient can use his/ her private key to
	 * decrypt the messages upon receiving them
	 */
	private void handleKeyExchangeMsg(String message, String sender,
			Context context, Intent i) {
		// TODO get the modulus and exponent of the public key of the sender &
		// reconstruct the public key
		String contactNum = sender;
		String[] parts = message.split(" "); // expected structure of the key
												// exchange message:
												// "keyx modBase64Encoded expBase64Encoded"
		if (parts.length == 3) {
			String recipientPubModBase64Str = parts[1];
			String recipientPubExpBase64Str = parts[2];

			/*
			 * ================================ for testing only - to be removed
			 * later
			 */
			// verifyRecipientsPublicKey(recipientPubModBase64Str,recipientPubExpBase64Str,
			// context);
			/*
			 * ================================
			 */

			byte[] recipientPubModBA = Base64.decode(recipientPubModBase64Str,
					Base64.DEFAULT); // TODO to decide whether to use NO_WRAP or
										// NO_PADDING here
			byte[] recipientPubExpBA = Base64.decode(recipientPubExpBase64Str,
					Base64.DEFAULT);
			BigInteger recipientPubMod = new BigInteger(recipientPubModBA);
			BigInteger recipientPubExp = new BigInteger(recipientPubExpBA);

			Log.i(TAG, "the recipient's public key modulus is "
					+ recipientPubMod + " and exponent is " + recipientPubExp);

			// TODO store the intended recipient's public key in the app's
			// SharedPreferences
			SharedPreferences prefs = context.getSharedPreferences(contactNum,
					Context.MODE_PRIVATE);
			SharedPreferences.Editor prefsEditor = prefs.edit();

			prefsEditor.putString(PREF_PUBLIC_MOD, recipientPubModBase64Str);
			prefsEditor.putString(PREF_PUBLIC_EXP, recipientPubExpBase64Str);
			// prefsEditor.putString(PREF_PHONE_NUMBER, recipient);
			prefsEditor.commit();

			Log.i(TAG,
					"successfully remembered the contact " + contactNum
							+ " and its public key module "
							+ prefs.getString(PREF_PUBLIC_MOD, DEFAULT_PREF)
							+ " and exponent "
							+ prefs.getString(PREF_PUBLIC_EXP, PREF_PUBLIC_EXP));
			Toast.makeText(context, "Ready to send secure message to "+contactNum, Toast.LENGTH_LONG).show();
			// call the UI Activity to verify that the updated sharedpreferences are available for its us
			int activitySwitch = 1;
			if (activitySwitch == 1) {
				//MainActivity.onPublicKeyReceived(i, contactNum, context); // without
																	// calling
				// this the activity UI won't see the updated
				// SharedPreferences
			} else if (activitySwitch == 2) {

			}
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

			String privateMod = prefs.getString(PREF_PRIVATE_MOD, DEFAULT_PREF);
			String priavteExp = prefs.getString(PREF_PRIVATE_EXP, DEFAULT_PREF);
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

				// There can be multiple SMS from multiple senders, there can be
				// a maximum of nbrOfpdus different senders
				// However, send long SMS of same sender in one message
				for (int i = 0; i < nbrOfpdus; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

					String originatinAddress = msgs[i].getOriginatingAddress();

					// Check if index with number exists
					if (!msg.containsKey(originatinAddress)) {
						// Index with number doesn't exist
						// Save string into associative array with sender number
						// as index
						msg.put(msgs[i].getOriginatingAddress(),
								msgs[i].getMessageBody());

					} else {
						// Number has been there, add content but consider that
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

	/*
	 * private void verifyRecipientsPublicKey(String mod, String exp, Context
	 * context) { SharedPreferences prefs =
	 * context.getSharedPreferences(PREFS_RECIPIENT, Context.MODE_PRIVATE);
	 * 
	 * String storedRecipientsPublicMod = prefs.getString(PREF_PUBLIC_MOD,
	 * DEFAULT_PREF); String storedRecipientsPublicExp =
	 * prefs.getString(PREF_PUBLIC_EXP, DEFAULT_PREF);
	 * 
	 * boolean result = (mod.equals(storedRecipientsPublicMod) && exp
	 * .equals(storedRecipientsPublicExp)); Log.w(TAG,
	 * "the recipient's public key received is the same as it was generated " +
	 * result); }
	 */
}
