/**
 * Type énuméré pour les erreurs contextuelles.
 * Ce type énuméré définit toutes les erreurs contextuelles possibles et 
 * permet l'affichage des messages d'erreurs pour la passe 2.
 */

// -------------------------------------------------------------------------
// A COMPLETER, avec les différents types d'erreur et les messages d'erreurs 
// correspondants
// -------------------------------------------------------------------------
//ErreurContext.ErreurAffectationInvalide.leverErreurContext("kikoo", 506984); utilisation
package fr.esisar.compilation.verif;

public enum ErreurContext 
{
   
   ErreurNonRepertoriee,
   ErreurAffectationInvalide,
   ErreurTypeInconnu,
   ErreurIdentificateurNonDeclare,
   ErreurIdentificateurDejaDeclare,
   ;
   
   

   void leverErreurContext(String s, int numLigne) throws ErreurVerif 
   {
      System.err.println("Erreur contextuelle : ");
      switch (this) 
      {
      	case ErreurAffectationInvalide:
      		System.err.println("Affectation invalide " + s);
      		break;
      	case ErreurTypeInconnu:
      		System.err.println("Type Inconnu " + s);
      		break;
      	case ErreurIdentificateurNonDeclare:
      		System.err.println("Variable non declaree " + s);
      		break;
      	case ErreurIdentificateurDejaDeclare:
      		System.err.println("Identificateur deja declare" + s);
         default:
            System.err.print("Non repertoriee");
      }
      System.err.println(" ... ligne " + numLigne);
      throw new ErreurVerif();
   }

}


