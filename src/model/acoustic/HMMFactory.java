package model.acoustic;

public class HMMFactory {

	public HMM createPhonemeHMM() {
		HMM hmm = new HMM(4);
		for(int i = 0; i < 3; i++) {
			hmm.addTransition(i, i);
			hmm.addTransition(i, i+1);
		}
		return hmm;
	}
	
	public HMM createSilenceHMM() {
		HMM hmm = new HMM(6);
		for(int i = 0; i < 5; i++) {
			hmm.addTransition(i, i);
		}
		hmm.addTransition(0, 1);
		hmm.addTransition(1, 3);
		hmm.addTransition(3, 4);
		hmm.addTransition(4, 5);
		hmm.addTransition(0, 2);
		hmm.addTransition(1, 4);
		hmm.addTransition(3, 2);
		hmm.addTransition(2, 4);
		return hmm;
	}
}
