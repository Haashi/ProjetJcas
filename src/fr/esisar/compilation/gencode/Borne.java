package fr.esisar.compilation.gencode;

public class Borne {
	private int BorneInf;
	private int BorneSup;
	
	public Borne(int BorneInf, int BorneSup) {
		this.BorneInf=BorneInf;
		this.BorneSup=BorneSup;
	}
	public int getBorneInf() {
		return this.BorneInf;
	}
	public int getBorneSup() {
		return this.BorneSup;
	}
}