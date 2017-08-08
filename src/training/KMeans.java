package training;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import model.FrameFeatures;

public class KMeans {

	private static final int L = 13;
	private static final int MAX_ITERATIONS = 20;
	private static final double STOP_THRESHOLD = 1;
	
	Collection<FrameFeatures> vectors;
	private double[][] means = new double[2][L];
	List<Set<FrameFeatures>> assignments;
	
	public KMeans(Collection<FrameFeatures> vectors) {
		this.vectors = vectors;
	}
	
	public void runKMeans() {
		double[] mean = this.calcMean(vectors);
		means[0] = this.perturb(mean);
		means[1] = this.perturb(mean);
		assignments = new ArrayList<Set<FrameFeatures>>();
		assignments.add(new HashSet<FrameFeatures>());
		assignments.add(new HashSet<FrameFeatures>());
		double distSum = 0;
		for(FrameFeatures v : vectors) {
			double d0 = this.distance(v.getValues(), means[0]);
			double d1 = this.distance(v.getValues(), means[1]);
			assignments.get(d0 < d1 ? 0 : 1).add(v);
			distSum += d0 < d1 ? d0 : d1;
		}
		for(int i = 0; i < MAX_ITERATIONS; i++) {
			means[0] = this.calcMean(assignments.get(0));
			means[1] = this.calcMean(assignments.get(1));
//			for(int x = 0; x < 2; x++) {
//				String s = "";
//				for(int y = 0; y < L; y++)
//					s += means[x][y] + " ";
//				System.out.println(s);
//			}
			double newDistSum = 0;
			assignments.set(0, new HashSet<>());
			assignments.set(1, new HashSet<>());
			for(FrameFeatures v : vectors) {
				double d0 = this.distance(v.getValues(), means[0]);
				double d1 = this.distance(v.getValues(), means[1]);
				assignments.get(d0 < d1 ? 0 : 1).add(v);
				newDistSum += d0 < d1 ? d0 : d1;
			}
			if(distSum - newDistSum < STOP_THRESHOLD)
				break;
			distSum = newDistSum;
		}
	}
	
	public double[] calcMean(Collection<FrameFeatures> vectors) {
		double[] mean = new double[L];
		for(FrameFeatures v : vectors) {
			for(int i = 0; i < L; i++)
				mean[i] += v.getValue(i);
		}
		for(int i = 0; i < L; i++)
			mean[i] /= vectors.size();
		return mean;
	}
	
	public double[] perturb(double[] vector) {
		double[] result = new double[L];
		Random random = new Random();
		for(int i = 0; i < L; i++) {
			double d = (random.nextDouble() - 0.5) / 1000; // so it falls between -0.5 and 0.5
			result[i] = vector[i] + d;
		}
		return result;
	}
	
	public double distance(double[] v1, double[] v2) {
		double d = 0;
		for(int i = 0; i < L; i++) {
			d += (v1[i] - v2[i]) * (v1[i] - v2[i]);
		}
		return Math.sqrt(d);
	}
	
	public double[][] getMeans() {
		return means;
	}
	
	public double[][] getCovs() {
		double[][] covs = new double[2][L];
		covs[0] = this.getCovs(0);
		covs[1] = this.getCovs(1);
		return covs;
	}
	
	public double[] getCovs(int clusterInd) {
		Set<FrameFeatures> vectors = assignments.get(clusterInd);
		double[] cov = new double[L];
		for(FrameFeatures v : vectors) {
			for(int j = 0; j < L; j++) {
				cov[j] += Math.pow(v.getValue(j) - means[clusterInd][j], 2);
			}
		}
		return cov;
	}
	
	public double[] getWeights() {
		double[] weights = {
				(double)assignments.get(0).size() / vectors.size(),
				(double)assignments.get(1).size() / vectors.size()
		};
		return weights;
	}
}
