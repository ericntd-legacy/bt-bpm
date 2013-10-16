package sg.edu.dukenus.securesms.sms;

import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;

import sg.edu.dukenus.securesms.crypto.MyKeyUtils;
import sg.edu.dukenus.securesms.utils.MyUtils;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

public class SmsSender {
	// debugging
	private final String TAG = "SmsSender";

	private String recipientNum;
	private String message;

	private final String HEALTH_SMS = "gmstelehealth";

	public SmsSender(String phoneNum, String msg) {
		this.recipientNum = phoneNum;
		this.message = msg;
	}

	public SmsSender(String phoneNum) {
		this.recipientNum = phoneNum;
	}

	public String getRecipientNum() {
		return this.recipientNum;
	}

	public void setRecipientNum(String phoneNo) {
		this.recipientNum = phoneNo;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String msgStr) {
		this.message = msgStr;
	}

	public void sendLongSMS(Context context) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";
		SmsManager sm = SmsManager.getDefault();

		if (this.message != null && !this.message.isEmpty() && this.recipientNum!=null && !this.recipientNum.isEmpty()) {
			
			ArrayList<String> parts = sm.divideMessage(this.message);

			int numParts = parts.size();

			ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
			ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();

			for (int i = 0; i < numParts; i++) {
				sentIntents.add(PendingIntent.getBroadcast(context, 0,
						new Intent(SENT), 0));
				deliveryIntents.add(PendingIntent.getBroadcast(context, 0,
						new Intent(DELIVERED), 0));
			}

			sm.sendMultipartTextMessage(this.recipientNum, null, parts,
					sentIntents, deliveryIntents);
		} else {
			Log.e(TAG, "message or contact num not set, message: "+this.message+" and recipient: "+this.recipientNum);
		}
	}

	public void sendSMS(Context context) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
				new Intent(SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
				new Intent(DELIVERED), 0);

		SmsManager sms = SmsManager.getDefault();
		try {
			sms.sendTextMessage(this.recipientNum, null, message, sentPI,
					deliveredPI);
		} catch (IllegalArgumentException e) {
			Log.e(TAG,
					"some information is not correct e.g. recipient's phone number",
					e);
		}
	}

	public void sendKeyExchangeSMS(String recipient, String mod, String exp,
			Context context) {
		if (mod.length() != 0 && exp.length() != 0) {
			String msg = "keyx " + mod + " " + exp;
			Log.w(TAG, ">> sendKeyExchangeSMS() - Sending key exchange sms: '" + msg + "' with length: "+msg.length() + " to "+recipient);
			
			this.recipientNum = recipient;
			this.message = msg;
			// TextView debug = (TextView) findViewById(R.id.DebugMessages);
			// debug.append("Sending key exchange sms: '" + msg + "'");
			if (this.message.length() > 160) {
				sendLongSMS(context);
			} else {
				sendSMS(context);
			}
		} else {
			Log.w(TAG, "mod or exp can't be empty string");
		}
	}

	public void sendKeyExchangeSMS(Context context) {
		// TODO to retrieve the modulus and exponent of the curent user's public
		// key
		// SharedPreferences prefs = getSharedPreferences(PREFS,
		// Context.MODE_PRIVATE);
		if (this.recipientNum==null || this.recipientNum.isEmpty()) {
			Log.e(TAG, "Recipient number is not set: "+this.recipientNum);
			MyUtils.alert("Recipient number not set", context);
		} else {

			String pubMod = MyKeyUtils.getPubMod(MyKeyUtils.PREFS_MY_KEYS, context);
			//String pubMod = MyKeyUtils.getPubMod(this.recipientNum, context);
			// String pubExp = prefs.getString(PREF_PUBLIC_EXP, DEFAULT_PREF);
			String pubExp = MyKeyUtils.getPubExp(MyKeyUtils.PREFS_MY_KEYS, context);
		
			// EditText recipient = (EditText) findViewById(R.id.InputRecipientNum);
			if (pubMod.length() != 0 && pubExp.length() != 0) {
				sendKeyExchangeSMS(this.recipientNum, pubMod, pubExp, context);
			} else {
				Log.e(TAG, "mod or exp of public key not found");
				MyUtils.alert("key not found, please generate first", context);
			}
		}
	}

	/*
	 * send health measurement securely message should have format:
	 * "gmstelehealth [encrypted-then-encoded measurement]"
	 */
	public void sendSecureSMS(Context context, String measurementStr) {
		if (measurementStr.length() > 117) {
			Log.e(TAG,
					"the measurement is too long for encryption with key size 1024 bits");
			return;
		}
		// Log.w(TAG,
		// "sending a secure SMS, recipient is "+this.recipientNum+" original message is "+this.message);
		// TODO check a public key for a recipient is stored
		RSAPublicKeySpec recipientsPubKey = MyKeyUtils.getRecipientsPublicKey(
				this.recipientNum, context);
		Log.i(TAG, "recipient's RSAPublicKeySpec is " + recipientsPubKey);
		if (recipientsPubKey == null) {
			Log.e(TAG, "recipient's public key could not be retrieved for "
					+ this.recipientNum);
			// MyUtils.alert("Public key not found for " + this.recipientNum,
			// context);
			// debugMessages.setText("recipient's public key could not be retrieved");
			return;
		}

		/*
		 * if (this.recipientNum == null || this.message == null ||
		 * this.message.isEmpty() || this.recipientNum.isEmpty()) { Log.e(TAG,
		 * "this should never happen but it does; either message or recipient is not supplied"
		 * ); MyUtils.alert("either message or recipient is not supplied",
		 * context); //
		 * debugMessages.setText("either message or recipient is not supplied");
		 * return; }
		 */

		Log.i(TAG, "sendSecureSMS(" + this.message + ", " + this.recipientNum
				+ ")");

		try {
			Log.d(TAG, "measurement before encryption: " + measurementStr
					+ " and recipient's key is not null "
					+ (recipientsPubKey != null));
			byte[] encrypted = MyKeyUtils.encryptMsg(measurementStr,
					recipientsPubKey);
			String processedMeasurementStr = Base64.encodeToString(encrypted,
					Base64.NO_WRAP);// NO_WRAP is
									// necessary,
									// otherwise the
									// string will be
									// broken into
									// multiple lines
									// i.e. CRLF or LF
									// characters are
									// included

			// TODO encode the main content of the message and compose the SMS
			// message

			String smsMsg = HEALTH_SMS + " " + processedMeasurementStr;
			Log.w(TAG, "measurement string after encryption: " + encrypted
					+ " and then after base64 encoding: "
					+ processedMeasurementStr);
			Log.w(TAG, "complete message to be sent: '" + smsMsg + "'");

			// set the final message to be sent
			this.message = smsMsg;

			// TextView debug = (TextView) findViewById(R.id.DebugMessages);
			// debug.append("Sending secure sms: '" + smsMsg +
			// "' with length: "+smsMsg.length());
			// TODO send the SMS message
			Log.w(TAG, "length of the message is " + smsMsg.length());
			if (smsMsg.length() > 160) {
				Log.i(TAG, "sms is " + this.message + " and recipient is "
						+ this.recipientNum);
				sendLongSMS(context);
				// for testing only
				// sendSMS(recipient, "gmstelehealth eKAoUlBFA9JEU31pRjHa");
				// //this message should be short enough or is it?
			} else {
				Log.i(TAG, "sms is " + this.message + " and recipient is "
						+ this.recipientNum);
				sendSMS(context);
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception happens", e);
		}

	}
}
