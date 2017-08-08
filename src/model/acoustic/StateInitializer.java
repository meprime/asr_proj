package model.acoustic;

import java.util.HashSet;
import java.util.Set;

import training.KMeans;
import model.FrameFeatures;

public class StateInitializer {

	private HmmState state;
	private Set<FrameFeatures> initialFeatures;
	
	public StateInitializer(HmmState state) {
		this.state = state;
		this.initialFeatures = new HashSet<FrameFeatures>();
	}
	
	public void addToInitialFeatures(FrameFeatures feature) {
		this.initialFeatures.add(feature);
	}
	
	public void initialize() {
		KMeans kMeans = new KMeans(initialFeatures);
		kMeans.runKMeans();
		state.setMeans(kMeans.getMeans());
		state.setCovs(kMeans.getCovs());
		state.setWeights(kMeans.getWeights());
	}
}
