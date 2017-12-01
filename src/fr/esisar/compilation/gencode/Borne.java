package fr.esisar.compilation.gencode;

/** classe générique pour regrouper 2 valeurs en un seul objet
 * 
 */
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
