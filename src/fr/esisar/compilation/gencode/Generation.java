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
      Adresse addrStack = new Adresse();
      
      //Appeler le parcours des déclarations en passant la classe qui répertorie les offsets de la pile et la racine arbre
      parcoursDecl(currentNode ,addrStack);
      Prog.ajouter(Inst.creation1(Operation.ADDSP, Operande.creationOpEntier(addrStack.getOffset())));
      //addrStack.afficher();
      parcoursInst(currentNode.getFils2(), Prog.instance(), addrStack);
      
      //System.out.println(a.getNoeud());
      previousNode = currentNode;
      currentNode = currentNode.getFils2().getFils2();
      //System.out.println(currentNode.getNoeud());
      
     /* switch(currentNode.getNoeud())
      {
      	case Nop:
      	case Vide: break;
    	  
      
      	case Ligne: Prog.ajouter(Inst.creation0(Operation.WNL));
      				break;
      	
      	case Ecriture:  inst = Inst.creation1(Operation.WSTR, Operande.creationOpChaine(currentNode.getFils1().getFils2().getChaine().substring(1,currentNode.getFils1().getFils2().getChaine().length()-1)));
      					System.out.println(Operande.creationOpChaine(currentNode.getFils1().getFils2().getChaine()));
      					Prog.ajouter(inst);
      					break;
      	
      				
      	default:
      		System.err.println("Unknown node");
      }*/
      
      // Fin du programme
      // L'instruction "HALT"
      inst = Inst.creation0(Operation.HALT);
      // On ajoute l'instruction à la fin du programme
      Prog.ajouter(inst);

      // On retourne le programme assembleur généré
      return Prog.instance(); 
   }
	
	
	static void parcoursInst(Arbre a, Prog prog, Adresse addrStack)
	{
		Arbre currentNode = a;
		Noeud node = currentNode.getNoeud();
		System.out.println("----CouCou ---- ");
		
		if(node == Noeud.ListeInst)
		{
			parcoursInst(currentNode.getFils1(), prog, addrStack);
			parcoursInst(currentNode.getFils2(), prog, addrStack);
		}
		else if(node == Noeud.Vide)return;
		else if(node == Noeud.Affect)
		{
			if(currentNode.getFils2().getNoeud() == Noeud.Entier || currentNode.getFils2().getNoeud() == Noeud.Reel || currentNode.getFils2().getNoeud() == Noeud.Ident)
			{
				directAffect(currentNode, prog, addrStack);
			}
			//Get the ident on the left side of the := sign from the stack, first Son 
			
			//And call, for the right side of the := sign second Son
			
		}
		
	}
	
	static void directAffect(Arbre a, Prog prog, Adresse addrStack)
	{//suppose that first son of a is a ident node
		Arbre currentNode = a; 
		Noeud node = currentNode.getNoeud();
		Registre R = Registre.R0;
		ArrayList<Inst> inst = new ArrayList<Inst>();
		
		
	/*	if (R.estVide())
		{
			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(R)));	
		}
		Penser au pop */
		//Case index !!!!
		if(currentNode.getFils2().getNoeud() == Noeud.Ident)
		{
			
		}
		else
		{ //Case Entier or Reel on the right side := 
			if(currentNode.getFils2().getNoeud() == Noeud.Entier)
			{
				int alpha = currentNode.getFils2().getEntier();
				String ident = currentNode.getFils1().getChaine();
				int addrOffset = addrStack.chercher(ident);
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(alpha), Operande.opDirect(R)));
				inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(addrOffset, Registre.GB)));
			}
			else if(currentNode.getFils2().getNoeud() == Noeud.Reel)
			{
				float beta = currentNode.getFils2().getReel();
				String ident = currentNode.getFils1().getChaine();
				int addrOffset = addrStack.chercher(ident);
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpReel(beta), Operande.opDirect(R)));
				inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(addrOffset, Registre.GB)));
			}
		}
		
		ajouterInst(inst, prog);
		return;
	}
	
	static void ajouterInst(ArrayList<Inst> i, Prog prog)
	{
		for(Inst inst : i)
		{
			Prog.ajouter(inst);
		}
	}
	
	
	
	
	
	static void parcoursDecl(Arbre a, Adresse addr)
	{//Parcours de la liste des déclarations, pour chaque Identificateur on lui associe un offset par rapport à GB pour le placer dans la pile
		Arbre currentNode = a;
		Noeud node = currentNode.getNoeud();
		
		//currentNode = currentNode.getFils2();
		//System.out.println("Hello" + currentNode.getNoeud());
		
		if(node == Noeud.Programme)parcoursDecl(currentNode.getFils1(), addr);
		else if(node == Noeud.ListeDecl)
		{
			parcoursDecl(currentNode.getFils1(), addr);
			parcoursDecl(currentNode.getFils2(), addr);
		}
		else if(node == Noeud.Vide)return;
		else if(node == Noeud.Decl)
		{		
			if(currentNode.getFils2().getNoeud() == Noeud.Tableau)
			{
				ArrayList<String> nom = searchIdent(currentNode.getFils1(), new ArrayList<String>());
				ArrayList<Borne> borne = searchBorne(currentNode.getFils2(), new ArrayList<Borne>());
				Borne arrayBorne [] = new Borne[borne.size()];
				for(String name : nom)
				{
					addr.allouerTableau(name,borne.toArray(arrayBorne));
				}
				
			}
			else parcoursDecl(currentNode.getFils1(), addr);
		}
		else if(node == Noeud.ListeIdent)
		{
			parcoursDecl(currentNode.getFils1(), addr);
			parcoursDecl(currentNode.getFils2(), addr);
		}
		else if(node == Noeud.Ident)
		{
			addr.allouer(currentNode.getChaine(), currentNode.getDecor().getDefn().getType());
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
			b = searchBorne(currentNode.getFils1(), b);
			b = searchBorne(currentNode.getFils2(), b);
			return(b);
		}
		else if (node == Noeud.Intervalle)
		{
			b.add(new Borne(currentNode.getFils1().getEntier(), currentNode.getFils2().getEntier()));
			return(b);
		}
		return(b);
	}
}



