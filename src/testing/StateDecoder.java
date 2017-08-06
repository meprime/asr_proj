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
		for(int i = 0; i < features.size(); i++) {
			probs[0][0] = 1;
		}
		for(int t = 0; t < features.size(); t++) {
			for(int j = 0; j < searchHmm.getStatesCount(); j++) {
				for(int k = 0; k < searchHmm.getStatesCount(); k++) {
					double obsProb = searchHmm.getStates().get(j).getProbability(features.get(t));
					double p = probs[t-1][k] * searchHmm.getTransitionProbablity(k, j) * obsProb;
					if(p > probs[t][j]) {
						probs[t][j] = p;
						pars[t][j] = k;
					}
				}
			}
		}
		int bestFinalState = 0;
		for(int i = 1; i < searchHmm.getStatesCount(); i++) {
			if(probs[features.size()-1][i] > probs[features.size()-1][bestFinalState]) {
				bestFinalState = i;
			}
		}
		Map<HmmState, String> stateToWordMap = HMMFactory.getInstance().getStateToWordMap();
		String result = "";
		String lastWord = "";
		for(int t = features.size()-1, bestCurLevelState = bestFinalState; t >= 0; t--) {
			String word = stateToWordMap.get(searchHmm.getStates().get(bestCurLevelState));
			if(!word.equals(lastWord)) {
				lastWord = word;
				result = word + result;
			}
			bestCurLevelState = pars[t][bestCurLevelState];
		}
		return result;
	}
}

