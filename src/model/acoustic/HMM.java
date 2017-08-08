package model.acoustic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HMM {

	private List<HmmState> states;
	private Set<Transition> transitions;
	private boolean isSearchHmm;  // tof!
	
	public HMM(List<HmmState> states, Set<Transition> transitions, boolean isSearchHmm) {
		this.states = states;
		this.transitions = transitions;
		this.isSearchHmm = isSearchHmm;
	}
	
	public HMM(List<HmmState> states, Set<Transition> transitions) {
		this(states, transitions, false);
	}
	
	// copy constructor
	public HMM(HMM oldHmm) {
		List<HmmState> states = new ArrayList<HmmState>();
		for(HmmState state : oldHmm.getStates()) {
			HmmState newState = new HmmState(state.isFinal(), state.getGene());
			newState.setCovs(state.getCovs());
			newState.setMeans(state.getMeans());
			newState.setWeights(state.getWeights());
			states.add(newState);
		}
		this.states = states;
		this.transitions = oldHmm.getTransitions();
	}
	
	public HMM(int statesCount) {
		states = new ArrayList<HmmState>();
		for(int i = 0; i < statesCount; i++) {
			if(i == statesCount-1)
				states.add(new HmmState(true));
			else
				states.add(new HmmState(false));
		}
		transitions = new HashSet<>();
	}
	
	public void addTransition(int from, int to, double prob) {
		Transition t = new Transition(states.get(from), states.get(to), prob);
		this.transitions.add(t);
	}
	
	public Set<Transition> getTransitions() {
		return this.transitions;
	}
	
	public int getStatesCount() {
		return states.size();
	}
	
	public List<HmmState> getStates() {
		return this.states;
	}
	
//	public void setState(List<HmmState> states) {
//		this.states = states;
//	}
	
	public HmmState getInitialState() {
		return states.get(0);
	}
	public HmmState getFinalState() {
		return states.get(states.size()-1);
	}
	
	public int getNonFinalStatesCount() {
		return states.size() - 1;
	}
	
	public Transition getTransition(int from, int to) {
		HmmState fromState = states.get(from);
		HmmState toState = states.get(to);
		for(Transition t : transitions) {
			if(t.equals(fromState, toState)) {
				return t;
			}
		}
		return null;
	}
	
	public double getTransitionProbablity(int from, int to) {
		if(!isSearchHmm) {
			if(to < from || to - from > 3)
				return 0;
		} else {
			if(to != 0 && (to < from || to - from > 3))
				return 0;
		}
		Transition t = this.getTransition(from, to);
		return t == null ? 0 : t.getProbability();
	}
	
}
