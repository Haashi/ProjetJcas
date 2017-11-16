package fr.esisar.compilation.gencode;

import fr.esisar.compilation.global.src.Defn;
import fr.esisar.compilation.global.src.NatureType;
import fr.esisar.compilation.global.src.Type;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
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
	      this.offset=0;
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

	   	public void allouer(String id,Type type,Borne... borne) {
	   		if(type.getNature()!=NatureType.Array) {
	   			offset++;
	   			boolean ajouter = this.enrichir(id, offset);
	   			if(!ajouter) {
	   				System.out.println("Erreur dans l'ajout adresse");
	   			}
	   		}
	   		else {
	   			allouerTableau(id,borne);
	   		}
	   	}
	   	
	   	public void allouerTableau(String id,Borne...borne) {
	   		if(borne.length==1) {
	   			for(int j= borne[0].getBorneInf();j<=borne[0].getBorneSup();j++) {
   					offset++;
   					boolean ajouter = this.enrichir(id.concat("["+j+"]"), offset);
   					if(!ajouter) {
   						System.out.println("Erreur lors de l'ajout adresse tableau");
   					}
   				}
	   		}
	   		else {
	   			Borne newBorne[]= new Borne[borne.length-1];
	   			int i=0;
	   			for(int j=1;j<borne.length;j++) {
	   				newBorne[i]=borne[j];
	   				i++;
	   			}
	   			for(i=borne[0].getBorneInf();i<=borne[0].getBorneSup();i++) {
	   				allouerTableau(id.concat("["+i+"]"),newBorne);
	   			}
	   		}
	   	}
	   /**
	    * Cherche la defn associée à la chaîne s dans l'environnement.
	    * Si la chaîne s n'est pas dans l'environnement, chercher(s) 
	    * retourne null.
	    */
	   public Integer chercher(String s,Integer... indices) {
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
	   public void afficher() {
		     Enumeration<String> keys = Memoire.keys();
		     while (keys.hasMoreElements()) {
		    	String id = keys.nextElement();
		        String s = "CHAINE : " + id + " --> ADRESSE : ";
		        Integer def = Memoire.get(id);
		        System.out.println(s + def);
		     }
	   }
	   
	   public int getOffset() {
		   return this.offset;
	   }
	   
	 
}
