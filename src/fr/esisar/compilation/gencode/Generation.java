package fr.esisar.compilation.gencode;

import fr.esisar.compilation.global.src.*;
import fr.esisar.compilation.global.src3.*;
import java.util.ArrayList;

/**
 * Génération de code pour un programme JCas à partir d'un arbre décoré.
 */

class Generation {
   
   /**
    * Méthode principale de génération de code.
    * Génère du code pour l'arbre décoré a.
    */
	static Prog coder(Arbre a) {
	  Prog.ajouterGrosComment("Programme généré par JCasc");
      Arbre currentNode = a; 
      Arbre previousNode ;
      String chaine;
      Inst inst;
      
      //Appeler le parcours des déclarations en passant la classe qui répertorie les offsets de la pile et la racine arbre
      parcoursDecl(a);
      
      
      System.out.println(a.getNoeud());
      previousNode = currentNode;
      currentNode = currentNode.getFils2().getFils2();
      System.out.println(currentNode.getNoeud());
      
      switch(currentNode.getNoeud())
      {
      
      	case Vide: break;
    	  
      
      	case Ligne: Prog.ajouter(Inst.creation0(Operation.WNL));
      				break;
      	
      	case Ecriture:  inst = Inst.creation1(Operation.WSTR, Operande.creationOpChaine(currentNode.getFils1().getFils2().getChaine().substring(1,currentNode.getFils1().getFils2().getChaine().length()-1)));
      					System.out.println(Operande.creationOpChaine(currentNode.getFils1().getFils2().getChaine()));
      					Prog.ajouter(inst);
      					break;
      	
      				
      	default:
      		System.err.println("Unknown node");
      }
      // Fin du programme
      // L'instruction "HALT"
      inst = Inst.creation0(Operation.HALT);
      // On ajoute l'instruction à la fin du programme
      Prog.ajouter(inst);

      // On retourne le programme assembleur généré
      return Prog.instance(); 
   }
	
	
	static void parcoursDecl(Arbre a)
	{//Parcours de la liste des déclarations, pour chaque Identificateur on lui associe un offset par rapport à GB pour le placer dans la pile
		Arbre currentNode = a;
		Noeud node = currentNode.getNoeud();
		
		//currentNode = currentNode.getFils2();
		//System.out.println("Hello" + currentNode.getNoeud());
		
		if(node == Noeud.Programme)parcoursDecl(currentNode.getFils1());
		else if(node == Noeud.ListeDecl)
		{
			parcoursDecl(currentNode.getFils1());
			parcoursDecl(currentNode.getFils2());
		}
		else if(node == Noeud.Vide)return;
		else if(node == Noeud.Decl)
		{
			parcoursDecl(currentNode.getFils1());
			if(currentNode.getFils2().getNoeud() == Noeud.Tableau)
			{
				ArrayList<String> nom = searchIdent(currentNode.getFils1(), new ArrayList());
				//ArrayList<Borne> borne = searchBorne(currentNode, new ArrayList());
				
				
			}
		}
		else if(node == Noeud.ListeIdent)
		{
			parcoursDecl(currentNode.getFils1());
			parcoursDecl(currentNode.getFils2());
		}
		else if(node == Noeud.Ident)
		{
			//Call stockage de l'ident pour les variables de type boolean, integer, real
		
		}
		else if(node == Noeud.Tableau)
		{
			
			
		}
	}

	static ArrayList<String> searchIdent (Arbre a, ArrayList<String> s)
	{
		Arbre currentNode = a;
		Noeud node = currentNode.getNoeud();
		if(node == Noeud.ListeIdent)
		{
			s = searchIdent(currentNode.getFils1(), s);
			s = searchIdent(currentNode.getFils2(), s);
			return(s);
		}
		else if (node == Noeud.Vide)return(s);
		else if (node == Noeud.Ident)
		{
			s.add(currentNode.getChaine());
			return(s);
		}
		return(s);
	}
	
	static ArrayList<Borne> searchBorne (Arbre a, ArrayList<Borne> b)
	{
		Arbre currentNode = a;
		Noeud node = currentNode.getNoeud();
		if(node == Noeud.Tableau)
		{
			b.add(new Borne());
			b = searchBorne(currentNode.getFils1(), b);
			b = searchBorne(currentNode.getFils2(), b);
			return(b);
		}
		return(b);
	}
	


}



