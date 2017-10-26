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
   ErreurOperationInvalide,
   ErreurTypeInconnu,
   ErreurIdentificateurNonDeclare,
   ErreurIdentificateurDejaDeclare,
   ErreurTypeWrite,
   ErreurTypeWhile,
   ErreurTypeIf,
   ErreurTypeRead,
   ErreurTypeForVariable,
   ErreurTypeForBorne,
   ;
   
   

   void leverErreurContext(String s, int numLigne) throws ErreurVerif 
   {
      System.err.println("########################################## \nErreur contextuelle : ");
      switch (this) 
      {
      	case ErreurAffectationInvalide:
      		System.err.println("Affectation invalide : " + s);
      		break;
      	case ErreurTypeForVariable:
      		System.err.println("For invalide, la variable de contrôle doit être de type integer");
      		break;
      	case ErreurTypeForBorne:
      		System.err.println("For invalide, les bornes doivent être de type integer");
      		break;
      	case ErreurOperationInvalide:
      		System.err.println("Operation invalide : " + s);
      		break;
      	case ErreurTypeInconnu:
      		System.err.println("Type Inconnu : " + s);
      		break;
      	case ErreurIdentificateurNonDeclare:
      		System.err.println("Variable non declaree : " + s);
      		break;
      	case ErreurIdentificateurDejaDeclare:
      		System.err.println("Identificateur deja declare : " + s);
      		break;
      	case ErreurTypeRead:
      		System.err.println("Type non supporté dans l'instruction read : seuls le types integer et real sont supportés");
      		break;
      	case ErreurTypeWrite:
      		System.err.println("Type non supporté dans l'instruction write : seuls les types integer, real et string sont supportés");
      		break;
      	case ErreurTypeWhile:
      		System.err.println("L'expression suivant Tant que n'est pas de type boolean");
      		break;
      	case ErreurTypeIf:
      		System.err.println("L'expression suivant Si n'est pas de type boolean");
      		break;
         default:
            System.err.print("Non repertoriee");
            break;
      }
      System.err.println(" ... ligne " + numLigne);
      throw new ErreurVerif();
   }

}


