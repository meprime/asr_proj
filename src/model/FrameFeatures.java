package model;

public class FrameFeatures {

	public static final int FEATURES_COUNT = 13;
	
	private double[] values;

	public FrameFeatures() {
		values = new double[FEATURES_COUNT];
	}
	
	public double[] getValues() {
		return this.values;
	}
	
	public void setValues(double[] values) {
		this.values = values;
	}
	
	public double getValue(int i) {
		return values[i];
	}
	
	public boolean isNullFrame() {
		for(int i = 0; i < FEATURES_COUNT; i++)
			if(values[i] != 0)
				return false;
		return true;
	}
}
