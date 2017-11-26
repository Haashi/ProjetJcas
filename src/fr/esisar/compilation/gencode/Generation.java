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
      Inst inst;
      Adresse addrStack = new Adresse();
      
      //Saving space for the variable in stack, and keeping their offset to know where each variable need to be save in the stack
      parcoursDecl(a.getFils1(),addrStack);
      //Instruction to move the StackPointer, depends on the number of variable
      Prog.ajouter(Inst.creation1(Operation.ADDSP, Operande.creationOpEntier(addrStack.getOffset())));
      //Reading all instructions and generate code.
      parcoursInst(a.getFils2(), Prog.instance(), addrStack);
      
      // Fin du programme
      // L'instruction "HALT"
      inst = Inst.creation0(Operation.HALT);
      // On ajoute l'instruction à la fin du programme
      Prog.ajouter(inst);
      
      //Débordement 
      Prog.ajouter(Prog.L_Etiq_Debordement_Intervalle);
      Prog.ajouter(Inst.creation1(Operation.WSTR, Operande.creationOpChaine("Task Successfully Failed")));
      Prog.ajouter(inst);
      
      Prog.ajouter(Prog.L_Etiq_Debordement_Indice);
      Prog.ajouter(Inst.creation1(Operation.WSTR, Operande.creationOpChaine("Segmentation Fault")));
      Prog.ajouter(inst);
      
      Prog.ajouter(Prog.L_Etiq_Debordement_Arith);
      Prog.ajouter(Inst.creation1(Operation.WSTR, Operande.creationOpChaine("BufferOverflow")));
      Prog.ajouter(inst);
      
      Prog.ajouter(Prog.L_Etiq_Pile_Pleine);
      Prog.ajouter(Inst.creation1(Operation.WSTR, Operande.creationOpChaine("StackOverflow")));
      Prog.ajouter(inst);
      
      // On retourne le programme assembleur généré
      return Prog.instance(); 
   }
	
	
	static void parcoursInst(Arbre a, Prog prog, Adresse addrStack)
	{		
		switch(a.getNoeud())
		{
		case ListeInst : 
			parcoursInst(a.getFils1(), prog, addrStack);
			parcoursInst(a.getFils2(), prog, addrStack);
			break;
		case Affect : 
			if(a.getFils2().getNoeud() == Noeud.Entier || a.getFils2().getNoeud() == Noeud.Reel || a.getFils2().getNoeud() == Noeud.Ident || a.getFils2().getNoeud() == Noeud.Index)
			{
				directAffect(a, prog, addrStack);
			}
			else
			{
				operationAffect(a, prog, addrStack);
			}
			break;
		case Ecriture :
			parcoursEcriture(a.getFils1(), prog, addrStack);
			break;
		case Ligne : 
			Prog.ajouter(Inst.creation0(Operation.WNL));
			break;
		case Vide :
		case Nop:
		default:
			break;
		}
		return;
	}
	static void operationAffect(Arbre a, Prog prog, Adresse addrStack)
	{
		
	}
	
	static void parcoursEcriture(Arbre a, Prog prog, Adresse addrStack )
	{
		ArrayList<Inst> inst = new ArrayList<Inst>();
		String str ;
		int offset;
		switch(a.getNoeud())
		{
		case ListeExp :
			parcoursEcriture(a.getFils1(), prog, addrStack);
			parcoursEcriture(a.getFils2(), prog, addrStack);
			break;
		case Chaine :
			//Here the string we give the string to the instruction
			str = a.getChaine();
			str = str.substring(1, str.length()-1);
			inst.add(Inst.creation1(Operation.WSTR, Operande.creationOpChaine(str)));
			break;
		case Ident :
			//We need to put the integer or real to print in R1 register
			str = a.getChaine();
			offset = addrStack.chercher(str);
			inst.add(Inst.creation2(Operation.LOAD,Operande.creationOpIndirect(offset, Registre.GB),Operande.opDirect(Registre.R1)));
			if(a.getDecor().getType() == Type.Integer || a.getDecor().getDefn().getType().getNature() == NatureType.Interval)
			{
				inst.add(Inst.creation0(Operation.WINT));
			}
			else if (a.getDecor().getDefn().getType().getNature() == NatureType.Real)
			{
				inst.add(Inst.creation0(Operation.WFLOAT));
			}
			break;
		case Index :
				Integer [] indArray;
				ArrayList<Integer> ind;
				ind = arrayIndices(a,new ArrayList<Integer>());
				indArray = new Integer[ind.size()];
				str = nameArray(a);
				offset = addrStack.chercher(str, ind.toArray(indArray));
				inst.add(Inst.creation2(Operation.LOAD,Operande.creationOpIndirect(offset, Registre.GB),Operande.opDirect(Registre.R1)));
				if(a.getDecor().getType() == Type.Integer || a.getDecor().getType().getNature() == NatureType.Interval)
				{
					inst.add(Inst.creation0(Operation.WINT));
				}
				else if (a.getDecor().getType().getNature() == NatureType.Real)
				{
					inst.add(Inst.creation0(Operation.WFLOAT));
				}
			break;
		default :
			break;
		}
		ajouterInst(inst, prog);
		return;
	}
	
	static void directAffect(Arbre a, Prog prog, Adresse addrStack)
	{
		Registre R = Registre.R0;
		ArrayList<Inst> inst = new ArrayList<Inst>();
		ArrayList<Integer> ind;
		Integer [] indArray;
		String ident1, ident2 ;
		int offset1, offset2;
		boolean isInterval = false;
		
		//Info from the left side of :=
		if(a.getFils1().getNoeud() == Noeud.Index)
		{ //affecting in an array as array[5][6] := something 
			ident1 = nameArray(a.getFils1());
			ind = arrayIndices(a.getFils1(), new ArrayList<Integer>());
			indArray = new Integer[ind.size()];
			offset1 = addrStack.chercher(ident1, ind.toArray(indArray));	
		}
		else
		{// Affecting to a real, integer, interval or boolean as variable := something
			if(a.getDecor().getType() != Type.Integer && a.getDecor().getType().getNature() == NatureType.Interval)isInterval = true;
			ident1 = a.getFils1().getChaine();
			offset1 = addrStack.chercher(ident1);			
		}
		
		//Info from the left side of the := 
		if(a.getFils2().getNoeud() == Noeud.Index)
		{	//Case of an array something := array[5][6]
			ind = arrayIndices(a.getFils2(), new ArrayList<Integer>());
			indArray = new Integer[ind.size()];
			ident2 = nameArray(a.getFils2());
			offset2 = addrStack.chercher(ident2, ind.toArray(indArray));
			inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset2, Registre.GB), Operande.opDirect(R)));
		}
		else if(a.getFils2().getNoeud() == Noeud.Ident)
		{	//Case a := b 
			ident2 = a.getFils2().getChaine();
			NatureType type;
			if(a.getFils1().getDecor().getType() == Type.Integer)
			{
				type = NatureType.Interval;
			}
			else
			{
				type = a.getFils1().getDecor().getDefn().getType().getNature();
			}
			
			if(type == NatureType.Boolean)
			{	//Right side is a boolean  -> Convention : Boolean False = 0; and True != 0
				if(ident2.equals("true"))
				{	//We need to convert true to 1 
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1), Operande.opDirect(R)));
				}
				else if (ident2.equals("false"))
				{	//Same as above, with conversion from false to 0
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0), Operande.opDirect(R)));					
				}
				else 
				{	//No specific action here, because we load a variable from the stack 
					offset2 = addrStack.chercher(ident2);
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset2, Registre.GB), Operande.opDirect(R)));
				}
			}
			else if (type == NatureType.Real || type == NatureType.Interval)
			{	//case where the right side is a real or a integer
				offset2 = addrStack.chercher(ident2);
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset2, Registre.GB), Operande.opDirect(R)));
			}
			
			//Need to be the case of equality between two array, maybe for later
			/*else if (type == NatureType.Array)
			{
				
			}*/
		}
		else
		{	//Case Entier or Reel on the right side :=,  as a constant value like 13 or 17.0 
			if(a.getFils2().getNoeud() == Noeud.Entier)
			{
				int alpha = a.getFils2().getEntier();
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(alpha), Operande.opDirect(R)));
			}
			else if(a.getFils2().getNoeud() == Noeud.Reel)
			{
				float alpha = a.getFils2().getReel();
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpReel(alpha), Operande.opDirect(R)));
			}
		}
		
		if(isInterval)
		{
			int sup, inf;
			inf = a.getDecor().getType().getBorneInf() ;
			sup = a.getDecor().getType().getBorneSup() ;
			codeDebordInterval(inst,inf,sup,R);
		}
		
		inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset1, Registre.GB)));	
		ajouterInst(inst, prog);
		return;
	}
	
	static void codeDebordInterval (ArrayList<Inst> inst, int inf, int sup, Registre R)
	{
		Registre R2 = Registre.R2;
		
		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(inf), Operande.opDirect(R2)));
		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(R2), Operande.opDirect(R)));
		inst.add(Inst.creation1(Operation.BLT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Intervalle)));

		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(sup), Operande.opDirect(R2)));
		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(R2), Operande.opDirect(R)));
		inst.add(Inst.creation1(Operation.BGT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Intervalle)));
	}
	
	static void ajouterInst(ArrayList<Inst> i, Prog prog)
	{
		for(Inst inst : i)Prog.ajouter(inst);
	}

	static void parcoursDecl(Arbre a, Adresse addr)
	{//Parcours de la liste des déclarations, pour chaque Identificateur on lui associe un offset par rapport à GB pour le placer dans la pile
		switch(a.getNoeud())
		{
		case ListeDecl : 
			parcoursDecl(a.getFils1(), addr);
			parcoursDecl(a.getFils2(), addr);
			break;
		case Decl :
			if(a.getFils2().getNoeud() == Noeud.Tableau)
			{ //Cas de déclaration d'un tableau on recupère bien toutes les dimensions et nom de tableau
				ArrayList<String> nom = searchIdent(a.getFils1(), new ArrayList<String>());
				ArrayList<Borne> borne = searchBorne(a.getFils2(), new ArrayList<Borne>());
				Borne arrayBorne [] = new Borne[borne.size()];
				for(String name : nom)
				{
					addr.allouerTableau(name,borne.toArray(arrayBorne));
				}
				
			}
			else parcoursDecl(a.getFils1(), addr);
			break;
		case ListeIdent : 
			parcoursDecl(a.getFils1(), addr);
			parcoursDecl(a.getFils2(), addr);
			break;
		case Ident :
			//Cas de déclaration d'autre chose qu'un tableau
			addr.allouer(a.getChaine(), a.getDecor().getDefn().getType());
			break;
		default:
			break;
		}
		return;
	}

	static String nameArray(Arbre a)
	{
		if(a.getNoeud() == Noeud.Index)return(nameArray(a.getFils1()));
		else if (a.getNoeud() == Noeud.Ident)return(a.getChaine());
		return("I'm a Teapot");
	}
	
	static ArrayList<Integer> arrayIndices (Arbre a, ArrayList<Integer> ind)
	{	
		switch(a.getNoeud())
		{
		case Index :;
			ind = arrayIndices(a.getFils1(), ind);
			ind = arrayIndices(a.getFils2(), ind);
			return(ind);
		case Entier :
			ind.add(a.getEntier());
			return(ind);
		default : 
			return(ind);
		}
	}
	
	static ArrayList<String> searchIdent (Arbre a, ArrayList<String> s)
	{
		switch(a.getNoeud())
		{
		case ListeIdent : 
			s = searchIdent(a.getFils1(), s);
			s = searchIdent(a.getFils2(), s);
			return(s);
		case Ident :
			s.add(a.getChaine());
			return(s);
		default:
			return(s);
		}
	}
	
	static ArrayList<Borne> searchBorne (Arbre a, ArrayList<Borne> b)
	{
		switch(a.getNoeud())
		{
		case Tableau : 
			b = searchBorne(a.getFils1(), b);
			b = searchBorne(a.getFils2(), b);
			return(b);
		case Intervalle :
			b.add(new Borne(a.getFils1().getEntier(), a.getFils2().getEntier()));
			return(b);
		default :
			return(b);
		}
	}
	
}



