package fr.esisar.compilation.gencode;
import fr.esisar.compilation.global.src3.Registre;
import fr.esisar.compilation.global.src3.Operande;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/** estLibre(), getRegistre(), getOperande(),setRegistre(),freeRegistre(),lookRegistre(),afficher() 
 * estLibre() : return true si le registre est libre
 * getOperande() : retourne l'opérande contenue dans le registre demandé
 * setRegistre() : remplit un registre avec une operande
 * freeRegistre(): libere un registre
 * lookRegistre() : remet le time d'un registre à 0
 * afficher() : affiche le contenu de tous les registres
 * @author root
 *
 */

public class GestRegistres {
	/** Registres[i] vaut true lors que Ri est disponible, false sinon **/
	
	private final HashMap<Registre, Boolean> Registres;
	private  int NbRegistresLibres;
	
	public GestRegistres() {
		this.Registres = new HashMap<Registre, Boolean>();
		this.NbRegistresLibres=15;
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
	System.out.println("plus de registres");	
	return(null);
	}
	
	
	
	public boolean freeRegistre(Registre registre) {
		if(!Registres.get(registre)) {
			Registres.put(registre,true);
			NbRegistresLibres ++;
			return true;
		}
		else {
			System.out.println("Registre déjà libéré");
			return false;
		}
	}
	
	public boolean alloueRegistre(Registre registre) {
		if(Registres.get(registre)) {
			Registres.put(registre,false);
			NbRegistresLibres ++;
			return true;
		}
		else {
			System.out.println("Registre déjà occupé");
			return false;
		}
	}
	
	   public void afficher() {
		     for(Map.Entry<Registre, Boolean> r : Registres.entrySet()) {
			    
			    	Registre id = r.getKey();
			        String s = "REGISTRE : " + id + " --> est disponible : ";
			       Boolean def = r.getValue();
			        System.out.println(s + " " +def);
			     }
	   }
}
