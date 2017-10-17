package fr.esisar.compilation.verif;

/**
 * Exception levée en cas d'erreur interne dans ReglesTypage.
 */

public class ErreurReglesTypage extends RuntimeException 
{
	public ErreurReglesTypage(String s) {
	      super("===========================================================\n" + 
	            "                ERREUR INTERNE ReglesTypage                \n" + 
	            "===========================================================\n" + 
	            s + "\n" + 
	            "===========================================================\n");
	   }

}

