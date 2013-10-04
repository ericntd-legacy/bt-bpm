package sg.edu.nus.omronhealth.spp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import sg.edu.nus.omronhealth.util.Base94Encode;

@SuppressLint("DefaultLocale")
public class BPMeasurementData extends OmronMeasurementData {

	public static final int UNIT_mmHG = 0;
	public static final int UNIT_kPa = 1;

	public static final String TAG = "BloodPressure";
	private static final boolean D = true;
	
	
	char UNo;
	int YY, MM, DD, hh, mm, ss;
	int unit;
	int sys;
	int dia, pulse;
	int bodyMovementFlag;
	int irregPulseFlag;
	
	private final String MYSQL_DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

	public char getUNo() {
		return UNo;
	}

	public int getYY() {
		return YY;
	}

	public int getMM() {
		return MM;
	}

	public int getDD() {
		return DD;
	}

	public int getHh() {
		return hh;
	}

	public int getMm() {
		return mm;
	}

	public int getSs() {
		return ss;
	}

	public int getUnit() {
		return unit;
	}

	public int getSys() {
		return sys;
	}

	public int getDia() {
		return dia;
	}

	public int getPulse() {
		return pulse;
	}

	public int getBodyMovementFlag() {
		return bodyMovementFlag;
	}

	public int getIrregPulseFlag() {
		return irregPulseFlag;
	}

	public void setUNo(char uNo) {
		UNo = uNo;
	}

	public void setYY(int yY) {
		YY = yY;
	}

	public void setMM(int mM) {
		MM = mM;
	}

	public void setDD(int dD) {
		DD = dD;
	}

	public void setHh(int hh) {
		this.hh = hh;
	}

	public void setMm(int mm) {
		this.mm = mm;
	}

	public void setSs(int ss) {
		this.ss = ss;
	}

	public void setUnit(int unit) {
		this.unit = unit;
	}

	public void setSys(int sys) {
		this.sys = sys;
	}

	public void setDia(int dia) {
		this.dia = dia;
	}

	public void setPulse(int pulse) {
		this.pulse = pulse;
	}

	public void setBodyMovementFlag(int bodyMovementFlag) {
		this.bodyMovementFlag = bodyMovementFlag;
	}

	public void setIrregPulseFlag(int irregPulseFlag) {
		this.irregPulseFlag = irregPulseFlag;
	}


	public BPMeasurementData(char UNo, int YY, int MM, int DD, int hh, int mm,
			int ss, int unit, int sys, int dia, int pulse,
			int bodyMovementFlag, int irregPulseFlag) {
		this.UNo = UNo;
		this.YY = YY;
		this.MM = MM;
		this.DD = DD;
		this.hh = hh;
		this.mm = mm;
		this.ss = ss;
		this.unit = unit;
		this.sys = sys;
		this.dia = dia;
		this.pulse = pulse;
		this.bodyMovementFlag = bodyMovementFlag;
		this.irregPulseFlag = irregPulseFlag;
	}
	
	public BPMeasurementData(Parcel in){
		readFromParcel(in);
	}

	@Override
	public String toString() {
		String unitStr;
		if (unit == UNIT_mmHG) {
			unitStr = "mmHG";
		} else if (unit == UNIT_kPa) {
			unitStr = "kPa";
		} else {
			unitStr = "UNKNOWN UNIT";
			Log.e(TAG, "Unknown unit: " + unit);
		}
		//String.format(en_US , "ss");
		return String
				.format("Measurement data:: User: %c, Date/time of measurement (YY/MM/DD hh:mm:ss) %d/%d/%d %d:%d:%d \n Sys: %d %s Dia: %d %s Pulse: %d bpm. Body movement flag: %d. Irregularity pulse flag: %d",
						UNo, YY, MM, DD, hh, mm, ss, sys, unitStr, dia,
						unitStr, pulse, bodyMovementFlag, irregPulseFlag);
	}
	
	@Override
	public String toSmsHumanString(){
		return String.format("On%d/%d/%d %d:%d:%d, sys:%d dia:%d pulse:%d (%d%d)",
				DD, MM, YY, hh, mm, ss,
				sys, dia, pulse,
				bodyMovementFlag, irregPulseFlag);
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Log.d(TAG, "writing parcel");

		dest.writeInt(UNo);
		dest.writeInt(YY);
		dest.writeInt(MM);
		dest.writeInt(DD);
		dest.writeInt(hh);
		dest.writeInt(mm);
		dest.writeInt(ss);
		dest.writeInt(sys);
		dest.writeInt(dia);
		dest.writeInt(pulse);
		dest.writeInt(bodyMovementFlag);
		dest.writeInt(irregPulseFlag);

	}
	
	private void readFromParcel(Parcel in){
		UNo = (char) in.readInt();
		YY = in.readInt();
		MM = in.readInt();
		DD = in.readInt();
		hh = in.readInt();
		mm = in.readInt();
		ss = in.readInt();
		sys = in.readInt();
		dia = in.readInt();
		pulse = in.readInt();
		bodyMovementFlag = in.readInt();
		irregPulseFlag = in.readInt();
	}

	
	public static final Parcelable.Creator<BPMeasurementData> CREATOR = new Parcelable.Creator<BPMeasurementData>() {
		public BPMeasurementData createFromParcel(Parcel in) {
			return new BPMeasurementData(in);
		}

		public BPMeasurementData[] newArray(int size) {
			return new BPMeasurementData[size];
		}
	};

	@Override
	public String toSmsMachineString() {
		//TODO: come up with a better solution to encrypt the whole message together and more securely with public-key cryptography
		return " " + measurementDateToStr() + measurementDataToStr();
		//return null;
	}
	
	public int dateToInt(){
		/*
		 * Concat date
		 * output MMDDYY
		 */
		int type = 1;
		int _date;
		if (YY > -1) {
			_date = type * 1000000 + MM * 10000 + DD * 100 + YY;
		} else {
			_date = type * 1000000 + 01 * 10000 + 01 * 100 + 1;
		}
		System.out.println("date: " + _date);
		return _date;
	}
	public int timeToInt() {
		/*
		 * output: hhmmss
		 */
		if (hh > -1) {
			return hh * 10000 + mm * 100 + ss;
		} else {
			return 0;
		}
	}
	public int measurementDataToInt() {
		/*
		 * Output: pulse sys dia [flags]
		 * 
		 * check flags by checking if it is odd/even
		 */
		int _data = pulse * 10000000 +
				    sys * 10000 +
				    dia * 10 +
				    bodyMovementFlag * 2 +
				    irregPulseFlag;
		System.out.println("measurementData: " + _data);
		return _data;
	}
	
	public String measurementDataToStr(){
		return Base94Encode.encodeBase94_data(measurementDataToInt());
	}
	
	public String measurementDateToStr(){
		return Base94Encode.encodeBase94(dateToInt()) + timeToStr();
	}
	
	private String timeToStr(){
		return Base94Encode.encodeBase94(timeToInt());
	}
	
	public String getDateTime() {
		String dateTime = String.format("%d/%d/%d %d:%d:%d", YY, MM, DD, hh, mm, ss);
		return dateTime;
	}
	
	//TODO: to return proper String for year, month date instead of integer
	//for example: day 1 should be String "0" instead
	public String getDateTimeMySQL() {
		//Calendar cal = new GregorianCalendar();
		Calendar cal = Calendar.getInstance();
		/*set(int year, int month, int date, int hourOfDay, int minute, int second)
		Sets the values for the fields YEAR, MONTH, DAY_OF_MONTH, HOUR, MINUTE, and SECOND.
		*/
		if (D) Log.w(TAG, "month is "+this.MM+" day is "+this.DD);
		//In Calendar class, January is month '0' instead of '1' ==> has to minus 1 before converting to Date Class for formatting
		cal.set(this.YY+2000, this.MM-1, this.DD, this.hh, this.mm, this.ss);
		//Testing
		//cal.set(13+2000, 9, 11, 15, 14, 4);
		//MySQL datetime format
		SimpleDateFormat formatter=new SimpleDateFormat(MYSQL_DATEFORMAT);
		Date dt = cal.getTime();
		String dateTime = formatter.format(dt);
		return dateTime;
	}

}


