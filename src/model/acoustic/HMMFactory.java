package model.acoustic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import main.MyProperties;
import model.Phoneme;

public class HMMFactory {

	private Map<Phoneme, HMM> baseHmms;

	private static HMMFactory instance;
	public static HMMFactory getInstance() {
		if(instance == null)
			instance = new HMMFactory();
		return instance;
	}
	
	private HMMFactory() {
		baseHmms = new HashMap<Phoneme, HMM>();
		baseHmms.put(Phoneme.A, this.createPhonemeHMM());
		baseHmms.put(Phoneme.N, this.createPhonemeHMM());
		baseHmms.put(Phoneme.S, this.createPhonemeHMM());
		baseHmms.put(Phoneme.SIL, this.createSilenceHMM());
	}
	
	public Map<Phoneme, HMM> getBaseHmms() {
		return baseHmms;
	}
	
	public HMM createPhonemeHMM() {
		HMM hmm = new HMM(4);
		for(int i = 0; i < 3; i++) {
			hmm.addTransition(i, i, 0.75);
			hmm.addTransition(i, i+1, 0.25);
		}
		return hmm;
	}
	
	public HMM createSilenceHMM() {
		HMM hmm = new HMM(6);
		for(int i = 0; i < 5; i++) {
			hmm.addTransition(i, i, (i==2 || i==4) ? 0.75 : 0.4);
		}
		hmm.addTransition(0, 1, 0.3);
		hmm.addTransition(1, 3, 0.3);
		hmm.addTransition(3, 4, 0.3);
		hmm.addTransition(4, 5, 0.25);
		hmm.addTransition(0, 2, 0.3);
		hmm.addTransition(1, 4, 0.3);
		hmm.addTransition(3, 2, 0.3);
		hmm.addTransition(2, 4, 0.25);
		return hmm;
	}
	
	private HMM searchHmm;
	private Map<HmmState, String> stateToWordMap;
	public HMM getSearchHMM() {
		if(searchHmm != null)
			return searchHmm;
		stateToWordMap = new HashMap<HmmState, String>();
		try(Scanner lexScanner = new Scanner(new File(MyProperties.getInstance().getProperty(MyProperties.PROP_LEXICON_PATH)))) {
			List<HMM> wordHmms = new ArrayList<HMM>();
			while(lexScanner.hasNextLine()) {
				String line = lexScanner.nextLine();
				String word = line.split(" ")[0];
				List<HMM> wordPhonemeHmms = new ArrayList<HMM>();
				for(int i = 0; i < word.length(); i++) {
					HMM phonemeHmm = this.getBaseHmms().get(Phoneme.getByChar(word.charAt(i)));
					HMM newPhonemeHmm = new HMM(phonemeHmm);
					wordPhonemeHmms.add(newPhonemeHmm);
				}
				HMM wordHmm = this.concatenate(wordPhonemeHmms);
				wordHmm.getStates().remove(wordHmm.getStates().size()-1); // removing the final state.
				for(HmmState state : wordHmm.getStates()) {
					stateToWordMap.put(state, word);
				}
				wordHmms.add(wordHmm);
			}
			List<HmmState> states = new ArrayList<HmmState>();
			Set<Transition> transitions = new HashSet<Transition>();
			HMM silHmm = new HMM(this.getBaseHmms().get(Phoneme.getByChar(' ')));
			states.addAll(silHmm.getStates());
			transitions.addAll(silHmm.getTransitions());
			for(HmmState state : silHmm.getStates()) {
				stateToWordMap.put(state, " ");
			}
			double silTransitionProb = 1.0 / wordHmms.size();
			for(HMM wordHmm : wordHmms) {
				System.out.println("wordHmm states: " + wordHmm.getStatesCount() + ", wordHmm transitions: " + wordHmm.getTransitions().size());
				states.addAll(wordHmm.getStates());
				transitions.addAll(wordHmm.getTransitions());
				transitions.add(new Transition(silHmm.getStates().get(silHmm.getStatesCount()-2), wordHmm.getInitialState(), silTransitionProb));
				transitions.add(new Transition(wordHmm.getStates().get(wordHmm.getStatesCount()-1), silHmm.getInitialState(), 1));
			}
			searchHmm = new HMM(states, transitions, true);
			if(MyProperties.getInstance().isDebug()) {
				for(HMM wordHmm : wordHmms) {
					System.out.println("wordHmm:");
					this.printHmm(wordHmm);
				}
				System.out.println("Search HMM:");
				this.printHmm(searchHmm);
			}
			return searchHmm;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<HmmState, String> getStateToWordMap() {
		return this.stateToWordMap;
	}
	
	private Set<Transition> allTransitions;
	public HMM concatenate(List<HMM> hmms) {
		List<HmmState> states = new ArrayList<HmmState>();
		if(allTransitions == null)
			allTransitions = new HashSet<Transition>();
		Set<Transition> transitions = new HashSet<Transition>();
		HMM result = new HMM(states, transitions);
		for(int i = 0; i < hmms.size(); i++) {
			HMM hmm = hmms.get(i);
			for(int j = 0; j < hmm.getStates().size(); j++) {
				HmmState state = hmm.getStates().get(j);
				if(j < hmm.getStates().size()-1 || i == hmms.size()-1)
					states.add(state);
			}
			for(Transition t : hmm.getTransitions()) {
				if(!t.getTo().equals(hmm.getStates().get(hmm.getStates().size()-1)) || i == hmms.size()-1)
					transitions.add(this.addTransitionIfNotExists(allTransitions, t));
			}
			if(i < hmms.size() - 1) {
				HMM nextHmm = hmms.get(i+1);
				if(hmm.equals(this.getBaseHmms().get(Phoneme.SIL))) {
					transitions.add(this.addTransitionIfNotExists(allTransitions, hmm.getStates().get(hmm.getStatesCount()-2), nextHmm.getInitialState(), 1));
				} else {
					if(nextHmm.equals(this.getBaseHmms().get(Phoneme.SIL))) {
						if(i < hmms.size() - 2) {
							transitions.add(this.addTransitionIfNotExists(allTransitions, hmm.getStates().get(hmm.getStatesCount()-2), nextHmm.getInitialState(), 0.5));
							transitions.add(this.addTransitionIfNotExists(allTransitions, hmm.getStates().get(hmm.getStatesCount()-2), hmms.get(i+2).getInitialState(), 0.5));
						} else {
							transitions.add(this.addTransitionIfNotExists(allTransitions, hmm.getStates().get(hmm.getStatesCount()-2), nextHmm.getInitialState(), 1));
						}
					} else {
						transitions.add(this.addTransitionIfNotExists(allTransitions, hmm.getStates().get(hmm.getStatesCount()-2), nextHmm.getInitialState(), 1));
					}
				}
			}
		}
		return result;
	}
	
	private Transition addTransitionIfNotExists(Set<Transition> transitions, HmmState from, HmmState to, double prob) {
		for(Transition t : transitions) {
			if(t.getFrom().equals(from) && t.getTo().equals(to))
				return t;
		}
		Transition t = new Transition(from, to, prob);
		transitions.add(t);
		return t;
	}

	private Transition addTransitionIfNotExists(Set<Transition> transitions, Transition tr) {
		for(Transition t : transitions) {
			if(t.getFrom().equals(tr.getFrom()) && t.getTo().equals(tr.getTo()))
				return tr;
		}
		transitions.add(tr);
		return tr;
	}
	
	public void writeTransitions() {
		for(Transition t : this.allTransitions)
			System.out.println(t.getProbability());
	}
	
	public void printHmm(HMM hmm) {
		List<HmmState> sortedStates = new ArrayList<>();
		List<HmmState> initialStates = new ArrayList<>(hmm.getStates());
		Map<HmmState, Integer> stateInds = new HashMap<>();
		for(int i = 0; i < hmm.getStatesCount(); i++)
			stateInds.put(hmm.getStates().get(i), i);
		for(int i = 0; i < hmm.getStatesCount(); i++) {
			for(int j = 0; j < initialStates.size(); j++) {
				boolean isFirst = true;
				for(int k = 0; k < initialStates.size(); k++) {
					if(j != 0 && k != j && hmm.getTransitionProbablity(stateInds.get(initialStates.get(k)), stateInds.get(initialStates.get(j))) > 0) {
						isFirst = false;
						break;
					}
				}
				if(isFirst) {
					sortedStates.add(initialStates.get(j));
					initialStates.remove(j);
					break;
				}
			}
		}
		for(int i = 0; i < hmm.getStatesCount(); i++) {
			int from = stateInds.get(sortedStates.get(i));
			for(int j = 0; j < hmm.getStatesCount(); j++) {
				int to = stateInds.get(sortedStates.get(j));
				if(hmm.getTransitionProbablity(from, to) > 0) {
					System.out.println(from + " -> " + to + ": " + hmm.getTransitionProbablity(from, to));
				}
			}
		}
	}
}
