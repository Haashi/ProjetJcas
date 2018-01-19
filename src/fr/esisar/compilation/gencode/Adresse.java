package fr.esisar.compilation.gencode;


import fr.esisar.compilation.global.src.NatureType;
import java.util.Hashtable;
import java.util.Enumeration;

public class Adresse {
	

	/** 
	 * Un environnement qui permet d'associer des adresses à des identifiants.
	 */

	   /**
	    * La table qui contient l'environnement
	    */
	   private final Hashtable<String, Integer> Memoire;
	   private static int offset;
	   /**
	    * Constructeur d'environnement. 
	    */
	   public Adresse() { 
	      this.Memoire = new Hashtable<String, Integer>();
	      offset=0;
	   }

	   /**
	    * Enrichissement de l'environnement avec le couple (s, Integer).
	    * <ul>
	    * <li> Si l'identifiant s est présent dans l'environnement, 
	    *      enrichir(s, Integer) ne fait rien et retourne false. </li>
	    * <li> Si l'identifiant s n'est pas présent dans l'environnement, 
	    *      enrichir(s, Integer) ajoute l'association (s, Integer) dans 
	    *      l'environnement et retourne true. </li>
	    * </ul>
	    */
	   private boolean enrichir(String s, Integer adresse) {
	      if (Memoire.containsKey(s)) {
	         return false;
	      } else {
	         Memoire.put(s, adresse);
	         return true;
	      }
	   }
	   
	   //a chaque allocation on incrémente l'offset
	   	public void allouer(String id,NatureType type,Borne... borne) {
	   		if(type!=NatureType.Array) {
	   			offset++;
	   			boolean ajouter = this.enrichir(id, offset);
	   			if(!ajouter) {
	   				System.out.println("Erreur dans l'ajout adresse");
	   			}
	   		}
	   		else {
	   			//si c'est un tableau, on ajoute la taille du tableau à l'offset
	   			offset++;
	   			this.enrichir(id, offset);
	   			offset--;
	   			int mul=1;
	   			for(int i=0;i<borne.length;i++) {
	   				mul=mul*(borne[i].getBorneSup()-borne[i].getBorneInf()+1);
	   			}
	   			offset+=mul;
	   		}
	   	}

	  
	   public Integer chercher(String s,Integer... indices) {
		   /**
		    * Cherche la defn associée à la chaîne s dans l'environnement.
		    * Si la chaîne s n'est pas dans l'environnement, chercher(s) 
		    * retourne null.
		    */
		   for (int i=0;i<indices.length;i++) {
			   s=s.concat("["+indices[i]+"]");
		   }
	      return Memoire.get(s);
	   }

	   public void liberer() {
		   Enumeration<String> keys = Memoire.keys();
		     while (keys.hasMoreElements()) {
		    	String id = keys.nextElement();
		    	if(Memoire.get(id)==offset) {
		    		Memoire.remove(id);
		    		offset--;
		    		break;
		    	}
		     }
	   }
	   //affiche toutes les correspondances identifiants -> adresses
	   public void afficher() {
		     Enumeration<String> keys = Memoire.keys();
		     while (keys.hasMoreElements()) {
		    	String id = keys.nextElement();
		        String s = "CHAINE : " + id + " --> ADRESSE : ";
		        Integer def = Memoire.get(id);
		        System.out.println(s + def);
		     }
	   }
	   //retourne le haut de la pile de déclaration
	   public int getOffset() {
		   return offset;
	   }
	 
}
