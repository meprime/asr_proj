package testing;

import java.util.List;
import java.util.Map;

import model.FrameFeatures;
import model.acoustic.HMM;
import model.acoustic.HMMFactory;
import model.acoustic.HmmState;

public class StateDecoder {

	public String decode(List<FrameFeatures> features) {
		HMM searchHmm = HMMFactory.getInstance().getSearchHMM();
		double[][] probs = new double[features.size()][searchHmm.getStatesCount()];
		int[][] pars = new int[features.size()][searchHmm.getStatesCount()];
		for(int i = 0; i < searchHmm.getStatesCount(); i++) {
			for(int t = 0; t < features.size(); t++)
				probs[t][i] = Long.MIN_VALUE;
		}
		probs[0][0] = 0;
		for(int t = 1; t < features.size(); t++) {
			String print = "t=" + t + ": ";
			for(int j = 0; j < searchHmm.getStatesCount(); j++) {
				for(int k = 0; k < searchHmm.getStatesCount(); k++) {
					if(searchHmm.getTransitionProbablity(k, j) > 0) {
						double obsProb = searchHmm.getStates().get(j).getProbability(features.get(t));
						//					double p = probs[t-1][k] * searchHmm.getTransitionProbablity(k, j) * obsProb;
//						if(t == 1)
//							System.out.println(searchHmm.getTransitionProbablity(k, j) + ", " + obsProb + ", " + (Math.log(searchHmm.getTransitionProbablity(k, j)) + Math.log(obsProb)));
						double p = probs[t-1][k] + Math.log(searchHmm.getTransitionProbablity(k, j)) + Math.log(obsProb);
						//					if(searchHmm.getTransitionProbablity(k, j) > 0)
						//						System.out.println("transition prob: " + searchHmm.getTransitionProbablity(k, j));
//						if(p > Integer.MIN_VALUE)
//							System.out.println("t: " + t + ", p: " + p);
						if(p > probs[t][j]) {
//							System.out.println("t: " + t + ", p: " + p + ", j: " + j + ", k: " + k);
							probs[t][j] = p;
							pars[t][j] = k;
						}
					}
				}
				if(probs[t][j] > 0)
					print += j + "," + probs[t][j] + "; ";
			}
//			System.out.println(print);
		}
		int bestFinalState = 0;
		for(int i = 1; i < searchHmm.getStatesCount(); i++) {
//			System.out.println(String.format("%.8f", probs[features.size()-1][i]));
			if(probs[features.size()-1][i] > probs[features.size()-1][bestFinalState]) {
				bestFinalState = i;
			}
		}
		System.out.println(features.size());
		Map<HmmState, String> stateToWordMap = HMMFactory.getInstance().getStateToWordMap();
		String result = "";
		String lastWord = "";
		for(int t = features.size()-1, bestCurLevelState = bestFinalState; t >= 0; t--) {
			String word = stateToWordMap.get(searchHmm.getStates().get(bestCurLevelState));
			System.out.println(word + ", " + bestCurLevelState + ", " + probs[t][bestCurLevelState]);
			if(!word.equals(lastWord)) {
				lastWord = word;
				result = word + result;
			}
			bestCurLevelState = pars[t][bestCurLevelState];
		}
		return result;
	}
}

