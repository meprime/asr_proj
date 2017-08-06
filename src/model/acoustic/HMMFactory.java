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

import util.MyProperties;
import model.Phoneme;

public class HMMFactory {

	private Map<Phoneme, HMM> baseHmms;

	private static HMMFactory instance;
	public static HMMFactory getInstance() {
		if(instance == null)
			instance = new HMMFactory();
		return instance;
	}
	
	public HMMFactory() {
		baseHmms = new HashMap<Phoneme, HMM>();
		baseHmms.put(Phoneme.A, HMMFactory.getInstance().createPhonemeHMM());
		baseHmms.put(Phoneme.N, HMMFactory.getInstance().createPhonemeHMM());
		baseHmms.put(Phoneme.S, HMMFactory.getInstance().createPhonemeHMM());
		baseHmms.put(Phoneme.SIL, HMMFactory.getInstance().createSilenceHMM());
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
				for(HmmState state : wordHmm.getStates()) {
					stateToWordMap.put(state, word);
				}
				wordHmms.add(wordHmm);
			}
			List<HmmState> states = new ArrayList<HmmState>();
			Set<Transition> transitions = new HashSet<Transition>();
			HMM silHmm = new HMM(this.getBaseHmms().get(Phoneme.getByChar(' ')));
			for(HmmState state : silHmm.getStates()) {
				stateToWordMap.put(state, " ");
			}
			double silTransitionProb = 1.0 / wordHmms.size();
			for(HMM wordHmm : wordHmms) {
				states.addAll(wordHmm.getStates());
				transitions.addAll(wordHmm.getTransitions());
				transitions.add(new Transition(silHmm.getFinalState(), wordHmm.getInitialState(), silTransitionProb));
				transitions.add(new Transition(wordHmm.getFinalState(), silHmm.getInitialState(), 1));
			}
			searchHmm = new HMM(states, transitions);
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
			for(int j = 0; j < hmm.getStates().size(); i++) {
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
}
