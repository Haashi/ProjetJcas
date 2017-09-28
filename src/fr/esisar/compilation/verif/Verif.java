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
   }
   
   /**************************************************************************
    * LISTE_INST
    **************************************************************************/
   private void verifier_LISTE_INST(Arbre a) throws ErreurVerif {
      // A COMPLETER
   }
   
   /**************************************************************************
    * INST
    **************************************************************************/
   
   private void verifier_INST(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * LISTE_IDF
    **************************************************************************/
   
   private void verifier_LIST_IDF(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * IDF
    **************************************************************************/
   
   private void verifier_IDF(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * TYPE
    **************************************************************************/
   
   private void verifier_TYPE(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * TYPE_INTERVALLE
    **************************************************************************/
   
   private void verifier_TYPE_INTERVALLE(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * CONSTANTE
    **************************************************************************/
   
   private void verifier_CONSTANTE(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * CONST
    **************************************************************************/
   
   private void verifier_CONST(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * PAS
    **************************************************************************/
   
   private void verifier_PAS(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * LISTE_EXP
    **************************************************************************/
   
   private void verifier_LISTE_EXP(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * EXP
    **************************************************************************/
   
   private void verifier_EXP(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * FACTEUR
    **************************************************************************/
   
   private void verifier_FACTEUR(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   
   /**************************************************************************
    * PLACE
    **************************************************************************/
   
   private void verifier_PLACE(Arbre a) throws ErreurVerif{
	   // A COMPLETER
   }
   // ------------------------------------------------------------------------
   // COMPLETER les operations de vérifications et de décoration pour toutes 
   // les constructions d'arbres
   // ------------------------------------------------------------------------

}
