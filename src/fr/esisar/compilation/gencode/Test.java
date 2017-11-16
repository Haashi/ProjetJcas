package fr.esisar.compilation.gencode;

import fr.esisar.compilation.global.src.NatureType;
import fr.esisar.compilation.global.src.Type;
import fr.esisar.compilation.global.src3.Registre;
public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		  
			  GestRegistres registres= new GestRegistres();
			  Adresse memoire = new Adresse();
			 
			  for(int i =0 ; i<=16;i++) {
				  memoire.allouer("ntm"+i,Type.Integer);
				  Registre r = registres.getRegistre();
				  
				  Operande2 Op = new Operande2("ntm" + i,memoire.chercher("ntm" + i));
				  registres.setRegistre(r, Op);
			  }
			  registres.freeRegistre(Registre.R0);
			  System.out.println(""+registres.getOperande(Registre.R1).getId());
			  System.out.println(""+registres.estLibre(Registre.R0));
			  System.out.println(""+registres.estLibre(Registre.R1));
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
