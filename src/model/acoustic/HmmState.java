package model.acoustic;

import java.util.UUID;

import model.FrameFeatures;

public class HmmState {
	private boolean isFinal;
	
	private double[] weights;
	private double[][] means;
	private double[][] covs;
	
	private String gene; 
	
	public HmmState(boolean isFinal, String uid) {
		this.isFinal = isFinal;
		weights = new double[2];
		means = new double[2][FrameFeatures.FEATURES_COUNT];
		covs = new double[2][FrameFeatures.FEATURES_COUNT];
		this.gene = uid;		
	}
	
	public HmmState(boolean isFinal) {
		this(isFinal, UUID.randomUUID().toString());
	}
	
	public String getGene() {
		return this.gene;
	}
	
	public boolean isFinal() {
		return this.isFinal;
	}
	
	public void setWeights(double w0, double w1) {
		this.weights[0] = w0;
		this.weights[1] = w1;
	}
	
	public void setWeights(double[] weights) {
		this.weights = weights;
	}
	
	public double[] getWeights() {
		return this.weights;
	}
	
	public double[][] getMeans() {
		return this.means;
	}
	
	public double[][] getCovs() {
		return this.covs;
	}
	
	public void setMeans(double[][] means) {
		this.means = means;
	}
	
	public void setCovs(double[][] covs) {
		this.covs = covs;
	}
	
	public double getProbability(FrameFeatures f) {
		if(f.isNullFrame()) {
			if(this.isFinal())
				return 1;
			else
				return 0;
		} else if(this.isFinal()) {
			return 0;
		} else {
			return getWeightedProbability(f, 0) + getWeightedProbability(f, 1);
		}
	}

	public double getWeightedProbability(FrameFeatures f, int mixtureInd) {
		int n = means[mixtureInd].length;
		double numerator = 0;
		double denominator = 0;
		for(int i = 0; i < n; i++) {
			double z = f.getValue(i) - means[mixtureInd][i];
			numerator += z * z / covs[mixtureInd][i];
			denominator += 2 * Math.PI * covs[mixtureInd][i];
		}
		double q = numerator;
		numerator /= -2;
		numerator = Math.exp(numerator);
		denominator = Math.sqrt(denominator);
		return weights[mixtureInd] * numerator / denominator;
	}
}