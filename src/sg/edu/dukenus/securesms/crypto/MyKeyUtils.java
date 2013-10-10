package sg.edu.dukenus.securesms.crypto;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import sg.edu.dukenus.securesms.sms.SmsSender;
import sg.edu.dukenus.securesms.utils.MyUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

public class MyKeyUtils {
	// debugging
	private static final String TAG = "MyKeyUtils";

	// sharedpreferences
	public static final String PREFS_MY_KEYS = "MyKeys";

	public static final String PREF_PUBLIC_MOD = "PublicModulus";
	public static final String PREF_PUBLIC_EXP = "PublicExponent";
	public static final String PREF_PRIVATE_MOD = "PrivateModulus";
	public static final String PREF_PRIVATE_EXP = "PrivateExponent";

	public static final String DEFAULT_PREF = "";
	public static final String DEFAULT_CONTACT_NUM = "+6584781395";
	public static final int DEFAULT_KEY_SIZE = 1024;
	/*
	 * get the modulus from sharedpreferences
	 */
	public static String getPubMod(String contactID, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(contactID,
				Context.MODE_PRIVATE);

		String pubMod = prefs.getString(PREF_PUBLIC_MOD, DEFAULT_PREF);
		
		return pubMod;
	}
	
	/*
	 * get the modulus from sharedpreferences
	 */
	public static String getPubExp(String contactID, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(contactID,
				Context.MODE_PRIVATE);

		String pubExp = prefs.getString(PREF_PUBLIC_EXP, DEFAULT_PREF);
		
		return pubExp;
	}

	protected static RSAPublicKeySpec getPublicKeySpec(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_MY_KEYS,
				Context.MODE_PRIVATE);

		String pubMod = prefs.getString(PREF_PRIVATE_MOD, DEFAULT_PREF);
		String pubExp = prefs.getString(PREF_PRIVATE_EXP, DEFAULT_PREF);
		// String recipient = prefs.getString(PREF_RECIPIENT_NUM, DEFAULT_PREF);
		if (!pubMod.isEmpty() && !pubExp.isEmpty()) {
			byte[] pubModBA = Base64.decode(pubMod, Base64.DEFAULT);
			byte[] pubExpBA = Base64.decode(pubExp, Base64.DEFAULT);
			BigInteger pubModBI = new BigInteger(pubModBA);
			BigInteger pubExpBI = new BigInteger(pubExpBA);

			RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(pubModBI,
					pubExpBI);

			return pubKeySpec;
		} 
			Log.w(TAG, "public key not generated");
			return null;
	}
	
	public static RSAPublicKeySpec getRecipientsPublicKey(String contactNum, Context context) {
		Log.w(TAG, "retrieving public key for contact "+contactNum);
		SharedPreferences prefs = context.getSharedPreferences(contactNum,
				Context.MODE_PRIVATE);

		String pubMod = prefs.getString(PREF_PUBLIC_MOD, DEFAULT_PREF);
		String pubExp = prefs.getString(PREF_PUBLIC_EXP, DEFAULT_PREF);
		Log.w(TAG, "the public modulus is "+pubMod+" and exponent is "+pubExp+ " for "+contactNum);
		// String recipient = prefs.getString(PREF_RECIPIENT_NUM, DEFAULT_PREF);
		if (!pubMod.isEmpty() && !pubExp.isEmpty()) {
			Log.i(TAG, "great! public key found for "+contactNum+" with modulus "+pubMod +" and exponent "+pubExp);
			byte[] pubModBA = Base64.decode(pubMod, Base64.DEFAULT);
			byte[] pubExpBA = Base64.decode(pubExp, Base64.DEFAULT);
			BigInteger pubModBI = new BigInteger(pubModBA);
			BigInteger pubExpBI = new BigInteger(pubExpBA);
			Log.i(TAG, "public modulus is "+pubModBI+" and public exponent is "+pubExpBI+" in base 256 "+pubModBA+" "+pubExpBA);
			
			// do I need to catch any exception for the following?
			RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(pubModBI,
					pubExpBI);
			//X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);

			return pubKeySpec;
		}
		Log.w(TAG, "recipient's public key not found");
		return null;
		
	}

	public static byte[] encryptMsg(String msg, RSAPublicKeySpec pubKeySpec) {
		if (msg != null && pubKeySpec != null && !msg.isEmpty()) {
			try {
				KeyFactory fact = KeyFactory.getInstance("RSA");

				PublicKey pubKey = fact.generatePublic(pubKeySpec);

				// TODO encrypt the message and send it
				//Cipher cipher = Cipher.getInstance("RSA/None/NoPadding");
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				//Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
				cipher.init(Cipher.ENCRYPT_MODE, pubKey);
				Log.d(TAG, "cipher block size is "+cipher.getBlockSize());
				byte[] msgByteArray = msg.getBytes();
				byte[] cipherData = new byte[cipher.getOutputSize(msgByteArray.length)];
					cipherData = cipher.doFinal(msgByteArray);
					Log.d(TAG, "output size is "+cipher.getOutputSize(msgByteArray.length));
					Log.d(TAG, "is the measurement already broken into chunks here? "+(new String(cipherData)));
					return cipherData;
				
				

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
			} catch (Exception e) {
				Log.e(TAG, "", e);
			} /*catch (NoSuchProviderException e) {
				Log.e(TAG, "", e);
			}*/
		}
		return null;
	}
	
	/*
	 * Check if keys are found in the app's SharedPreferences if not, generate
	 * them and save them to the app's SharedPreferences
	 */
	public static boolean getKeys(int keySize, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_MY_KEYS,
				Context.MODE_PRIVATE);
		String pubMod = prefs.getString(PREF_PUBLIC_MOD, DEFAULT_PREF);
		String pubExp = prefs.getString(PREF_PUBLIC_EXP, DEFAULT_PREF);
		String privateMod = prefs.getString(PREF_PRIVATE_MOD, DEFAULT_PREF);
		String privateExp = prefs.getString(PREF_PRIVATE_EXP, DEFAULT_PREF);

		boolean keysExist = false;

		if (!pubMod.isEmpty() && !pubExp.isEmpty()
				&& !privateMod.isEmpty()
				&& !privateExp.isEmpty()) {
			Log.i(TAG, "keys found, not regenerating");
			keysExist = true;
		} else {

			keysExist = false;
		}
		if (!keysExist) {
			Log.w(TAG, "keys not found, generating");
			return generateKeys(keySize, context);
			
		} else {
			//MyUtils.alert("Keys exist, not generating", MainActivity.this);
			byte[] myPubModBA = Base64.decode(pubMod, Base64.DEFAULT);
			byte[] myPubExpBA = Base64.decode(pubExp, Base64.DEFAULT);
			byte[] myPrivateModBA = Base64.decode(privateMod, Base64.DEFAULT);
			byte[] myPrivateExpBA = Base64.decode(privateExp, Base64.DEFAULT);

			BigInteger myPubModBI = new BigInteger(myPubModBA);
			BigInteger myPubExpBI = new BigInteger(myPubExpBA);

			BigInteger myPrivateModBI = new BigInteger(myPrivateModBA);
			BigInteger myPrivateExpBI = new BigInteger(myPrivateExpBA);

			Log.w(TAG, "the current user's stored public key modulus is "
					+ myPubModBI + " while the exponent is " + myPubExpBI
					+ " === private key modulus is " + myPrivateModBI
					+ " and exponent is " + myPrivateExpBI);	
			return true;
		}
	}
	
	public static boolean generateKeys(int keySize, Context context) {
		Log.i(TAG, "keys not found, generating now");
		try {

			/*
			 * Generating private and public key using RSA algorithm saving
			 * the keys to the app's shared preferences
			 */
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keySize);
			KeyPair kp = kpg.genKeyPair();
			Key publicKey = kp.getPublic();
			Key privateKey = kp.getPrivate();

			KeyFactory fact = KeyFactory.getInstance("RSA");
			RSAPublicKeySpec pub = fact.getKeySpec(publicKey,
					RSAPublicKeySpec.class);
			RSAPrivateKeySpec priv = fact.getKeySpec(privateKey,
					RSAPrivateKeySpec.class);

			/*
			 * save the public key to the app's SharedPreferences
			 */
			savePublicKey(pub, context);
			/*
			 * save the private key to the app's SharedPreferences
			 */
			savePrivateKey(priv, context);
			
			return true;

		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "RSA algorithm not available", e);
		} catch (InvalidKeySpecException e) {
			Log.e(TAG, "", e);
		}
		/*
		 * catch (IOException e) { Log.e(TAG,
		 * "Having trouble saving key file", e); }
		 */
		return false;
	}
	
	public static void savePublicKey(String mod, String exp, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_MY_KEYS,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();

		prefsEditor.putString(PREF_PUBLIC_MOD, mod);
		prefsEditor.putString(PREF_PUBLIC_EXP, exp);
		// prefsEditor.putString(PREF_PRIVATE_MOD, DEFAULT_PRIVATE_MOD);
		prefsEditor.commit();
	}

	private static void savePublicKey(RSAPublicKeySpec pubKey, Context context) {
		BigInteger pubModBI = pubKey.getModulus();
		BigInteger pubExpBI = pubKey.getPublicExponent();

		byte[] pubModBA = pubModBI.toByteArray();// Base64.encodeInteger(pubModBI);
													// // for some strange
													// reason this throws
													// NoSuchMethodError
		byte[] pubExpBA = pubExpBI.toByteArray();// Base64.encodeInteger(pubExpBI);

		try {
			String pubModBase64Str = Base64.encodeToString(pubModBA,
					Base64.NO_WRAP);
			String pubExpBase64Str = Base64.encodeToString(pubExpBA,
					Base64.NO_WRAP);

			Log.i(TAG, "the modulus of the current user's public key is "
					+ pubModBI + " and the exponent is " + pubExpBI
					+ " | encoded module is " + pubModBase64Str
					+ " | encoded exponent is " + pubExpBase64Str);

			savePublicKey(pubModBase64Str, pubExpBase64Str, context);

		} catch (NoSuchMethodError e) {
			Log.e(TAG, "Base64.encode() method not available", e);
		}
		// TODO extract the modulus and exponent and save them
	}

	public static void savePrivateKey(RSAPrivateKeySpec privateKey, Context context) {
		BigInteger privateModBI = privateKey.getModulus();
		BigInteger privateExpBI = privateKey.getPrivateExponent();

		byte[] privateModBA = privateModBI.toByteArray();// Base64.encodeInteger(pubModBI);
															// // for some
															// strange reason
															// this throws
															// NoSuchMethodError
		byte[] privateExpBA = privateExpBI.toByteArray();// Base64.encodeInteger(pubExpBI);

		try {
			String privateModBase64Str = Base64.encodeToString(privateModBA,
					Base64.NO_WRAP);
			String privateExpBase64Str = Base64.encodeToString(privateExpBA,
					Base64.NO_WRAP);
			Log.i(TAG, "the modulus of the current user's private key is "
					+ privateModBI + " and the exponent is " + privateExpBI
					+ " | encoded module is " + privateModBase64Str
					+ " | encoded exponent is " + privateExpBase64Str);

			savePrivateKey(privateModBase64Str, privateExpBase64Str, context);

		} catch (NoSuchMethodError e) {
			Log.e(TAG, "Base64.encode() method not available", e);
		}
	}

	private static void savePrivateKey(String mod, String exp, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_MY_KEYS,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();

		prefsEditor.putString(PREF_PRIVATE_MOD, mod);
		prefsEditor.putString(PREF_PRIVATE_EXP, exp);
		// prefsEditor.putString(PREF_PRIVATE_MOD, DEFAULT_PRIVATE_MOD);
		prefsEditor.commit();
	}
	
	public static void requestForKey(String contactNum, Context context) {
		Log.w(TAG, "hey why is this not running?");
		// get user's own keys
		boolean keys = MyKeyUtils.getKeys(DEFAULT_KEY_SIZE, context);
		
		if (keys) {
			Log.w(TAG, "user's own keys found");
			SmsSender smsSender = new SmsSender(contactNum);
		
			smsSender.sendKeyExchangeSMS(context);
		} else {
			Log.e(TAG, "could not find exisiting keys or generate new keys of user's own");
		}
		
	}

}
