package training;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import main.Debugger;
import main.MyProperties;
import model.FrameFeatures;
import model.Phoneme;
import model.acoustic.HMM;
import model.acoustic.HMMFactory;
import model.acoustic.HmmState;
import model.acoustic.StateInitializer;
import model.acoustic.Transition;
import model.params.UpdateParams;

public class Trainer {
	
	private List<String> utterances;
	private List<List<FrameFeatures>> features;

	public Trainer(String trainPath) throws FileNotFoundException {
		FeaturesExtractor extractor = new FeaturesExtractor(trainPath);
		extractor.extractFeaturesAndUtterances();
		this.utterances = extractor.getUtterances();
		this.features = extractor.getAllFeatures();
	}
	
	public void train(int epochCount) {
		List<HMM> longHmms = new ArrayList<HMM>();
		Map<Phoneme, HMM> baseHmms = HMMFactory.getInstance().getBaseHmms();
		System.out.println("About to create HMMs for utterances...");
		for(String u : utterances) {
			List<HMM> hmms = new ArrayList<HMM>();
			hmms.add(baseHmms.get(Phoneme.SIL));
			for(int i = 0; i < u.length(); i++) {
				hmms.add(baseHmms.get(Phoneme.getByChar(u.charAt(i))));
			}
			hmms.add(baseHmms.get(Phoneme.SIL));
			longHmms.add(HMMFactory.getInstance().concatenate(hmms));
		}
		System.out.println("HMMs for utterances created.");
		// initialize models
		System.out.println("About to initialize models...");
		Map<HmmState, StateInitializer> initializers = new HashMap<>();
		for(int i = 0; i < utterances.size(); i++) {
			HMM hmm = longHmms.get(i);
			List<FrameFeatures> hmmFeatures = features.get(i);
			int c = hmm.getNonFinalStatesCount();
			int floor = hmmFeatures.size() / c;
			int ceilCount = hmmFeatures.size() - floor * c;
			int nextFeatureInd = 0;
			int processedStatesCount = 0;
			for(HmmState state : hmm.getStates()) {
				if(state.isFinal())
					continue;
				StateInitializer init = initializers.containsKey(state) ? initializers.get(state) : new StateInitializer(state);
				initializers.put(state, init);
				for(int j = nextFeatureInd; j < nextFeatureInd + floor; j++)
					init.addToInitialFeatures(hmmFeatures.get(j));
				if(processedStatesCount < ceilCount)
					init.addToInitialFeatures(hmmFeatures.get(nextFeatureInd + floor));
				nextFeatureInd += floor + (processedStatesCount < ceilCount ? 1 : 0);
				processedStatesCount++;
			}
		}
		for(StateInitializer init : initializers.values()) {
			init.initialize();
		}
		System.out.println("Models initialized.");
		if(MyProperties.getInstance().isDebug())
			viewModelParams(longHmms);
		for(int i = 0; i < epochCount; i++)	{
			System.out.println("Starting " + i + "th epoch...");
			this.update(features, longHmms);
			System.out.println(i + "th epoch finished.");
		}
		if(MyProperties.getInstance().isDebug()) {
			printTrainedBaseHmms();
			viewModelParams(longHmms);
		}
	}
	
	public void update(List<List<FrameFeatures>> allFeatures, List<HMM> hmms) {
		UpdateParams params = new UpdateParams();
		for(int k = 0; k < hmms.size(); k++) {
			System.out.println(utterances.get(k));
			List<FrameFeatures> features = allFeatures.get(k);
			HMM hmm = hmms.get(k);
			double[][] alpha = ForwardBackward.getInstance().forward(hmm, features);
			double[][] beta = ForwardBackward.getInstance().backward(hmm, features);
			for(int t = 0; t < features.size()-1; t++) {
				for(int i = 0; i < hmm.getStatesCount(); i++) {
					double gamma = 0;
					for(int j = 0; j < hmm.getStatesCount(); j++) {
						double ksi = beta[t+1][j] * alpha[t][i] * hmm.getStates().get(j).getProbability(features.get(t+1)) * hmm.getTransitionProbablity(i, j);
						if(ksi > 0) {
							params.addKsi(hmm.getStates().get(i), hmm.getStates().get(j), ksi);
							gamma += ksi;
						}
					}
					if(gamma > 0) {
						params.addGamma(hmm.getStates().get(i), gamma);
					}
					// TODO BE CAREFUL!
					double p = hmm.getStates().get(i).getProbability(features.get(t));
					if(p > 0) {
						double gamma0 = gamma * hmm.getStates().get(i).getWeightedProbability(features.get(t), 0) / p;
						double gamma1 = gamma * hmm.getStates().get(i).getWeightedProbability(features.get(t), 1) / p;
						params.addGamma(hmm.getStates().get(i), 0, features.get(t), gamma0);
						params.addGamma(hmm.getStates().get(i), 1, features.get(t), gamma1);
					} else if(!hmm.getStates().get(i).isFinal() && !features.get(t).isNullFrame()) {
						System.out.println("0 denominator in calculating gamma. " + p);
					}
				}
			}
		}
		Set<Transition> updatedTransitions = new HashSet<Transition>();
		Set<HmmState> updatedState = new HashSet<HmmState>();
		for(int k = 0; k < hmms.size(); k++) {
			// update transitions
			for(Transition t : hmms.get(k).getTransitions()) {
				if(!updatedTransitions.contains(t)) {
					updatedTransitions.add(t);
					double ksi = params.getKsi(t.getFrom(), t.getTo());
					if(ksi > 0) {
						double gamma = params.getGamma(t.getFrom());
						t.setProbability(ksi / gamma);
					}
				}
			}
			// update mixture params
			for(HmmState state : hmms.get(k).getStates()) {
				if(!updatedState.contains(state) && !state.isFinal()) {
					updatedState.add(state);
					// update weights
					double[] gamma = {params.getGamma(state, 0), params.getGamma(state, 1)};
					state.setWeights(gamma[0] / (gamma[0] + gamma[1]), gamma[1] / (gamma[0] + gamma[1]));
					// update means
					double[][] means = state.getMeans();
					for(int i = 0; i < 2; i++) {
						if(gamma[i] > 0) {
							means[i] = params.getGammaWeightedObservationsSum(state, i);
							for(int j = 0; j < FrameFeatures.FEATURES_COUNT; j++)
								means[i][j] /= gamma[i];
						}
					}
					state.setMeans(means); // probably not required!
					// update covariance
					double[][] covs = state.getCovs();
					for(int i = 0; i < 2; i++) {
						if(gamma[i] > 0) {
							covs[i] = params.getCovarianceSum(state, i);
							for(int j = 0; j < FrameFeatures.FEATURES_COUNT; j++)
								covs[i][j] /= gamma[i];
						}
					}
					state.setCovs(covs); // probably not required!
				}
			}
		}
		
	}

	private void viewModelParams(List<HMM> hmms) {
		Scanner scanner = Debugger.getInstance().getSysinScanner();
		System.out.println("checking models params...");
		while(scanner.hasNextLine()) {
			String command = scanner.nextLine();
			if(command.equals("end"))
				break;
			if(command.equals("state")) {
				int hmmInd = scanner.nextInt();
				int stateInd = scanner.nextInt();
				HmmState state = hmms.get(hmmInd).getStates().get(stateInd);
				System.out.println("weights: " + state.getWeights()[0] + " " + state.getWeights()[1]);
				System.out.println("means:");
				String[] mean = {"", ""};
				for(int i = 0; i < 13; i++) {
					mean[0] += state.getMeans()[0][i] + " ";
					mean[1] += state.getMeans()[1][i] + " ";
				}
				System.out.println(mean[0]);
				System.out.println(mean[1]);
				System.out.println("covs:");
				String[] cov = {"", ""};
				for(int i = 0; i < 13; i++) {
					cov[0] += state.getCovs()[0][i] + " ";
					cov[1] += state.getCovs()[1][i] + " ";
				}
				System.out.println(cov[0]);
				System.out.println(cov[1]);
			}
		}
		System.out.println("models params checked.");
	}
	
	private void printTrainedBaseHmms() {
		for(Map.Entry<Phoneme, HMM> entry : HMMFactory.getInstance().getBaseHmms().entrySet()) {
			System.out.println(entry.getKey().toString());
			HMM hmm = entry.getValue();
			for(int i = 0; i < hmm.getStatesCount(); i++)
				for(int j = 0; j < hmm.getStatesCount(); j++) {
					if(hmm.getTransitionProbablity(i, j) > 0) {
						System.out.println(i + " -> " + j + ": " + hmm.getTransitionProbablity(i, j));
					}
				}
		}
	}
}
