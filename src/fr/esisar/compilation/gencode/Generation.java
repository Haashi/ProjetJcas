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
      GestRegistres gestRegistre = new GestRegistres();
      
      //Saving space for the variable in stack, and keeping their offset to know where each variable need to be save in the stack
      parcoursDecl(a.getFils1(),addrStack);
      //Instruction to move the StackPointer, depends on the number of variable
      Prog.ajouter(Inst.creation1(Operation.ADDSP, Operande.creationOpEntier(addrStack.getOffset())));
      //Reading all instructions and generate code.
      parcoursListeInst(a.getFils2(), Prog.instance(), addrStack,gestRegistre);
      
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
      
      Prog.ajouter(Prog.L_Etiq_Div_0);
      Prog.ajouter(Inst.creation1(Operation.WSTR, Operande.creationOpChaine("Division par 0")));
      Prog.ajouter(inst);
      
      
      // On retourne le programme assembleur généré
      return Prog.instance(); 
   }
	
	static void parcoursListeInst(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre) {
		switch(a.getNoeud()) {	
		case ListeInst:
	   		parcoursListeInst(a.getFils1(),prog,addrStack,gestRegistre);
	   		parcoursInst(a.getFils2(),prog,addrStack,gestRegistre);
	   		break;
	   	case Vide:
	   		break;
	   	default:
	   		break;
	   }
	}
	
	static int parcoursTaille(Type type) {
		switch(type.getNature()) {
		case Array:
			int a=parcoursTaille(type.getElement());
			return (a*(type.getIndice().getBorneSup()-type.getIndice().getBorneInf()+1));
		case Boolean:
		case Interval:
		case Real:
		case String:
			return 1;
		default:
			break;
		
		}
		return 0;
	}
	
	static void parcoursInst(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre) {
		Registre t2;
		Registre t1;
		Registre t3;
		switch(a.getNoeud()) {
		   	case Vide:
		   		break;
		   	case Nop:
		   		break;
		   	case Affect:
		   		switch(a.getDecor().getType().getNature()) {
				case Array:
					Registre t4 = gestRegistre.getRegistre();
					Etiq e1 = Etiq.nouvelle("etiq"), e2 = Etiq.nouvelle("etiq");
					t3 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,true);
					t2 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,true);
					int taille = parcoursTaille(a.getDecor().getType());
					t1=gestRegistre.getRegistre();
					
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0), Operande.opDirect(t4)));
					Prog.ajouter(e1);
					Prog.ajouter(Inst.creation2(Operation.CMP,Operande.creationOpEntier(taille),Operande.opDirect(t4)));
					Prog.ajouter(Inst.creation1(Operation.BGT, Operande.creationOpEtiq(e2)));
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpIndexe(0, Registre.GB, t3), Operande.opDirect(t1)));
					Prog.ajouter(Inst.creation2(Operation.STORE, Operande.opDirect(t1),Operande.creationOpIndexe(0, Registre.GB, t2)));
					Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t3)));
					Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t2)));
					Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t4)));
					Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e1)));
					Prog.ajouter(e2);
				
					gestRegistre.freeRegistre(t1);
					gestRegistre.freeRegistre(t2);
					gestRegistre.freeRegistre(t3);
					gestRegistre.freeRegistre(t4);
					break;
				case Boolean:
				case Interval:
				case Real:
					t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,false);
			   		parcoursPlaceStore(a.getFils1(),prog,addrStack,gestRegistre,t2);
			   		gestRegistre.freeRegistre(t2);
			   		break;
				default:
					break;
		   		}
		   		
		   		break;
		   	case Pour:
		   		parcoursPour(a,prog,addrStack,gestRegistre);
		   		break;
		   	case TantQue:
		   		parcoursTantQue(a,prog,addrStack,gestRegistre);
		   		break;
		   	case Si:
		   		parcoursSi(a, prog, addrStack, gestRegistre);
		   		break;
		   	case Lecture:
		   		parcoursLecture(a,prog,addrStack,gestRegistre);
		   		break;
		   	case Ecriture:
		   		parcoursListeExp(a.getFils1(),prog,addrStack,gestRegistre);
		   		break;
		   	case Ligne:
		   		Prog.ajouter(Inst.creation0(Operation.WNL));
		   		break;
		   	default:
		   		break;
		}
	}
	
	private static void parcoursPour(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre) {
		Registre t2;
		Registre t1;
		Etiq e1 = Etiq.nouvelle("etiq");
		Etiq e2 = Etiq.nouvelle("etiq");
		ArrayList<Inst> inst = new ArrayList<Inst>();
		t2=parcoursExp(a.getFils1().getFils2(),prog,addrStack,gestRegistre,false);
		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t2);
		gestRegistre.freeRegistre(t2);
		Prog.ajouter(e1);
		switch(a.getFils1().getNoeud()) {
		case Decrement:
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
			t2=parcoursExp(a.getFils1().getFils3(),prog,addrStack,gestRegistre,false);
			Prog.ajouter((Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1))));
			gestRegistre.freeRegistre(t2);
			gestRegistre.freeRegistre(t1);
			Prog.ajouter((Inst.creation1(Operation.BLT,Operande.creationOpEtiq(e2))));
			parcoursListeInst(a.getFils2(),prog,addrStack,gestRegistre);
	   		
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
	   		Prog.ajouter((Inst.creation2(Operation.SUB, Operande.creationOpEntier(1),Operande.opDirect(t1))));
	   		Prog.ajouter((Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith))));
	   		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t1);
	   		gestRegistre.freeRegistre(t1);
			
			inst.add(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e1)));
			ajouterInst(inst,prog);
			Prog.ajouter(e2);
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
	   		Prog.ajouter((Inst.creation2(Operation.ADD, Operande.creationOpEntier(1),Operande.opDirect(t1))));
	   		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t1);
	   		gestRegistre.freeRegistre(t1);
			break;
		case Increment:
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
			t2=parcoursExp(a.getFils1().getFils3(),prog,addrStack,gestRegistre,false);
			Prog.ajouter((Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1))));
			gestRegistre.freeRegistre(t2);
			gestRegistre.freeRegistre(t1);
			Prog.ajouter((Inst.creation1(Operation.BGT,Operande.creationOpEtiq(e2))));
			parcoursListeInst(a.getFils2(),prog,addrStack,gestRegistre);
	   		
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
	   		Prog.ajouter((Inst.creation2(Operation.ADD, Operande.creationOpEntier(1),Operande.opDirect(t1))));
	   		Prog.ajouter((Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith))));
	   		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t1);
	   		gestRegistre.freeRegistre(t1);
			
			Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e1)));
			Prog.ajouter(e2);
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
	   		Prog.ajouter((Inst.creation2(Operation.SUB, Operande.creationOpEntier(1),Operande.opDirect(t1))));
	   		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t1);
	   		gestRegistre.freeRegistre(t1);
			break;
		default:
			break;
		}
		
	}
	private static void parcoursTantQue(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		Etiq e1 = Etiq.nouvelle("etiq");
		Etiq e2 = Etiq.nouvelle("etiq");
		Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e2)));
		Prog.ajouter(e1);
		parcoursListeInst(a.getFils2(), prog, addrStack, gestRegistre);
		Prog.ajouter(e2);
		parcoursCond(a.getFils1(), prog, addrStack, gestRegistre, e1);
	}
	
	private static void parcoursSi(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		Etiq e1 = Etiq.nouvelle("etiq");
		Etiq e2 = Etiq.nouvelle("etiq");
		parcoursCond(a.getFils1(), prog, addrStack, gestRegistre, e1);
		parcoursListeInst(a.getFils3(), prog, addrStack, gestRegistre);
		Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e2)));
		Prog.ajouter(e1);
		parcoursListeInst(a.getFils2(), prog, addrStack, gestRegistre);
		Prog.ajouter(e2);
	}
	
	private static void parcoursCond(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre, Etiq e1){
		Registre t1=parcoursExp(a, prog, addrStack, gestRegistre,false);
		ArrayList<Inst> inst = new ArrayList<Inst>();
		switch(a.getNoeud()){
		case Egal:
			inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Et:
			inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Inf:
			inst.add(Inst.creation1(Operation.BLT, Operande.creationOpEtiq(e1)));
			break;
		case InfEgal:
			inst.add(Inst.creation1(Operation.BLE, Operande.creationOpEtiq(e1)));
			break;
		case Non:
			inst.add(Inst.creation1(Operation.BNE, Operande.creationOpEtiq(e1)));
			break;
		case NonEgal:
			inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Ou:
			inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Sup:
			inst.add(Inst.creation1(Operation.BGT, Operande.creationOpEtiq(e1)));
			break;
		case SupEgal:
			inst.add(Inst.creation1(Operation.BGE, Operande.creationOpEtiq(e1)));
			break;
		case Ident:
			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpEntier(1), Operande.opDirect(t1)));
			inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		default:
			break;	
		}
		gestRegistre.freeRegistre(t1);
		ajouterInst(inst, prog);
	}
	
	private static Registre parcoursPlaceStore(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre, Registre R){
		   Registre t1;
		   switch(a.getNoeud()) {
		   	case Index:
		   		t1=parcoursPlaceTablLoad(a,prog,addrStack,gestRegistre);
		   		switch(a.getDecor().getType().getNature()) {
				case Interval:
					codeDebordInterval(a.getDecor().getType().getBorneInf(), a.getDecor().getType().getBorneSup(), gestRegistre,R,false);
					break;
				default:
					break;
				
				}
		   		Prog.ajouter(Inst.creation2(Operation.STORE,Operande.opDirect(R),Operande.creationOpIndexe(0, Registre.GB, t1)));
		   		gestRegistre.freeRegistre(t1);
		   		return t1;
		   	case Ident:
		   		t1=parcoursIDFStore(a,prog,addrStack,gestRegistre,R);
		   		return t1;
		   	default:
		   }
		return null;
	   }
	
	private static Registre parcoursPlaceLoad(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre,boolean table){
		   Registre t1;
		   switch(a.getNoeud()) {
		   	case Index:
		   		t1=parcoursPlaceTablLoad(a,prog,addrStack,gestRegistre);
		   		if(t1==null){
		   			if(!table) {
		   				Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   			Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpIndexe(0, Registre.GB, Registre.R14),Operande.opDirect(Registre.R14)));;
			   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
		   			}
		   			return null;
		   		}
		   		else{
		   			if(!table) {
			   			Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpIndexe(0, Registre.GB, t1),Operande.opDirect(t1)));;
			   		}
			   		return t1;
		   		}
		   	case Ident:
		   		t1=parcoursIDFLoad(a,prog,addrStack,gestRegistre,false);
		   		return t1;
		   	default:
		   }
		return null;
	   }
	
	private static Registre parcoursPlaceTablLoad(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		   Registre t1;
		   Registre t2;
		   switch(a.getNoeud()) {
		   	case Index:
		   		t1=parcoursPlaceTablLoad(a.getFils1(),prog,addrStack,gestRegistre);
		   		t2=parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,false);
		   		int mul=parcoursTaille(a.getDecor().getType());
		   		int inf,sup;
		   		if(a.getFils1().getNoeud()==Noeud.Ident){
		   			inf=a.getFils1().getDecor().getDefn().getType().getIndice().getBorneInf();
		   			sup=a.getFils1().getDecor().getDefn().getType().getIndice().getBorneSup();
		   		}
		   		else{
		   			inf=a.getFils1().getDecor().getType().getIndice().getBorneInf();
		   			sup=a.getFils1().getDecor().getType().getIndice().getBorneSup();
		   		}
		   		if(t1==null && t2==null){
	   				//TRAVAIL ICI
	   				Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
	   				codeDebordInterval(inf, sup,gestRegistre,Registre.R15,true);
	   				Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
	   				Prog.ajouter(Inst.creation2(Operation.SUB, Operande.creationOpEntier(inf), Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(mul), Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
	   			}
	   			else if(t1==null){
	   				Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
	   				codeDebordInterval(inf, sup,gestRegistre,Registre.R15,true);
	   				Prog.ajouter(Inst.creation2(Operation.SUB, Operande.creationOpEntier(inf), Operande.opDirect(t2)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(mul), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(t2), Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R15), Operande.opDirect(t2)));
			   		return t2;
	   			}
	   			else if(t2==null){
	   				Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
	   				codeDebordInterval(inf, sup,gestRegistre,Registre.R15,true);
	   				Prog.ajouter(Inst.creation2(Operation.SUB, Operande.creationOpEntier(inf), Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(mul), Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R15), Operande.opDirect(t1)));
	   			}
	   			else{
	   				codeDebordInterval(inf, sup,gestRegistre,t2,true);
	   				Prog.ajouter(Inst.creation2(Operation.SUB, Operande.creationOpEntier(inf), Operande.opDirect(t2)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(mul), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
	   			}
		   		return t1;
		   	case Ident:
		   		t1=parcoursIDFLoad(a,prog,addrStack,gestRegistre,true);
		   		return t1;
		   	default:
		   }
		return null;
	   }
	
	private static Registre parcoursIDFStore(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre,Registre R) {
		ArrayList<Inst> inst = new ArrayList<Inst>();
		String ident = a.getChaine();
		int offset = addrStack.chercher(ident);
		switch(a.getDecor().getDefn().getType().getNature()) {
		case Interval:
			codeDebordInterval(a.getDecor().getDefn().getType().getBorneInf(), a.getDecor().getDefn().getType().getBorneSup(), gestRegistre,R,false);
			break;
		default:
			break;
		
		}
		inst.add(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset, Registre.GB)));	
		ajouterInst(inst, prog);
		gestRegistre.freeRegistre(R);
		
		return null;
	}
	
	private static Registre parcoursIDFLoad(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre, boolean table) {
		ArrayList<Inst> inst = new ArrayList<Inst>();
		String ident = a.getChaine();
		Registre R = gestRegistre.getRegistre();
		if(ident.equals("true")) {
			if(R==null){
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
				inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			}
			else{
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(R)));
			}
			
		}
		else if(ident.equals("false")) {
			if(R==null){
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0),Operande.opDirect(Registre.R14)));
				inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			}
			else{
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0),Operande.opDirect(R)));
			}
		}
		else if(ident.equals("max_int")){
			if(R==null){
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(java.lang.Integer.MAX_VALUE),Operande.opDirect(Registre.R14)));
				inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			}
			else{
				inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(java.lang.Integer.MAX_VALUE),Operande.opDirect(R)));
			}
			
		}
		else {
			int offset = addrStack.chercher(ident);
			if(!table) {
				if(R==null){
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset, Registre.GB),Operande.opDirect(Registre.R14)));
					inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
				}
				else{
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset, Registre.GB),Operande.opDirect(R)));
				}
			}
			else {
				if(R==null){
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(offset),Operande.opDirect(Registre.R14)));
					inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
				}
				else{
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(offset),Operande.opDirect(R)));
				}
				
			}
		}
		ajouterInst(inst, prog);
		
		return R;
	}
	
	
	static void parcoursListeExp(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		   	Registre t1;
		   	ArrayList<Inst> inst = new ArrayList<Inst>();
		   	NatureType b;
			switch(a.getNoeud()) {
		   	case ListeExp:
		   		parcoursListeExp(a.getFils1(),prog,addrStack,gestRegistre);
		   		t1=parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,false);
		   		try{
		   			b=a.getFils2().getDecor().getDefn().getType().getNature();
		   		}
		   		catch(Exception e){
		   			b=a.getFils2().getDecor().getType().getNature();
		   		}
		   		switch(b) {
				case Interval:
					inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(t1), Operande.opDirect(Registre.R1)));
					inst.add(Inst.creation0(Operation.WINT));
					ajouterInst(inst,prog);
					gestRegistre.freeRegistre(t1);
					break;
				case Real:
					inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(t1), Operande.opDirect(Registre.R1)));
					inst.add(Inst.creation0(Operation.WFLOAT));
					ajouterInst(inst,prog);
					gestRegistre.freeRegistre(t1);
					break;
				default:
					break;
		   			
		   		}
		   	case Vide:
		   		break;
		   	default:
		   }
	   }

	
	static Registre parcoursExp(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre,boolean table) {
		Registre t1;
		Registre t2;
		ArrayList<Inst> inst = new ArrayList<Inst>();
		switch(a.getNoeud()) {

		   	case Vide:
		   		break;
		   	case Plus:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
		   			ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else{
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Moins:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.SUB, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
		   			ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.SUB, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			//inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.SUB, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
		   			ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else{
		   			inst.add(Inst.creation2(Operation.SUB, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Et:
		   		
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R15)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15),Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
		   			ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(t1), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
		   			ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14),Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
		   			ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else{
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(t1), Operande.opDirect(t2)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   		
		   	case Ou:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(Registre.R15)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14),Operande.opDirect(Registre.R15)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst,prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14),Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(t1), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
		   			inst.add(Inst.creation2(Operation.ADD, Operande.opDirect(t1), Operande.opDirect(t2)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   		
		   	case Egal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case NonEgal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SNE, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SNE, Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SNE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SNE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case InfEgal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SLE, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SLE, Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SLE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SLE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Inf:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SLT, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SLT, Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SLT, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SLT, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case SupEgal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Sup:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SGT, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.SGT, Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SGT, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.SGT, Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case DivReel:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);

		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R15)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));	
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Quotient:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);

		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R15)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));	
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Mult:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
		   			ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			//inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
		   			inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else{
		   			inst.add(Inst.creation2(Operation.MUL, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		inst.add(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Reste:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);		   		

		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R15)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.MOD, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));	
			   		inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.MOD, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.MOD, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		inst.add(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		inst.add(Inst.creation2(Operation.MOD, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Index:
		   		t1 = parcoursPlaceLoad(a,prog,addrStack,gestRegistre,table);
		   		return t1;
		   	case MoinsUnaire:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		if(t1==null){
		   			
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			switch(a.getFils1().getNoeud()) {
		   			case Index:
		   				switch(a.getFils1().getDecor().getType().getNature()) {
		   				case Interval:
				   			inst.add(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				case Real:
				   			inst.add(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				default:
		   					break;
		   				
		   				}
		   				break;
		   			case Ident:
		   				switch(a.getFils1().getDecor().getDefn().getType().getNature()) {
		   				case Interval:
				   			inst.add(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				case Real:
		   					inst.add(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				default:
		   					break;
		   				}
		   				break;
		   			default:
		   				break;
		   			}
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else{
		   			switch(a.getFils1().getNoeud()) {
		   			case Index:
		   				switch(a.getFils1().getDecor().getType().getNature()) {
		   				case Interval:
				   			inst.add(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				case Real:
				   			inst.add(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				default:
		   					break;
		   				
		   				}
		   				break;
		   			case Ident:
		   				switch(a.getFils1().getDecor().getDefn().getType().getNature()) {
		   				case Interval:
				   			inst.add(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(t1)));
		   					break;
		   				case Real:
		   					inst.add(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(t1)));
		   					break;
		   				default:
		   					break;
		   				}
		   				break;
		   			default:
		   				break;
		   			}
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   	case PlusUnaire:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		if(t1==null){
			   		
		   		}
		   		else{
		   			
		   		}
		   		return t1;
		   	case Non:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(Registre.R14)));
			   		inst.add(Inst.creation2(Operation.MOD, Operande.creationOpEntier(2), Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else{
		   			inst.add(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t1)));
			   		inst.add(Inst.creation2(Operation.MOD, Operande.creationOpEntier(2), Operande.opDirect(t1)));
			   		ajouterInst(inst,prog);
			   		return t1;
		   		}
		   		
		   	case Conversion:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = gestRegistre.getRegistre();
		   		if(t1==null && t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation2(Operation.FLOAT,Operande.opDirect(Registre.R14),Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
			   		ajouterInst(inst, prog);
			   		return null;
		   		}
		   		else if(t1==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.FLOAT,Operande.opDirect(Registre.R14),Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		return t2;
		   		}
		   		else if(t2==null){
		   			inst.add(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.FLOAT,Operande.opDirect(t1),Operande.opDirect(Registre.R14)));
		   			inst.add(Inst.creation2(Operation.LOAD,Operande.opDirect(Registre.R14),Operande.opDirect(t1)));
			   		ajouterInst(inst, prog);
			   		return t1;
		   		}
		   		else{
			   		inst.add(Inst.creation2(Operation.FLOAT,Operande.opDirect(t1),Operande.opDirect(t2)));
			   		ajouterInst(inst, prog);
			   		gestRegistre.freeRegistre(t1);
			   		return t2;
		   		}

		   	case Ident:
		   		t1 = parcoursIDFLoad(a,prog,addrStack,gestRegistre,table);
		   		return t1;
		   	case Chaine:
		   		inst.add(Inst.creation1(Operation.WSTR, Operande.creationOpChaine(a.getChaine())));
		   		ajouterInst(inst, prog);
		   		break;
		   	case Entier:
		   		t1=gestRegistre.getRegistre();
		   		int alpha = a.getEntier();
		   		if(t1==null){
		   			inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(alpha), Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
		   			ajouterInst(inst,prog);
		   		}
		   		else{
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(alpha), Operande.opDirect(t1)));
					ajouterInst(inst, prog);
		   		}
		   		return t1;
		   	case Reel:
		   		t1=gestRegistre.getRegistre();
		   		float beta = a.getReel();
		   		if(t1==null){
		   			inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpReel(beta), Operande.opDirect(Registre.R15)));
		   			inst.add(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
		   			ajouterInst(inst,prog);
		   		}
		   		else{
					inst.add(Inst.creation2(Operation.LOAD, Operande.creationOpReel(beta), Operande.opDirect(t1)));
					ajouterInst(inst, prog);
		   		}
				return t1;
		   	default:
		   		break;
		   }
		return null;
}

	
	static void parcoursLecture(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre) {	
		Registre R = Registre.R1;
		gestRegistre.alloueRegistre(R);
		ArrayList<Inst> inst = new ArrayList<Inst>();
		switch(a.getFils1().getNoeud()) {
		case Index:
			switch(a.getFils1().getDecor().getType().getNature()) {
			case Interval:
				inst.add(Inst.creation0(Operation.RINT));
				break;
			case Real:
				inst.add(Inst.creation0(Operation.RFLOAT));
				break;
			default:
				break;
			
			}
			break;
		case Ident:
			switch(a.getFils1().getDecor().getDefn().getType().getNature()) {
			case Interval:
				inst.add(Inst.creation0(Operation.RINT));
				break;
			case Real:
				inst.add(Inst.creation0(Operation.RFLOAT));
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
		
		ajouterInst(inst,prog);
		parcoursPlaceStore(a.getFils1(),prog,addrStack,gestRegistre,R);
		gestRegistre.freeRegistre(R);
	}
		
	static void codeDebordInterval (int inf, int sup, GestRegistres gestRegitre, Registre R, boolean tab )
	{
		Registre R2 = gestRegitre.getRegistre();
		if(R2==null){
			R2=Registre.R14;
		}
		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(inf), Operande.opDirect(R2)));
		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(R2), Operande.opDirect(R)));
		if(tab){
			Prog.ajouter(Inst.creation1(Operation.BLT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Indice)));
		}
		else
		{
			Prog.ajouter(Inst.creation1(Operation.BLT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Intervalle)));
		}
		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(sup), Operande.opDirect(R2)));
		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(R2), Operande.opDirect(R)));
		if(tab){
			Prog.ajouter(Inst.creation1(Operation.BGT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Indice)));
		}
		else
		{
			Prog.ajouter(Inst.creation1(Operation.BGT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Intervalle)));
		}
		gestRegitre.freeRegistre(R2);
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
					addr.allouer(name,NatureType.Array,borne.toArray(arrayBorne));
				}
				
			}
			else parcoursDecl(a.getFils1(), addr);
			break;
		case ListeIdent : 
			parcoursDecl(a.getFils1(), addr);
			parcoursDecl(a.getFils2(), addr);
			break;
		case Ident :
			addr.allouer(a.getChaine(), a.getDecor().getDefn().getType().getNature());
			break;
		default:
			break;
		}
		return;
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



