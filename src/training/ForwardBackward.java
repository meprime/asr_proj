package training;

import java.util.List;

import model.FrameFeatures;
import model.acoustic.HMM;

public class ForwardBackward {

	private static ForwardBackward instance;
	public static ForwardBackward getInstance() {
		if(instance == null)
			instance = new ForwardBackward();
		return instance;
	}
	
	public double[][] forward(HMM hmm, List<FrameFeatures> features) {
		double[][] alpha = new double[features.size()][hmm.getStatesCount()];
		// initialization
		double likelihood = 0;
		alpha[0][0] = hmm.getInitialState().getProbability(features.get(0)) / hmm.getInitialState().getProbability(features.get(0));
		for(int t = 1; t < features.size(); t++) {
			double c = 0;
			for(int i = 0; i < hmm.getStatesCount(); i++) {
				for(int j = 0; j < hmm.getStatesCount(); j++) {
					alpha[t][i] += alpha[t-1][j] * hmm.getTransitionProbablity(j, i);
				}
				double p = hmm.getStates().get(i).getProbability(features.get(t));
				alpha[t][i] *= p;
				c += alpha[t][i];
			}
			for(int i = 0; i < hmm.getStatesCount(); i++) {
				alpha[t][i] /= c;
			}
			likelihood -= Math.log(c);
		}
		System.out.println("likelihood: " + likelihood);
		return alpha;
	}

	public double[][] backward(HMM hmm, List<FrameFeatures> features) {
		double[][] beta = new double[features.size()][hmm.getStatesCount()];
		// initialization
		for(int i = 0; i < hmm.getStatesCount(); i++) {
			beta[features.size()-1][i] = 1.0 / hmm.getStatesCount();
		}
		for(int t = features.size()-2; t >= 0; t--) {
			double c = 0;
			for(int i = 0; i < hmm.getStatesCount(); i++) {
				for(int j = 0; j < hmm.getStatesCount(); j++) {
					beta[t][i] += beta[t+1][j] 
							* hmm.getTransitionProbablity(i, j) 
							* hmm.getStates().get(j).getProbability(features.get(t+1));
				}
				c += beta[t][i];
			}
			for(int i = 0; i < hmm.getStatesCount(); i++) {
				beta[t][i] /= c;
			}
		}
		return beta;
	}
	
}

