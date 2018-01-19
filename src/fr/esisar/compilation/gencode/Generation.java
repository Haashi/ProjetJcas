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
      //Instantiation et déclaration des objets utiles
	  Inst inst;
      Adresse addrStack = new Adresse();
      GestRegistres gestRegistre = new GestRegistres();
      
      //Début du parcours de l'arbre de déclarations
      parcoursDecl(a.getFils1(),addrStack);
      
      if(addrStack.getOffset() < 0)
      { //Débordement d'entier
    	 throw new RuntimeException("Erreur Interne adresse.offset < 0, le compilateur ne peut pas gérer autant d'allocations de place en pile");
      }
      //Test si on peut ajouter autant de mots déclarés dans la pile
      Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(addrStack.getOffset())));
      
      Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
      //Instruction pour déplacer le StackPointer, dépend du nombre de variable
      Prog.ajouter(Inst.creation1(Operation.ADDSP, Operande.creationOpEntier(addrStack.getOffset())));
      //Début du pacours de l'arbre d'instructions
      parcoursListeInst(a.getFils2(), Prog.instance(), addrStack,gestRegistre);
    
      
      // Fin du programme
      
      // L'instruction "HALT"
      inst = Inst.creation0(Operation.HALT);
      // On ajoute l'instruction à la fin du programme
      Prog.ajouter(inst);
      
      //Ajout des étiquettes 
      Prog.ajouter(Prog.L_Etiq_Debordement_Intervalle);
      Prog.ajouter(Inst.creation1(Operation.WSTR, Operande.creationOpChaine("Debordement intervalle")));
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
	
	//Parcours de la liste d'instructions en suivant la grammaire du language
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
	
	//Programme pour parcourir la taille des cases à droite de mon indice dans un tableau, fonction utile dans le calcul de l'offset lors
	//d'un accès a une case d'un tableau, exemple : i:array[1..2] of array [a..b] of array [c..d] of integer
	//renvoie (b-a)*(d-c) si on l'appel avec i[1]    / renvoie (d-c) si on l'appel avec i[1][2]
	//c'est une fonction récursive qui parcours le Type
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
	
	//Début du parcours de l'arbre d'une instruction en fonction du type du noeud.
	static void parcoursInst(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre) {
		//on déclare les registres qu'on va utiliser
		Registre t2;
		Registre t1;
		Registre t3;
		Registre t4;
		switch(a.getNoeud()) {
		   	case Vide:
		   		break;
		   	case Nop:
		   		break;
		   	case Affect:
		   		//lors d'une affectation, on regarde le type d'affectation, soit une affectation de type Array, soit de type Boolean,Interval,Real
		   		//qui se traite de la même manière
		   		switch(a.getDecor().getType().getNature()) {
				case Array:
					
					//Pour l'affectation d'un tableau à un tableau, on utilise l'adressage contigue pour affecter un par un chaque élement du tableau
					//grâce à son offset, pour ça on génère une boucle en assembleur qui parcours la taille du tableau à affecter.
					
					//creation des étiquettes pour boucler
					Etiq e1 = Etiq.nouvelle("etiq"), e2 = Etiq.nouvelle("etiq");
					boolean convertir=false;
					
					if(a.getFils2().getNoeud()==Noeud.Conversion){
						//Cas ou on affecte un tableau d'entier à un tableau de réel, un Noeud.Conversion est inséré en passe 2
						//pour savoir qu'il faut convertir chaque élément du tableau à affecter
						//il faut donc ignorer le Noeud.Conversion pour parcourir l'expression plus bas, d'où l'appel .getFils1() par rapport au cas else
						t3 = parcoursExp(a.getFils2().getFils1(),prog,addrStack,gestRegistre,true);
						convertir=true;
					}
					else{
						t3 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,true);
					}
					//on réalise le parcours des fils qui sont des expressions, qui donneront en résultat un registre qui contiendra l'adresse des tableaux
					//le paramètre true permet de spécifier que l'on se situe dans le cas d'une affectation d'array, dont la génération de code est
					// différente dans parcours d'expression classique (voir la fonction parcoursExp)
					t2 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,true);
					
					//on récupère la taille du tableau pour itérer l'affectation valeur par valeur sur tous le tableau
					int taille = parcoursTaille(a.getDecor().getType());
					
					//on recupère des registres pour faire les calculs
					t1= gestRegistre.getRegistre();
					t4 = gestRegistre.getRegistre();
					
					//Generation du code de la boucle
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0), Operande.opDirect(t4)));
					Prog.ajouter(e1);
					Prog.ajouter(Inst.creation2(Operation.CMP,Operande.creationOpEntier(taille),Operande.opDirect(t4)));
					Prog.ajouter(Inst.creation1(Operation.BGE, Operande.creationOpEtiq(e2)));
					
					//Generation du code de l'affectation valeur par valeur en regardant si il est necessaire de convertir ou pas
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpIndexe(0, Registre.GB, t3), Operande.opDirect(t1)));
					if(convertir)Prog.ajouter(Inst.creation2(Operation.FLOAT, Operande.opDirect(t1), Operande.opDirect(t1)));	
					Prog.ajouter(Inst.creation2(Operation.STORE, Operande.opDirect(t1),Operande.creationOpIndexe(0, Registre.GB, t2)));
					
					//Code de fin de boucle
					Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t3)));
					Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t2)));
					Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t4)));
					Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e1)));
					Prog.ajouter(e2);
				
					//on libère tous les registres après leurs utilisations pour que les instructions suivantes puissent en utiliser.
					gestRegistre.freeRegistre(t1);
					gestRegistre.freeRegistre(t2);
					gestRegistre.freeRegistre(t3);
					gestRegistre.freeRegistre(t4);
					break;
				case Boolean:
				case Interval:
				case Real:
					//Lors d'une affectation, on récupère d'abord la valeur à affecter dans le registre t2 via un parcours d'expression, on spécifie
					// le paramètre false car cette fois si on est plus dans le cas d'affectation tableau à tableau
					t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,false);
					
					//on passe en paramètre le registre dans lequel est stocké la valeur pour aller l'affecter en pile
					//voir fonction
					parcoursPlaceStore(a.getFils1(),prog,addrStack,gestRegistre,t2);
			   		//on libère le registre après la fin de l'instruction, le deuxième registre utilisé ayant été libéré par parcoursPlaceStore
			   		gestRegistre.freeRegistre(t2);
			   		break;
				default:
					break;
		   		}
		   		
		   		break;
		   	case Pour:
		   		//voir fonction
		   		parcoursPour(a,prog,addrStack,gestRegistre);
		   		break;
		   	case TantQue:
		   		//voir fonction
		   		parcoursTantQue(a,prog,addrStack,gestRegistre);
		   		break;
		   	case Si:
		   		//voir fonction
		   		parcoursSi(a, prog, addrStack, gestRegistre);
		   		break;
		   	case Lecture:
		   		//voir fonction
		   		parcoursLecture(a,prog,addrStack,gestRegistre);
		   		break;
		   	case Ecriture:
		   		//voir fonction
		   		parcoursListeExp(a.getFils1(),prog,addrStack,gestRegistre);
		   		break;
		   	case Ligne:
		   		//ajout d'une instruction pour écrire sur une nouvelle ligne
		   		Prog.ajouter(Inst.creation0(Operation.WNL));
		   		break;
		   	default:
		   		break;
		}
	}
	
	//fonction pour générer le code de l'instruction pour
	private static void parcoursPour(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre) {
		//déclaration des objets utiles
		Registre t2;
		Registre t1;
		Etiq e1 = Etiq.nouvelle("etiq");
		Etiq e2 = Etiq.nouvelle("etiq");
		
		//on réalise un parcours Expression pour l'identifiant qui sert de variable de boucle
		t2=parcoursExp(a.getFils1().getFils2(),prog,addrStack,gestRegistre,false);
		//et on réalise l'affectation comme dans un Noeud.Affect avec la valeur initiale de la boucle
		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t2);
		//on libère le registre qu'on a utilisé pour l'affectation
		gestRegistre.freeRegistre(t2);
		//on ajoute l'etiquette pour boucler
		Prog.ajouter(e1);
		switch(a.getFils1().getNoeud()) {
		case Decrement:
			//on va chercher la variable de boucle
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
			//on va cherher la borne droite du for : ici, on peut aller chercher un identifiant, la valeur n'est pas forcément constante
			//c'est un choix qui a été fait, il peut engendrer des effets de bords
			t2=parcoursExp(a.getFils1().getFils3(),prog,addrStack,gestRegistre,false);
			//On les compare
			Prog.ajouter((Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1))));
			//on free les registres après la comparaison
			gestRegistre.freeRegistre(t2);
			gestRegistre.freeRegistre(t1);
			//on branche sur le reste du code si la valeur dépasse la borne 
			Prog.ajouter((Inst.creation1(Operation.BLT,Operande.creationOpEtiq(e2))));
			//on génere la liste d'intructions du for
			parcoursListeInst(a.getFils2(),prog,addrStack,gestRegistre);
	   		
			//on recharge la variable de boucle
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
			//Dans le cas d'un décrément, on la décrémente, et on verifie l'overflow
	   		Prog.ajouter((Inst.creation2(Operation.SUB, Operande.creationOpEntier(1),Operande.opDirect(t1))));
	   		Prog.ajouter((Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith))));
	   		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t1);
	   		gestRegistre.freeRegistre(t1);
			//on reboucle
			Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e1)));
			/*code de fin de boucle, on incrémente une fois la variable de boucle ( en effet si on est sorti, c'est que la valeur vaut borne-1
			or elle doit valoir borne), il n'est pas nécessaire ici de vérifier l'overflow car on a déjà itéré sur cette valeur*/
			Prog.ajouter(e2);
			t1=parcoursPlaceLoad(a.getFils1().getFils1(),prog,addrStack,gestRegistre,false);
	   		Prog.ajouter((Inst.creation2(Operation.ADD, Operande.creationOpEntier(1),Operande.opDirect(t1))));
	   		parcoursPlaceStore(a.getFils1().getFils1(),prog,addrStack,gestRegistre,t1);
	   		gestRegistre.freeRegistre(t1);
			break;
		case Increment:
			/*Squelette identique au decrement, sauf qu'on incrémente la variable de boucle, on change le sens de la comparaison et on 
			décrémente au lieu d'incrémenter à la fin de la boucle*/
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
	//fonction pour générer le code de l'instruction while
	private static void parcoursTantQue(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		Etiq e1 = Etiq.nouvelle("etiq");
		Etiq e2 = Etiq.nouvelle("etiq");
		//On fait un branchement inconditionnelle vers la comparaison
		Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e2)));
		Prog.ajouter(e1);
		//On génère les instructions propre au for
		parcoursListeInst(a.getFils2(), prog, addrStack, gestRegistre);
		Prog.ajouter(e2);
		//on donne l'étiquette du début de la boucle à la fonction parcoursCond, c'est elle qui generera le branch
		//voir la fonction
		parcoursCond(a.getFils1(), prog, addrStack, gestRegistre, e1);
	}
	
	//fonction pour générer le code de l'instruction if/else
	private static void parcoursSi(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		Etiq e1 = Etiq.nouvelle("etiq");
		Etiq e2 = Etiq.nouvelle("etiq");
		//on verifie la condition et on branche sur e1 si elle est vraie
		parcoursCond(a.getFils1(), prog, addrStack, gestRegistre, e1);
		//on génère le code des instructions ELSE
		//Si il n'y a pas de else, ce noeud sera vide, donc rien ne sera générer ce qui rend cette fonction
		//identique en cas d'appel avec if ou if/else
		parcoursListeInst(a.getFils3(), prog, addrStack, gestRegistre);
		//si on est dans le else, on branche sur les instructions APRES les instructions du IF
		Prog.ajouter(Inst.creation1(Operation.BRA, Operande.creationOpEtiq(e2)));
		Prog.ajouter(e1);
		//On genere le code de l'instruction du IF
		parcoursListeInst(a.getFils2(), prog, addrStack, gestRegistre);
		Prog.ajouter(e2);
	}
	
	private static void parcoursCond(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre, Etiq e1){
		//on parcours l'expression booléenne et on la stocke dans le registre R1
		//la fonction parcoursEXP va effectuer le calcul, et donc set les CC associé, il ne reste plus qu'à branch sur l'opération associé
		Registre t1=parcoursExp(a, prog, addrStack, gestRegistre,false);
		switch(a.getNoeud()){
		case Egal:
			Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Et:
			Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Inf:
			Prog.ajouter(Inst.creation1(Operation.BLT, Operande.creationOpEtiq(e1)));
			break;
		case InfEgal:
			Prog.ajouter(Inst.creation1(Operation.BLE, Operande.creationOpEtiq(e1)));
			break;
		case Non:
			Prog.ajouter(Inst.creation1(Operation.BNE, Operande.creationOpEtiq(e1)));
			break;
		case NonEgal:
			Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Ou:
			Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		case Sup:
			Prog.ajouter(Inst.creation1(Operation.BGT, Operande.creationOpEtiq(e1)));
			break;
		case SupEgal:
			Prog.ajouter(Inst.creation1(Operation.BGE, Operande.creationOpEtiq(e1)));
			break;
		case Ident:
			//si c'est un ident, il est necessaire d'évaluer sa valeur (en la comparant à 1) avant de brancher
			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpEntier(1), Operande.opDirect(t1)));
			Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(e1)));
			break;
		default:
			break;	
		}
		gestRegistre.freeRegistre(t1);
	}
	//fonction pour générer le parcours de l'identifiant pour une affectation/lecture
	private static Registre parcoursPlaceStore(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre, Registre R){
		   Registre t1;
		   //deux types d'affectation, soit à un tableau (case index), soit ) un identifiant
		   switch(a.getNoeud()) {
		   	case Index:
		   		//on va chercher la place en pile grâce à une fonction qui calcul l'offset par rapport à l'adresse de l'identifiant
		   		//voir fonction
		   		t1=parcoursPlaceTablLoad(a,prog,addrStack,gestRegistre);
		   		//on génére le code pour le débordemment d'interval si on affecte a un tableau d'intervalles
		   		switch(a.getDecor().getType().getNature()) {
				case Interval:
					//voir fonction
					codeDebordInterval(a.getDecor().getType().getBorneInf(), a.getDecor().getType().getBorneSup(), gestRegistre,R,false);
					break;
				default:
					break;
				
				}
		   		//on sauvegarde la valeur en pile
		   		Prog.ajouter(Inst.creation2(Operation.STORE,Operande.opDirect(R),Operande.creationOpIndexe(0, Registre.GB, t1)));
		   		gestRegistre.freeRegistre(t1);
		   		return t1;
		   	case Ident:
		   		//voir fonction
		   		t1=parcoursIDFStore(a,prog,addrStack,gestRegistre,R);
		   		return t1;
		   	default:
		   }
		return null;
	   }
	//fonction pour générer le parcours de l'identifiant pour un chargement (dans une expression par exemple)
	//Squelette similaire à la fonction parcoursPlaceStore si dessus
	private static Registre parcoursPlaceLoad(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre,boolean table){
		   Registre t1;
		   switch(a.getNoeud()) {
		   	case Index:
		   		t1=parcoursPlaceTablLoad(a,prog,addrStack,gestRegistre);
		   		//si on a pas de registre, c'est que parcoursPlaceTablLoad à empiler son resultat, donc on le depile, on effectue les calculs dans les registres
		   		//réservés R.14 et R.15 pour faire les calculs puis on rempile
		   		//avant de rempiler, il n'es pas necessaire d'utiliser TSTO puisqu'on vient de dépiler
		   		if(t1==null){
		   			if(!table) {
		   				Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   			Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpIndexe(0, Registre.GB, Registre.R14),Operande.opDirect(Registre.R14)));
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
	//cette fonction calcul l'adresse effective dans un element d'un tableau
	//par exemple si i:array of [1..3] of integer; est à l'adresse 1
	//alors la fonction retournera l'adresse 2 pour i[2] dans un registre
	private static Registre parcoursPlaceTablLoad(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		   Registre t1;
		   Registre t2;
		   switch(a.getNoeud()) {
		   	case Index:
		   		t1=parcoursPlaceTablLoad(a.getFils1(),prog,addrStack,gestRegistre);
		   		t2=parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,false);
		   		//on récupère la taille de ce qu'il y a après
		   		int mul=parcoursTaille(a.getDecor().getType());
		   		int inf,sup;
		   		//cas de base ou le dernier noeud index à un fils Noeud.Ident, et donc on doit appeler un Decor Defn, et non plus un Decor Type
		   		if(a.getFils1().getNoeud()==Noeud.Ident){
		   			//on récupère les bornes inf et sup des cases du tableau pour ne pas appeler i[5] sur un tableau de 3 éléments par exemple
		   			inf=a.getFils1().getDecor().getDefn().getType().getIndice().getBorneInf();
		   			sup=a.getFils1().getDecor().getDefn().getType().getIndice().getBorneSup();
		   		}
		   		else{
		   			inf=a.getFils1().getDecor().getType().getIndice().getBorneInf();
		   			sup=a.getFils1().getDecor().getType().getIndice().getBorneSup();
		   		}
		   		//cas si il n'y a plus de registre disponible, on fait les opérations en pile et dans les registres R15 R14
		   		if(t1==null && t2==null){
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
	   				//on utilise les bornes qu'on a récupérer plus haut pour générer un codeDebordInterval, le paramètre true permettra
	   				// de branch sur un segmentationfault au lieu de debordement d'intervalle
	   				codeDebordInterval(inf, sup,gestRegistre,t2,true);
	   				// on utilise la formule pour l'adressage contigue : @ en pile + Somme des (offset-borneinf)*taille du sous tableau
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
	//code pour générer l'enregistrement en pile d'un identifiant
	private static Registre parcoursIDFStore(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre,Registre R) {
		String ident = a.getChaine();
		int offset = addrStack.chercher(ident);
		switch(a.getDecor().getDefn().getType().getNature()) {
		//Cas ou c'est un interval et on doit donc verifier le débordement
		case Interval:
			codeDebordInterval(a.getDecor().getDefn().getType().getBorneInf(), a.getDecor().getDefn().getType().getBorneSup(), gestRegistre,R,false);
			break;
		default:
			break;
		
		}
		//on ajoute Operation.STORE
		Prog.ajouter(Inst.creation2(Operation.STORE, Operande.opDirect(R), Operande.creationOpIndirect(offset, Registre.GB)));	
		gestRegistre.freeRegistre(R);
		
		return null;
	}
	
	//code pour générer l'enregistrement en pile d'un identifiant
	private static Registre parcoursIDFLoad(Arbre a,Prog prog, Adresse addrStack,GestRegistres gestRegistre, boolean table) {
		String ident = a.getChaine();
		//ici on récupère un registre
		Registre R = gestRegistre.getRegistre();
		//on verifie si notre identifiant n'est pas un mot reserver, true/false/max_int
		if(ident.equals("true")) {
			if(R==null){
				//si il n'y a pas de registre de libre, on empile, mais auparavant, on doit verifier si il y a encore de la place en pile
				Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
				Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
		   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
	   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			}
			else{
				//sinon on charge en registre
				Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(R)));
			}
			
		}
		else if(ident.equals("false")) {
			if(R==null){
				Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0),Operande.opDirect(Registre.R14)));
				Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
		   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
				Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			}
			else{
				Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(0),Operande.opDirect(R)));
			}
		}
		else if(ident.equals("max_int")){
			if(R==null){
				Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(java.lang.Integer.MAX_VALUE),Operande.opDirect(Registre.R14)));
				Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
		   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));				
				Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			}
			else{
				Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(java.lang.Integer.MAX_VALUE),Operande.opDirect(R)));
			}
			
		}
		else {
			//si ce n'est pas un mot réservé, on va le chercher dans la pile 
			int offset = addrStack.chercher(ident);
			if(!table) {
				if(R==null){
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset, Registre.GB),Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
					Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
				}
				else{
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpIndirect(offset, Registre.GB),Operande.opDirect(R)));
				}
			}
			else {
				//si table=true, on veut l'adresse du tableau (sert dans le calcul de l'offset pour l'adresse contigue) donc on sauvegarde l'adresse en pile
				if(R==null){
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(offset),Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
					Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
				}
				else{
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(offset),Operande.opDirect(R)));
				}
				
			}
		}
		
		return R;
	}
	
	//Programme qui permet de generer l'écriture d'une liste d'expression
	static void parcoursListeExp(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre){
		   	Registre t1;
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
		   		//on doit verifier le NatureType de ce qu'on écrit pour écrire avec la bonne instruction Write
				case Interval:
					//on doit charger dans R1 l'élément à afficher
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(t1), Operande.opDirect(Registre.R1)));
					Prog.ajouter(Inst.creation0(Operation.WINT));
					gestRegistre.freeRegistre(t1);
					break;
				case Real:
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(t1), Operande.opDirect(Registre.R1)));
					Prog.ajouter(Inst.creation0(Operation.WFLOAT));
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

	//code pour générer les expressions, cette fonction est appelé récursivement et possède plusieurs cas de base, soit un identifiant, soit des constantes
	static Registre parcoursExp(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre,boolean table) {
		Registre t1;
		Registre t2;
		//on génère les instructions associées au type de l'expresion, le principe étant le même pour toutes les instructions, le détail est fait que pour
		// les case PLUS, EGAL, ET, MOINSUNAIRE et les cas de bases
		switch(a.getNoeud()) {

		   	case Vide:
		   		break;
		   	case Plus:
		   		//on va chercher plus loin dans l'arbre
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			//cas ou on a plus de regitre, on utilise R14,R15 et la pile
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
			   		return null;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		return t1;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		return t2;
		   		}
		   		else{
		   			//on fait la somme
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		//on verifie le debordement arithmetiue
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		gestRegistre.freeRegistre(t2);
			   		//on renvoit le registre dans lequel on a stocké le résultat
			   		return t1;
		   		}
		   	case Moins:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.SUB, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
		   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.SUB, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		return t1;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.SUB, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
		   			Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else{
		   			Prog.ajouter(Inst.creation2(Operation.SUB, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Et:
		   		//on va plus loins dans l'arbre
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15),Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
		   			
			   		return null;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(t1), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14),Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else{
		   			//les booléens étant coder comme des entiers (true = 1) (false = 0) on utilise la multiplication pour le Et
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(t1), Operande.opDirect(t2)));
		   			//On compare le resultat à 1
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(t2)));
			   		//on sauvegarde le résultat dans le registre t1
			   		//ces instructions sont nécessaires car la comparaison est nécessaire pour set les CC pour pouvoir branch dans une condition
			   		//avec la fonction ParcoursCond
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   		
		   	case Ou:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14),Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14),Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(t1), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.opDirect(t1), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(1),Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t1),Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   		
		   	case Egal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SEQ, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case NonEgal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SNE, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SNE, Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SNE, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SNE, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case InfEgal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SLE, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SLE, Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SLE, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SLE, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Inf:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SLT, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SLT, Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SLT, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SLT, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case SupEgal:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SGE, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Sup:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SGT, Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.SGT, Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SGT, Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.SGT, Operande.opDirect(t1)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case DivReel:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);

		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Quotient:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);

		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.DIV, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Mult:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);
		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
		   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
			   		return null;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		return t1;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
		   			Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		return t2;
		   		}
		   		else{
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.opDirect(t2), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation1(Operation.BOV,Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Arith)));
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Reste:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		
		   		t2 = parcoursExp(a.getFils2(),prog,addrStack,gestRegistre,table);		   		

		   		if(t1==null && t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R15)));
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpReel(0), Operande.opDirect(Registre.R15)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.MOD, Operande.opDirect(Registre.R15), Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.MOD, Operande.opDirect(t2), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.opDirect(Registre.R14), Operande.opDirect(t2)));
			   		return t2;
		   		}
		   		else if(t2==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.MOD, Operande.opDirect(Registre.R14), Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.creationOpEntier(0), Operande.opDirect(t2)));
			   		Prog.ajouter(Inst.creation1(Operation.BEQ, Operande.creationOpEtiq(Prog.L_Etiq_Div_0)));
			   		Prog.ajouter(Inst.creation2(Operation.MOD, Operande.opDirect(t2), Operande.opDirect(t1)));	
			   		gestRegistre.freeRegistre(t2);
			   		return t1;
		   		}
		   	case Index:
		   		//cas de base, on va chercher la valeur en pile
		   		t1 = parcoursPlaceLoad(a,prog,addrStack,gestRegistre,table);
		   		return t1;
		   	case MoinsUnaire:
		   		//on va chercher plus loins
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		
		   		if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			//si c'est une constante ou un tableau, on peut directement appeler getDecor().getType(), sinon c'est un Defn()
		   			switch(a.getFils1().getNoeud()) {
		   			case Reel:
		   			case Entier:
		   			case Index:
		   				//on regarde le nature type pour multiplier soit par -1, soit par -1.0000E00
		   				switch(a.getFils1().getDecor().getType().getNature()) {
		   				case Interval:
				   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				case Real:
				   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				default:
		   					break;
		   				
		   				}
		   				break;
		   			case Ident:
		   				switch(a.getFils1().getDecor().getDefn().getType().getNature()) {
		   				case Interval:
				   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				case Real:
		   					Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(Registre.R14)));
		   					break;
		   				default:
		   					break;
		   				}
		   				break;
		   			default:
		   				break;
		   			}
		   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else{
		   			switch(a.getFils1().getNoeud()) {
		   			case Ident:
		   				switch(a.getFils1().getDecor().getDefn().getType().getNature()) {
		   				case Interval:
				   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(t1)));
		   					break;
		   				case Real:
		   					Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(t1)));
		   					break;
		   				default:
		   					break;
		   				}
		   				break;

		   			case Reel:
		   			case Entier:
		   			case Index:
		   				switch(a.getFils1().getDecor().getType().getNature()) {
		   				case Interval:
				   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(t1)));
		   					break;
		   				case Real:
				   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpReel(-1),Operande.opDirect(t1)));
		   					break;
		   				default:
		   					break;
		   				
		   				}
		   				break;
		   			default:
		   				break;
		   			}
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
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.MUL, Operande.creationOpEntier(-1),Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(Registre.R14)));
			   		Prog.ajouter(Inst.creation2(Operation.MOD, Operande.creationOpEntier(2), Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
			   		Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}
		   		else{
		   			Prog.ajouter(Inst.creation2(Operation.ADD, Operande.creationOpEntier(1), Operande.opDirect(t1)));
			   		Prog.ajouter(Inst.creation2(Operation.MOD, Operande.creationOpEntier(2), Operande.opDirect(t1)));
			   		return t1;
		   		}
		   		
		   	case Conversion:
		   		t1 = parcoursExp(a.getFils1(),prog,addrStack,gestRegistre,table);
		   		if(t1==null){
		   			Prog.ajouter(Inst.creation1(Operation.POP, Operande.opDirect(Registre.R14)));
		   			Prog.ajouter(Inst.creation2(Operation.FLOAT,Operande.opDirect(Registre.R14),Operande.opDirect(Registre.R14)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
		   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R14)));
			   		return null;
		   		}	
		   		else{
			   		Prog.ajouter(Inst.creation2(Operation.FLOAT,Operande.opDirect(t1),Operande.opDirect(t1)));
			   		return t1;
		   		}

		   	case Ident:
		   		//cas de base, on va chercher la valeur en pile
		   		t1 = parcoursIDFLoad(a,prog,addrStack,gestRegistre,table);
		   		return t1;
		   	case Chaine:
		   		//le seul cas ou on appele une chaine, c'est dans une écriture, donc on écrit la chaine
		   		Prog.ajouter(Inst.creation1(Operation.WSTR, Operande.creationOpChaine(a.getChaine().substring(1, a.getChaine().length()-1))));
		   		break;
		   	case Entier:
		   		//si c'est une constante, on alloue un registre et on le charge, si il y a pas de regitre, on le charge en pile
		   		t1=gestRegistre.getRegistre();
		   		int alpha = a.getEntier();
		   		if(t1==null){
		   			Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(alpha), Operande.opDirect(Registre.R15)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
		   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
		   		}
		   		else{
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(alpha), Operande.opDirect(t1)));
		   		}
		   		return t1;
		   	case Reel:
		   		t1=gestRegistre.getRegistre();
		   		float beta = a.getReel();
		   		if(t1==null){
		   			Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpReel(beta), Operande.opDirect(Registre.R15)));
					Prog.ajouter(Inst.creation1(Operation.TSTO, Operande.creationOpEntier(1)));
			   	    Prog.ajouter(Inst.creation1(Operation.BOV, Operande.creationOpEtiq(Prog.L_Etiq_Pile_Pleine)));
		   			Prog.ajouter(Inst.creation1(Operation.PUSH, Operande.opDirect(Registre.R15)));
		   		}
		   		else{
					Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpReel(beta), Operande.opDirect(t1)));
		   		}
				return t1;
		   	default:
		   		break;
		   }
		return null;
}

	//code pour générer les instructions d'une lecture
	static void parcoursLecture(Arbre a, Prog prog, Adresse addrStack,GestRegistres gestRegistre) {	
		//l'élement à lire doit etre absolument dans R1
		Registre R = Registre.R1;
		gestRegistre.alloueRegistre(R);
		
		switch(a.getFils1().getNoeud()) {
		case Index:
			switch(a.getFils1().getDecor().getType().getNature()) {
			//si c'est un int, on utilise RINT
			//si c'est un float, on utilise RFLOAT
			case Interval:
				Prog.ajouter(Inst.creation0(Operation.RINT));
				break;
			case Real:
				Prog.ajouter(Inst.creation0(Operation.RFLOAT));
				break;
			default:
				break;
			
			}
			break;
		case Ident:
			switch(a.getFils1().getDecor().getDefn().getType().getNature()) {
			case Interval:
				Prog.ajouter(Inst.creation0(Operation.RINT));
				break;
			case Real:
				Prog.ajouter(Inst.creation0(Operation.RFLOAT));
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
		//puis on affecte ce qu'on vient de lire dans R1
		parcoursPlaceStore(a.getFils1(),prog,addrStack,gestRegistre,R);
		gestRegistre.freeRegistre(R);
	}
		
	static void codeDebordInterval (int inf, int sup, GestRegistres gestRegitre, Registre R, boolean tab )
	{
		//on alloue un registre pour les calculs
		//si il n'y a plus de registre, on utilise R.14 
		Registre R2 = gestRegitre.getRegistre();
		if(R2==null){
			R2=Registre.R14;
		}
		//on compare la borne inf
		Prog.ajouter(Inst.creation2(Operation.LOAD, Operande.creationOpEntier(inf), Operande.opDirect(R2)));
		Prog.ajouter(Inst.creation2(Operation.CMP, Operande.opDirect(R2), Operande.opDirect(R)));
		if(tab){
			//si c'est un tableau, c'est une Segmentation Fault
			Prog.ajouter(Inst.creation1(Operation.BLT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Indice)));
		}
		else
		{
			//sinon c'est un debordement d'intervalle
			Prog.ajouter(Inst.creation1(Operation.BLT, Operande.creationOpEtiq(Prog.L_Etiq_Debordement_Intervalle)));
		}
		//on compare la borne sup
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
			//appel a la classe auxiliaire
			addr.allouer(a.getChaine(), a.getDecor().getDefn().getType().getNature());
			break;
		default:
			break;
		}
		return;
	}
	
	//fonction par parcourir une liste d'ident pour un tableau
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
	
	//fonction pour parcour les bornes d'un tableau
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



