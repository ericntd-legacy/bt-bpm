package sg.edu.nus.omronhealth.spp;

import android.os.Parcelable;

public abstract class OmronMeasurementData implements Parcelable{
	public abstract String toString();
	public abstract String toSmsHumanString();
	public abstract String toSmsMachineString();
	
	public abstract String measurementDataToStr();
	public abstract String measurementDateToStr();
}
