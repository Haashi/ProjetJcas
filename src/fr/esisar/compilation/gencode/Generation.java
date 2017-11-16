package fr.esisar.compilation.gencode;

import fr.esisar.compilation.global.src.*;
import fr.esisar.compilation.global.src3.*;

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
      
      System.out.println(a.getNoeud());
      previousNode = currentNode;
      currentNode = currentNode.getFils2().getFils2();
      System.out.println(currentNode.getNoeud());
      
      switch(currentNode.getNoeud())
      {
      	case Ligne: Prog.ajouter(Inst.creation0(Operation.WNL));
      				break;
      	
      	case Ecriture: inst = Inst.creation1(Operation.WSTR, Operande2.creationOpChaine(currentNode.getFils1().getFils2().getChaine().substring(1,currentNode.getFils1().getFils2().getChaine().length()-1)));
      					System.out.println( Operande2.creationOpChaine(currentNode.getFils1().getFils2().getChaine()));
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
}



