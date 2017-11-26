package fr.esisar.compilation.gencode;

import fr.esisar.compilation.global.src.NatureType;
import fr.esisar.compilation.global.src.Type;
import fr.esisar.compilation.global.src3.Registre;
public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		  
			  GestRegistres registres= new GestRegistres();
			  Adresse memoire = new Adresse();
			 Registre r = Registre.R0;
			 
			 for(int i =0 ;i<20;i++) {
			  if(registres.estLibre()){
				 r = registres.getRegistre();
				  
			  }
			 }
		
			 
			  registres.afficher();
			   Type a = Type.creationArray(Type.Integer,Type.String);
			   Borne  borne = new Borne(0,5);
			   Borne borne2= new Borne(0,3);
			
			/* 
			   Memoire.allouer("a", a, borne,borne2);
			   Memoire.allouer("b", Type.Integer);
			   Memoire.afficher();
			   Integer adresse = Memoire.chercher("b");
			   Integer adresse2 = Memoire.chercher("a", 3,1);
			   System.out.println(adresse2 + " a");
			   System.out.println(adresse + "b");
			   Memoire.liberer();
			   Memoire.allouer("c", Type.Integer);
			   Memoire.afficher();
			   adresse = Memoire.chercher("b");
			   System.out.println(adresse + "b");*/
			   
	}

}
