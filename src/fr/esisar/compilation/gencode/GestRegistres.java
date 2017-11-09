package fr.esisar.compilation.gencode;

public class GestRegistres {
	/** Registres[i] vaut true lors que Ri est disponible, false sinon **/
	private Boolean[] Registres = new Boolean[15];
	
	public GestRegistres() {
		for(int i =0;i<15;i++) {
			Registres[i]=true;
		}
	}
	public boolean estLibre(int registre) {
		return(Registres[registre]);
	}
	public int getRegistre() {
		/** retourne le premier indice associé à un registre libre -1 si aucun libre**/
		Boolean trouve = false;
		int i=0;
		while(!trouve && i<15) {
			if(Registres[i]) {
				trouve=true;
			}
			else 
				i++;
		}
		if(trouve) {
			Registres[i]=false;
			return i;
		}
		else
			return -1;
		
	}
	public boolean giveRegistre(int registre) {
		if(Registres[registre]==false) {
			Registres[registre]=true;
			System.out.println(Registres[registre]);
			return true;
		}
		else {
			System.out.println("Registre déjà libéré");
			return false;
		}
	}
}
