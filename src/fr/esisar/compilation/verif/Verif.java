package fr.esisar.compilation.verif;

import fr.esisar.compilation.global.src.*;
 
/**
 * Cette classe permet de réaliser la vérification et la décoration 
 * de l'arbre abstrait d'un programme.
 */
public class Verif {

   private Environ env; // L'environnement des identificateurs

   /**
    * Constructeur.
    */
   public Verif() {
      env = new Environ();
   }

   /**
    * Vérifie les contraintes contextuelles du programme correspondant à 
    * l'arbre abstrait a, qui est décoré et enrichi. 
    * Les contraintes contextuelles sont décrites 
    * dans Context.txt.
    * En cas d'erreur contextuelle, un message d'erreur est affiché et 
    * l'exception ErreurVerif est levée.
    */
   public void verifierDecorer(Arbre a) throws ErreurVerif {
      verifier_PROGRAMME(a);
   }

   /**
    * Initialisation de l'environnement avec les identificateurs prédéfinis.
    */
   private void initialiserEnv() {
      Defn def;
      // integer
      def = Defn.creationType(Type.Integer);
      def.setGenre(Genre.PredefInteger);
      env.enrichir("integer", def);
      
      // boolean
      def = Defn.creationType(Type.Boolean);
      def.setGenre(Genre.PredefBoolean);
      env.enrichir("boolean", def);
      
      // real
      def = Defn.creationType(Type.Real);
      def.setGenre(Genre.PredefReal);
      env.enrichir("real", def);
      
      //true
      def = Defn.creationConstBoolean(true);
      def.setGenre(Genre.PredefTrue);
      env.enrichir("true", def);
      
      //false
      def = Defn.creationConstBoolean(false);
      def.setGenre(Genre.PredefFalse);
      env.enrichir("false", def);
      
      //max_int
      def = Defn.creationConstInteger(java.lang.Integer.MAX_VALUE);
      def.setGenre(Genre.PredefMaxInt);
      env.enrichir("max_int", def);
      
      // ------------
      // A COMPLETER
      // ------------
   }

   /**************************************************************************
    * PROGRAMME
    **************************************************************************/
   private void verifier_PROGRAMME(Arbre a) throws ErreurVerif {
	  
      initialiserEnv();
      verifier_LISTE_DECL(a.getFils1());
      verifier_LISTE_INST(a.getFils2());
   }

   /**************************************************************************
    * LISTE_DECL
    **************************************************************************/
   private void verifier_LISTE_DECL(Arbre a) throws ErreurVerif {
	// A COMPLETER
	   switch(a.getNoeud()) {
	   	case ListeDecl:
	   		verifier_LISTE_DECL(a.getFils1());
	   		verifier_DECL(a.getFils2());
	   		break;
	   	case Vide:
	   		break;
	   	default:
	   		throw new ErreurInterneVerif("Arbre incorrect dans verifier_LISTE_DECL");
	   }
   }
   
   private void verifier_DECL(Arbre a) throws ErreurVerif{
	   // A COMPLETER
	   verifier_LISTE_IDF_Decl(a.getFils1(),verifier_TYPE(a.getFils2()));
   }
   
   private void verifier_LISTE_IDF_Decl(Arbre a, Type type) throws ErreurVerif {
	   switch(a.getNoeud()) {
	   	case ListeIdent:
	   		verifier_LISTE_IDF_Decl(a.getFils1(),type);
	   		verifier_IDF_Decl(a.getFils2(),type);
	   		break;
	   	case Vide:
	   		break;
	   	default:
	   		throw new ErreurInterneVerif("Arbre incorrect dans verifier_LISTE_IDF_Decl");
	   }
   }
   
   private void verifier_IDF_Decl(Arbre a, Type type) throws ErreurVerif{
	   Defn def = env.chercher(a.getChaine());
	   if(def!=null) {
		   ErreurContext.ErreurIdentificateurDejaDeclare.leverErreurContext(a.getChaine(), a.getNumLigne());
	   }
	   def = Defn.creationVar(type);
	   def.setGenre(Genre.NonPredefini);
	   env.enrichir(a.getChaine(), def);
	   Decor decor = new Decor(def);
	   a.setDecor(decor);
   }
   
   private Type verifier_TYPE(Arbre a) throws ErreurVerif{
	   Type type = null;
	   switch (a.getNoeud()) {
	   	case Ident:
	   		type = verifier_IDF_Type(a);
	   		break;
	   	case Intervalle:
	   		type = Type.creationInterval(a.getFils1().getEntier(),a.getFils2().getEntier());
	   		break;
	   	case Tableau:
	   		type = Type.creationArray(verifier_TYPE(a.getFils1()), verifier_TYPE(a.getFils2()));
	   		break;
	   	default:
	   		throw new ErreurInterneVerif("Arbre incorrect dans verifier_TYPE");
	   }
	   return type;
   }
   
   
   private Type verifier_IDF_Type(Arbre a) throws ErreurVerif{
	   Defn def = env.chercher(a.getChaine());
	   if(def==null) {
		   ErreurContext.ErreurTypeInconnu.leverErreurContext(a.getChaine(), a.getNumLigne());
	   }
	   else {
		   switch(def.getGenre()) {
		case PredefInteger:
		case PredefBoolean:
		case PredefReal:
			break;
		default:
			ErreurContext.ErreurTypeInconnu.leverErreurContext(a.getChaine(), a.getNumLigne());
		   }
	   }
	   Decor decor = new Decor(def);
	   a.setDecor(decor);
	   return def.getType();
   }
   
   /**************************************************************************
    * LISTE_INST
    **************************************************************************/
   private void verifier_LISTE_INST(Arbre a) throws ErreurVerif {
	   switch(a.getNoeud()) {
	   	case ListeInst:
	   		verifier_LISTE_INST(a.getFils1());
	   		verifier_INST(a.getFils2());
	   		break;
	   	case Vide:
	   		break;
	   	default:
	   		throw new ErreurInterneVerif("Arbre incorrect dans verifier_LISTE_INST");
	   }
   }
   
   private void verifier_INST(Arbre a) throws ErreurVerif {
	   Type t1;
	   Type t2;
	   switch(a.getNoeud()) {
	   	case Vide:
	   		break;
	   	case Nop:
	   		break;
	   	case Affect:
	   		t1 = verifier_PLACE(a.getFils1());
	   		
	   		t2 = verifier_EXP(a.getFils2());
	   		

	   		ResultatAffectCompatible res = ReglesTypage.affectCompatible(t1,t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			Decor decor = new Decor(t1);
	 	 	   decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
	 	 	   conv.setDecor(decor);
	   			a.setFils2(conv);
	   			
	   		}
	 	   Decor decor = new Decor(t1);
	 	   decor.setInfoCode(( a.getFils2().getDecor().getInfoCode()));
	 	   a.setDecor(decor);
	   		break;
	   	case Pour:
	   		ReglesTypage.VerifFor(a.getFils1().getNoeud(), verifier_IDF_Inst(a.getFils1().getFils1()) , verifier_EXP(a.getFils1().getFils2()), verifier_EXP(a.getFils1().getFils3()),a.getNumLigne());
	   		verifier_LISTE_INST(a.getFils2());
	   		break;
	   	case TantQue:
	   		t1=verifier_EXP(a.getFils1());
	   		if(!t1.equals(Type.Boolean)) {
	   			ErreurContext.ErreurTypeWhile.leverErreurContext("", a.getNumLigne());
	   		}
	   		verifier_LISTE_INST(a.getFils2());
	   		break;
	   	case Si:
	   		t1=verifier_EXP(a.getFils1());
	   		if(!t1.equals(Type.Boolean)) {
	   			ErreurContext.ErreurTypeIf.leverErreurContext("", a.getNumLigne());
	   		}
	   		verifier_LISTE_INST(a.getFils2());
	   		verifier_LISTE_INST(a.getFils3());
	   		break;
	   	case Lecture:
	   		t1=verifier_PLACE(a.getFils1());
	   		if(!(t1.getNature()==NatureType.Real||t1.getNature()==NatureType.Interval)) {
	   			ErreurContext.ErreurTypeRead.leverErreurContext("", a.getNumLigne());
	   		}
	   		break;
	   	case Ecriture:
	   		verifier_LISTE_EXP(a.getFils1());
	   		break;
	   	case Ligne:
	   		break;
	   	default:
	   		throw new ErreurInterneVerif("Arbre incorrect dans verifier_INST");
	   }
   }
   
   private void verifier_LISTE_EXP(Arbre a) throws ErreurVerif{
	   switch(a.getNoeud()) {
	   	case ListeExp:
	   		verifier_LISTE_EXP(a.getFils1());
	   		Type t1=verifier_EXP(a.getFils2());
	   		if(!(t1.getNature()==NatureType.Real || t1.getNature()==NatureType.Interval || t1.getNature()==NatureType.String)) {
	   			ErreurContext.ErreurTypeWrite.leverErreurContext("", a.getNumLigne());
	   		}
	   		break;
	   	case Vide:
	   		break;
	   	default:
	   		throw new ErreurInterneVerif(
	   				"Arbre incorrect dans verifier_LISTE_INST");
	   }
   }
   
   private Type verifier_PLACE(Arbre a) throws ErreurVerif{
	   Type t1;
	   Type t2;
	   switch(a.getNoeud()) {
	   	case Index:
	   		t1=verifier_PLACE(a.getFils1());
	   		t2=verifier_EXP(a.getFils2());
	   		ResultatBinaireCompatible res = ReglesTypage.binaireCompatible(Noeud.Index, t1, t2,a.getNumLigne());
	   		Decor decor = new Decor(res.getTypeRes());
	   		
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   	case Ident:
	   		t1=verifier_IDF_Inst(a);
	   		return t1;
	   	default:
	   		throw new ErreurInterneVerif("Arbre incorrect dans verifier_PLACE");
	   }
   }
   
   private Type verifier_IDF_Inst(Arbre a) throws ErreurVerif{
	   Defn def = env.chercher(a.getChaine());
	   if(def==null) {
		   ErreurContext.ErreurIdentificateurNonDeclare.leverErreurContext(a.getChaine(), a.getNumLigne());
	   }
	   if(def.getGenre()==Genre.PredefBoolean || def.getGenre()==Genre.PredefInteger || def.getGenre()==Genre.PredefReal) {
		   ErreurContext.ErreurAffectationInvalide.leverErreurContext(a.getChaine(), a.getNumLigne());
	   }
	   Decor decor = new Decor(def);
	   decor.setInfoCode(1);
	   a.setDecor(decor);
	   return def.getType();
   }
   
   private Type verifier_EXP(Arbre a) throws ErreurVerif{
	   Type t1;
	   Type t2;
	   ResultatBinaireCompatible res;
	   ResultatUnaireCompatible res1;
	   Decor decor;
	   switch(a.getNoeud()) {
	   	case Vide:
	   		break;
	   	case Plus:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());   		
	   		res = ReglesTypage.binaireCompatible(Noeud.Plus, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	   a.setDecor(decor);
		 	   
	   		return res.getTypeRes();
	   		
	   	case Moins:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Moins, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	   a.setDecor(decor);
	   		return res.getTypeRes();
	   	
	   	case Et:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Et, t1, t2,a.getNumLigne());
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case Ou:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Ou, t1, t2,a.getNumLigne());
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case Egal:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Egal, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   	
	   	case NonEgal:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.NonEgal, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case InfEgal:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.InfEgal, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   	
	   	case Inf:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Inf, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case SupEgal:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.SupEgal, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case Sup:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Sup, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case DivReel:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.DivReel, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case Quotient:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Quotient, t1, t2,a.getNumLigne());
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   	
	   	case Mult:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Mult, t1, t2,a.getNumLigne());
	   		if(res.getConv2()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils2(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils2(conv);
	   		}
	   		if(res.getConv1()) {
	   			Arbre conv = Arbre.creation1(Noeud.Conversion,a.getFils1(), a.getNumLigne());
	   			decor = new Decor(t1);
		 	 	decor.setInfoCode((conv.getFils1().getDecor().getInfoCode()));
		 	 	conv.setDecor(decor);
	   			a.setFils1(conv);
	   		}
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case Reste:
	   		t1 = verifier_EXP(a.getFils1());
	   		t2 = verifier_EXP(a.getFils2());
	   		res = ReglesTypage.binaireCompatible(Noeud.Reste, t1, t2,a.getNumLigne());
	   		decor = new Decor(res.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode()+ a.getFils2().getDecor().getInfoCode() +1);
		 	a.setDecor(decor);
	   		return res.getTypeRes();
	   		
	   	case Index:
	   		t1= verifier_PLACE(a);
	   		return t1;
	   		
	   	case MoinsUnaire:
	   		t1 = verifier_EXP(a.getFils1());
	   		res1 = ReglesTypage.unaireCompatible(Noeud.MoinsUnaire,t1,a.getNumLigne());
	   		decor = new Decor(res1.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode());
		 	a.setDecor(decor);
	   		return res1.getTypeRes();
	   	
	   	case PlusUnaire:
	   		t1 = verifier_EXP(a.getFils1());
	   		res1 = ReglesTypage.unaireCompatible(Noeud.PlusUnaire,t1,a.getNumLigne());
	   		decor = new Decor(res1.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode());
		 	a.setDecor(decor);
	   		return res1.getTypeRes();
	   		
	   	case Non:
	   		t1 = verifier_EXP(a.getFils1());
	   		res1 = ReglesTypage.unaireCompatible(Noeud.Non,t1,a.getNumLigne());
	   		decor = new Decor(res1.getTypeRes());
	   		decor.setInfoCode(a.getFils1().getDecor().getInfoCode());
		 	a.setDecor(decor);
	   		return res1.getTypeRes();
	   
	   	case Ident:
	   		t1=verifier_IDF_Inst(a);
	   		return t1;
	   		
	   	case Chaine:
	   		decor = new Decor(Type.String);
	   		decor.setInfoCode(1);
		 	   a.setDecor(decor);
	   		return Type.String;
	   	case Entier:
	   		decor = new Decor(Type.Integer);
	   		decor.setInfoCode(1);
		 	   a.setDecor(decor);
	   		return Type.Integer;
	   	case Reel:
	   		decor = new Decor(Type.Real);
	   		decor.setInfoCode(1);
		 	   a.setDecor(decor);
	   		return Type.Real;
	   	default:
	   		throw new ErreurInterneVerif("Arbre incorrect dans verifier_EXP");
	   }
	return Type.Boolean;
	   
	   
   }
   // ------------------------------------------------------------------------
   // COMPLETER les operations de vérifications et de décoration pour toutes 
   // les constructions d'arbres
   // ------------------------------------------------------------------------

}
