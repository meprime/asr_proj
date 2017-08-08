package model.params;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.FrameFeatures;
import model.acoustic.HmmState;

public class UpdateParams {

	private Set<Ksi> ksis;
	private Set<GammaGeneral> gammaGs;
	private Set<GammaSpecific> gammaSs;
	
	public UpdateParams() {
		ksis = new HashSet<UpdateParams.Ksi>();
		gammaGs = new HashSet<UpdateParams.GammaGeneral>();
		gammaSs = new HashSet<UpdateParams.GammaSpecific>();
	}
	
	public void addKsi(HmmState from, HmmState to, double val) {
		boolean found = false;
		for(Ksi k : ksis) {
			if(k.getFrom().equals(from) && k.getTo().equals(to)) {
				k.addToVal(val);
				found = true;
			}
		}
		if(!found) {
			Ksi k = new Ksi(from, to, val);
			this.ksis.add(k);
		}
	}
	
	public void addGamma(HmmState state, double val) {
		boolean found = false;
		for(GammaGeneral g : gammaGs) {
			if(g.getState().equals(state)) {
				g.addToVal(val);
				found = true;
			}
		}
		if(!found) {
			GammaGeneral g = new GammaGeneral(state, val);
			this.gammaGs.add(g);
		}
	}
	
	public void addGamma(HmmState state, int k, FrameFeatures v, double val) {
		boolean found = false;
		for(GammaSpecific g : gammaSs) {
			if(g.getState().equals(state) && g.getK() == k) {
				g.addToVals(val, v);
				found = true;
			}
		}
		if(!found) {
			GammaSpecific g = new GammaSpecific(state, k, v, val);
			this.gammaSs.add(g);
		}
	}
	
	public double getKsi(HmmState from, HmmState to) {
		for(Ksi k : ksis) {
			if(k.getFrom().equals(from) && k.getTo().equals(to)) {
				return k.val;
			}
		}
		return 0;
	}
	
	public double getGamma(HmmState state) {
		for(GammaGeneral g : gammaGs) {
			if(g.getState().equals(state)) {
				return g.val;
			}
		}
		return 0;
	}
	
	public double getGamma(HmmState state, int k) {
		for(GammaSpecific g : gammaSs) {
			if(g.getState().equals(state) && g.k == k) {
				return g.val;
			}
		}
		return 0;
	}
	
	public double[] getGammaWeightedObservationsSum(HmmState state, int k) {
		for(GammaSpecific g : gammaSs) {
			if(g.getState().equals(state) && g.k == k) {
				return g.getWeightedObservationsSum();
			}
		}
		return new double[FrameFeatures.FEATURES_COUNT];
	}
	
	public double[] getCovarianceSum(HmmState state, int k) {
		for(GammaSpecific g : gammaSs) {
			if(g.getState().equals(state) && g.k == k) {
				return g.getCovarianceSum();
			}
		}
		return new double[FrameFeatures.FEATURES_COUNT];
	}
	
	class Ksi {
		private HmmState from;
		private HmmState to;
		private double val;
		
		public Ksi(HmmState from, HmmState to, double val) {
			this.from = from;
			this.to = to;
			this.val = val;
		}
		
		public HmmState getFrom() {
			return this.from;
		}

		public HmmState getTo() {
			return this.to;
		}
		
		public void addToVal(double d) {
			this.val += d;
		}
	}
	
	class GammaGeneral {
		private HmmState state;
		private double val;
		
		public GammaGeneral(HmmState state, double val) {
			this.state = state;
			this.val = val;
		}
		
		public HmmState getState() {
			return state;
		}
		
		public void addToVal(double d) {
			this.val += d;
		}
	}
	
	class GammaSpecific {
		private HmmState state;
		private int k;
		private double val;
		private Map<FrameFeatures, Double> observations;

		public GammaSpecific(HmmState state, int k, FrameFeatures o, double val) {
			this.state = state;
			this.k = k;
			this.val = val;
			this.observations = new HashMap<FrameFeatures, Double>();
			observations.put(o, val);
		}
		
		public HmmState getState() {
			return state;
		}
		
		public int getK() {
			return this.k;
		}
		
		public void addToVals(double d, FrameFeatures o) {
			this.val += d;
			this.observations.put(o, d);
		}
		
		public double[] getWeightedObservationsSum() {
			double[] result = new double[FrameFeatures.FEATURES_COUNT];
			for(Entry<FrameFeatures, Double> entry : observations.entrySet()) {
				FrameFeatures o = entry.getKey();
				for(int i = 0; i < FrameFeatures.FEATURES_COUNT; i++) {
					result[i] += o.getValue(i) * entry.getValue();
				}
			}
			return result;
		}
		
		public double[] getCovarianceSum() {
			double[] result = new double[FrameFeatures.FEATURES_COUNT];
			double[] mean = state.getMeans()[k];
			for(Entry<FrameFeatures, Double> entry : observations.entrySet()) {
				FrameFeatures o = entry.getKey();
				for(int i = 0; i < FrameFeatures.FEATURES_COUNT; i++) {
					result[i] += entry.getValue() * Math.pow(o.getValue(i) - mean[i], 2);
				}
			}
			return result;
		}
	}
}
