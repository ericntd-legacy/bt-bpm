package sg.edu.nus.omronhealth.spp;

import android.os.Parcel;

public class WeighingScale extends OmronBaseClass {
	
	//profile
	private int unit;
	private char user;
	private int sex;
	private int YY,MM,DD;
	private int height;

	public WeighingScale() {
		super();
		deviceType = BLOOD_PRESSURE_MON;
	}

	@Override
	public void handleProfileData(byte[] frame, int len) {
		try {
			checkChecksum(frame,len);
		} catch (ChecksumError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		unit = frame[4];
		user = (char) frame[5];
		YY = frame[6];
		MM = frame[7];
		DD = frame[8];
		sex = frame[9];
		height = ((int) frame[10] & 0xff ) * 256 + frame[11]; //XXX
		
		

	}

	@Override
	public String profileString() {
		return "User: " + user;
	}
	
	private class WSMeasurementData extends OmronMeasurementData{
		char user;
		int mYY, mMM, mDD;
		int mhh, mmm;
		int unit;
		int sex;
		int weight; //coded
		int height; //coded
		int bmi; //coded
		int bodyFatRate; //coded
		int basalMetabolism;
		int visceralFatLevel; //coded
		int bodyAge;
		int skeletalMuscleRateWB; //coded
		

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public String toSmsHumanString(){
			return "";
		}


		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}


		@Override
		public void writeToParcel(Parcel dest, int flags) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String toSmsMachineString() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String measurementDataToStr() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String measurementDateToStr() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	@Override
	public OmronMeasurementData handleMeasurementData(byte[] frame, int len,
			int dataNum) {
		// TODO Auto-generated method stub
		return null;
	}

}
