package fr.esisar.compilation.gencode;
import fr.esisar.compilation.global.src3.Registre;
import java.util.HashMap;
import java.util.Map;


/** Classe pour gérer les registres, 
 * les registres sont gérer par une HashMap<Registre,Boolean>
 *
 */

public class GestRegistres {
	/** Registres[i] vaut true lors que Ri est disponible, false sinon **/
	
	private final HashMap<Registre, Boolean> Registres;
	private  int NbRegistresLibres;
	
	public GestRegistres() {
		this.Registres = new HashMap<Registre, Boolean>();
		this.NbRegistresLibres=13;
		for(Registre r : Registre.values()) {
			Registres.put(r,true);
		}
	}
	
	public boolean estLibre() {
		
		return(NbRegistresLibres >2);
	}
	
	public Registre getRegistre() {
		/** retourne le premier registre libre null si aucun libre**/
	  
		for(Registre r : Registre.values()) {
			if(Registres.get(r) && r!=Registre.GB && r!=Registre.LB && r!=Registre.R15 && r!=Registre.R14) {
				NbRegistresLibres --;
				Registres.put(r,false);
				return (r);
			}
			
		}
	return(null);
	}
	
	
	//indique qu'un registre n'est plus libre
	public boolean freeRegistre(Registre registre) {
		if(registre==null){
			return false;
		}
		if(!Registres.get(registre)) {
			Registres.put(registre,true);
			NbRegistresLibres ++;
			return true;
		}
		else {
			return false;
		}
	}
	
	//indique qu'un registre est occupé
	public boolean alloueRegistre(Registre registre) {
		if(Registres.get(registre)) {
			Registres.put(registre,false);
			NbRegistresLibres ++;
			return true;
		}
		else {
			return false;
		}
	}
	
	//sert à des fin de test uniquement
	   public void afficher() {
		     for(Map.Entry<Registre, Boolean> r : Registres.entrySet()) {
			    
			    	Registre id = r.getKey();
			        String s = "REGISTRE : " + id + " --> est disponible : ";
			       Boolean def = r.getValue();
			        System.out.println(s + " " +def);
			     }
	   }
}
