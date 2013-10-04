package sg.edu.nus.omronhealth.spp;

//import android.os.Parcelable;
import android.util.Log;

public class BloodPressureMon extends OmronBaseClass {

	/* User profile */
	private int unit;
	private char userNum;

	public static final String TAG = "BloodPressure";

	public BloodPressureMon() {
		super();
		deviceType = BLOOD_PRESSURE_MON;
	}

	public String profileString() {
		return "User: " + userNum;
	}

	public void handleProfileData(byte[] frame, int len) {
		try {
			checkChecksum(frame, len);
		} catch (ChecksumError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		unit = frame[8];
		userNum = (char) frame[9];
	}



	public OmronMeasurementData handleMeasurementData(byte[] frame, int len,
			int dataNum) {
		char UNo;
		int YY, MM, DD, hh, mm, ss;
		int unit;
		int sys;
		int dia, pulse;
		int bodyMovementFlag;
		int irregPulseFlag;

		try {
			checkChecksum(frame, len);
		} catch (ChecksumError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		UNo = (char) frame[3];
		YY = frame[4];
		MM = frame[5];
		DD = frame[6];
		hh = frame[7];
		mm = frame[8];
		ss = frame[9];
		unit = frame[10];
		sys = ((int) frame[11] & 0xff) * 256 + frame[12]; // XXX
		// TODO: fix this
		if (sys < 0) sys += 256;
		Log.d(TAG, "frame 11:" + frame[11]);
		Log.d(TAG, "frame 11:" + frame[12]);

		dia = frame[13];
		pulse = frame[14];
		bodyMovementFlag = 0; // TODO
		irregPulseFlag = 0; // TODO

		BPMeasurementData bp = new BPMeasurementData(UNo, YY, MM, DD, hh, mm,
				ss, unit, sys, dia, pulse, bodyMovementFlag, irregPulseFlag);
		Log.d(TAG, "measurement data: " + bp);
		return bp;
	}

}
