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
	   		throw new ErreurInterneVerif(
	   				"Arbre incorrect dans verifier_LISTE_DECL");
	   }
	   
   }
   
   /**************************************************************************
    * DECL
    **************************************************************************/
   
   private void verifier_DECL(Arbre a) throws ErreurVerif{
	   // A COMPLETER

	   
	   verifier_LISTE_IDF_Decl(a.getFils1(),verifier_TYPE(a.getFils2()));
   }
   
   /**************************************************************************
    * LISTE_INST
    **************************************************************************/
   private void verifier_LISTE_INST(Arbre a) throws ErreurVerif {
      // A COMPLETER
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
	   		throw new ErreurInterneVerif(
	   				"Arbre incorrect dans verifier_LISTE_DECL");
	   }
   }
   
   private void verifier_IDF_Decl(Arbre a, Type type) throws ErreurVerif{
	   Defn def = env.chercher(a.getChaine());
	   if(def!=null) {
		   throw new ErreurVerif();
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
	   		break;
	   }
	   return type;
   }
   
   
   private Type verifier_IDF_Type(Arbre a) throws ErreurVerif{
	   Defn def = env.chercher(a.getChaine());
	   if(def==null) {
		   throw new ErreurVerif();
	   }
	   Decor decor = new Decor(def);
	   a.setDecor(decor);
	   return def.getType();
   }
   // ------------------------------------------------------------------------
   // COMPLETER les operations de vérifications et de décoration pour toutes 
   // les constructions d'arbres
   // ------------------------------------------------------------------------

}
