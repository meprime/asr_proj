package model.acoustic;

public class HMM {

	private int statesCount;
	private HmmState[] states;
	private double[][] a;	// transition probabilities
	private double[] b;		// observation probabilities
	
	public HMM(int statesCount) {
		this.statesCount = statesCount;
		states = new HmmState[statesCount];
		for(int i = 0; i < statesCount; i++) {
			if(i == statesCount-1)
				states[i] = new HmmState(true);
			else
				states[i] = new HmmState(false);
		}
	}
	
	// TODO Implement this method.
	public void addTransition(int from, int to) {
		
	}
	
	public HmmState getInitialState() {
		return states[0];
	}
	public HmmState getFinalState() {
		return states[statesCount-1];
	}
	
	class HmmState {
		private static final int GMMs_COUNT = 2;
		private boolean isFinal;
		
		private double[] weights;
		private double[][] means;
		private double[][] covs;
		
		public HmmState(boolean isFinal) {
			this.isFinal = isFinal;
		}
	}
}
