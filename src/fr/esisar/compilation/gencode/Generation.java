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
      Inst inst;
      Adresse addrStack = new Adresse();
      
      parcoursDecl(currentNode.getFils1(),addrStack);//Saving space for the variable in stack, and keeping their offset to know where each variable need to be save in the stack
      Prog.ajouter(Inst.creation1(Operation.ADDSP, Operande.creationOpEntier(addrStack.getOffset())));//Instruction to move the StackPointer, depends on the number of variable
      parcoursInst(currentNode.getFils2(), Prog.instance(), addrStack);//Reading all instructions and generate code.
      
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
		if(node == Noeud.ListeInst)
		{
			parcoursInst(currentNode.getFils1(), prog, addrStack);
			parcoursInst(currentNode.getFils2(), prog, addrStack);
		}
		else if(node == Noeud.Vide || node == Noeud.Nop)return;
		else if(node == Noeud.Affect)
		{
			//First case of an affect with no operation on the right side of the := sign
			if(currentNode.getFils2().getNoeud() == Noeud.Entier || currentNode.getFils2().getNoeud() == Noeud.Reel || currentNode.getFils2().getNoeud() == Noeud.Ident || currentNode.getFils2().getNoeud() == Noeud.Index)directAffect(currentNode, prog, addrStack);
	
		}
		else if(node == Noeud.Ligne)
		{//Easy case to print new_line in terminal
			Prog.ajouter(Inst.creation0(Operation.WNL));
			return;
		}
		return;
	}
	
	static void directAffect(Arbre a, Prog prog, Adresse addrStack)
	{
		Arbre currentNode = a; 
		Registre R = Registre.R0;
		ArrayList<Inst> inst = new ArrayList<Inst>();
		ArrayList<Integer> ind;
		Integer [] indArray;
		String ident1, ident2 ;
		int offset1, offset2;
		
		//Eventually manage problem of register
		/*	if (R.estVide())
		{
			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(R)));	
		}
		Think of the end POP if necessary*/
		//Info from the left side of :=
		if(currentNode.getFils1().getNoeud() == Noeud.Index)
		{ //affecting in an array as array[5][6] := something 
			ident1 = nameArray(currentNode.getFils2());
			ind = arrayIndices(currentNode.getFils2(), new ArrayList<Integer>());
			indArray = new Integer[ind.size()];
			offset1 = addrStack.chercher(ident1, ind.toArray(indArray));	
		}
		else
		{// Affecting to a real, integer, interval or boolean as variable := something
			ident1 = currentNode.getFils1().getChaine();
			offset1 = addrStack.chercher(ident1);			
		}
		
		//Info from the left side of the := 
		if(currentNode.getFils2().getNoeud() == Noeud.Index)
		{	//Case of an array something := array[5][6]
			ind = arrayIndices(currentNode.getFils2(), new ArrayList<Integer>());
			indArray = new Integer[ind.size()];
			ident2 = nameArray(currentNode.getFils2());
			offset2 = addrStack.chercher(ident2, ind.toArray(indArray));
			inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset2, Registre.GB), Operande.opDirect(R)));
			inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset1, Registre.GB)));	
		}
		else if(currentNode.getFils2().getNoeud() == Noeud.Ident)
		{	//Case a := b 
			ident2 = currentNode.getFils2().getChaine();
			NatureType type = currentNode.getFils1().getDecor().getDefn().getType().getNature();
			if(type == NatureType.Boolean)
			{	//Right side is a boolean  -> Convention : Boolean False = 0; and True != 0
				if(ident2.equals("true"))
				{	//We need to convert true to 1 
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1), Operande.opDirect(R)));
					inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset1, Registre.GB)));
				}
				else if (ident2.equals("false"))
				{	//Same as above, with conversion from false to 0
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0), Operande.opDirect(R)));
					inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset1, Registre.GB)));
					
				}
				else 
				{	//No specific action here, because we load a variable from the stack 
					offset2 = addrStack.chercher(ident2);
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset2, Registre.GB), Operande.opDirect(R)));
					inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset1, Registre.GB)));
				}
			}
			else if (type == NatureType.Real || type == NatureType.Interval)
			{	//case where the right side is a real or a integer
				offset2 = addrStack.chercher(ident2);
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset2, Registre.GB), Operande.opDirect(R)));
				inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset1, Registre.GB)));
			}
			//Need to be the case of equality between two array, maybe for later
			/*else if (type == NatureType.Array)
			{
				
			}*/
		}
		else
		{	//Case Entier or Reel on the right side :=,  as a constant value like 13 or 17.0 
			if(currentNode.getFils2().getNoeud() == Noeud.Entier)
			{
				int alpha = currentNode.getFils2().getEntier();
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpReel(alpha), Operande.opDirect(R)));
			}
			else if(currentNode.getFils2().getNoeud() == Noeud.Reel)
			{
				float alpha = currentNode.getFils2().getReel();
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpReel(alpha), Operande.opDirect(R)));
			}
			inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset1, Registre.GB)));
		}
		ajouterInst(inst, prog);
		return;
	}
	
	static void ajouterInst(ArrayList<Inst> i, Prog prog)
	{
		for(Inst inst : i)Prog.ajouter(inst);
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

	static String nameArray(Arbre a)
	{
		if(a.getNoeud() == Noeud.Index)return(nameArray(a.getFils1()));
		else if (a.getNoeud() == Noeud.Ident)return(a.getChaine());
		return("I'm a Teapot");
	}
	
	static ArrayList<Integer> arrayIndices (Arbre a, ArrayList<Integer> ind)
	{
		if(a.getNoeud() == Noeud.Index)
		{
			ind = arrayIndices(a.getFils1(), ind);
			ind = arrayIndices(a.getFils2(), ind);
			return(ind);
		}
		else if(a.getNoeud() == Noeud.Ident)return(ind);
		else if(a.getNoeud() == Noeud.Entier)
		{
			ind.add(a.getEntier());
			return(ind);
		}
		return(ind);
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



