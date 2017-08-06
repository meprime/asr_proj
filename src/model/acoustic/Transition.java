package model.acoustic;

public class Transition {
	private HmmState from;
	private HmmState to;
	private double probability;
	
	public Transition(HmmState from, HmmState to, double prob) {
		this.from = from;
		this.to = to;
		this.probability = prob;
	}
	
	public void setProbability(double p) {
		this.probability = p;
	}
	
	public HmmState getFrom() {
		return this.from;
	}
	
	public HmmState getTo() {
		return this.to;
	}
	
	public double getProbability() {
		return this.probability;
	}

	public boolean equals(HmmState from, HmmState to) {
		return this.from.getGene().equals(from.getGene()) && this.to.getGene().equals(to.getGene());
	}
}