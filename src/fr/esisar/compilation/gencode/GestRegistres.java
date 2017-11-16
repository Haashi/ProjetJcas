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
	
	private final HashMap<Registre, Operande2> Registres;
	 
	public GestRegistres() {
		this.Registres = new HashMap<Registre, Operande2>();
		for(Registre r : Registre.values()) {
			Registres.put(r,null);
		}
	}
	public boolean estLibre(Registre registre) {
		
		return(Registres.get(registre)==null);
	}
	
	public Registre getRegistre() {
		/** retourne le premier registre libre -1 si aucun libre**/
		
		for(Registre r : Registre.values()) {
			if(Registres.get(r)==null && r!=Registre.GB && r!=Registre.LB) {
	
				return (r);
			}
			
		}
		int max=Registres.get(Registre.R0).time;
		Registre Rmax = Registre.R0;
		for(Registre r : Registre.values()) {
			if(r!=Registre.GB && r!=Registre.LB && Registres.get(r).time> max) {
				max = Registres.get(r).time;
				Rmax=r;
			}
		}
		return(Rmax);
	}
	public Operande2 getOperande(Registre registre) {
		
		return(Registres.get(registre));
	}
	public void setRegistre(Registre registre,Operande2 operande) {
		
		operande.time=0;
		for(Registre r : Registre.values()) {
			if(Registres.get(r)!=null && r!=Registre.GB && r!=Registre.LB)
				Registres.get(r).time++;
		}
		Registres.put(registre,operande );
		
	}
	public boolean freeRegistre(Registre registre) {
		if(Registres.get(registre)!=null) {
			Registres.put(registre,null);
		
			return true;
		}
		else {
			System.out.println("Registre déjà libéré");
			return false;
		}
	}
	
	public void lookRegistre(Registre registre) {
		Registres.get(registre).time=0;
	}
	
	   public void afficher() {
		     for(Map.Entry<Registre, Operande2> r : Registres.entrySet()) {
			    
			    	Registre id = r.getKey();
			        String s = "REGISTRE : " + id + " --> Operande : ";
			        Operande2 def = r.getValue();
			        if(def==null) {
			        	System.out.println(s + "null");
			        }
			        else {
			        	System.out.println(s + def.getId() + " Time : " +def.time);
			        }
			     }
	   }
}
