/**
 * Type énuméré pour les erreurs contextuelles.
 * Ce type énuméré définit toutes les erreurs contextuelles possibles et 
 * permet l'affichage des messages d'erreurs pour la passe 2.
 */

// -------------------------------------------------------------------------
// A COMPLETER, avec les différents types d'erreur et les messages d'erreurs 
// correspondants
// -------------------------------------------------------------------------
//ErreurContext.ErreurOperationInvalide.leverErreurContext("kikoo", 506984); utilisation
package fr.esisar.compilation.verif;

public enum ErreurContext 
{
   
   ErreurNonRepertoriee,
   ErreurOperationInvalide,
   ErreurMauvaiseDeclaration,
   ErreurIdentificateurNonDeclare;
   
   

   void leverErreurContext(String s, int numLigne) throws ErreurVerif 
   {
      System.err.println("Erreur contextuelle : ");
      switch (this) 
      {
      	case ErreurOperationInvalide:
      		System.err.print("Operation invalide " + s);
      		break;
      	case ErreurMauvaiseDeclaration:
      		System.err.println("Declaration incorrecte " + s);
      		break;
      	case ErreurIdentificateurNonDeclare:
      		System.err.print("Variable non declare " + s);
      		break;
         default:
            System.err.print("Non repertoriee");
      }
      System.err.println(" ... ligne " + numLigne);
      throw new ErreurVerif();
   }

}


