package model;

public enum Phoneme {

	A,
	N,
	S,
	SIL;

	public static Phoneme getByChar(char c) {
		switch(c) {
		case 'A':
			return Phoneme.A;
		case 'N':
			return Phoneme.N;
		case 'S':
			return Phoneme.S;
		default:
			return Phoneme.SIL;
		}
	}
}
