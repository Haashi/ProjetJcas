package fr.esisar.compilation.verif;

/**
 * Exception levée en cas d'erreur interne dans ReglesTypage.
 */

public class ErreurReglesTypage extends RuntimeException 
{
	public ErreurReglesTypage(String t1,int Nbligne,int mode) throws ErreurVerif {
		switch(mode) {
		case 1:
			System.out.println("Erreur de Typage ligne "+ Nbligne +"  la variable de controle est de type "+ t1 +" alors que le type Interval est attendu");
			throw new ErreurVerif();
		case 2 :
			System.out.println("Erreur de Typage ligne " + Nbligne + " les deux bornes doivent etre de type interval" );
			throw new ErreurVerif();
	}
	}
	
	public ErreurReglesTypage(String t1,String t2,int Nbligne,String mode) throws ErreurVerif {
		System.out.println("Erreur de Typage ligne "+ Nbligne+" : "+ "Lors d'une " + mode +  " le type " + t1 + " est incompatible avec le type " + t2 );
		throw new ErreurVerif();
	}
	public ErreurReglesTypage(String t, int Nbligne,String mode) throws ErreurVerif {
		System.out.println("Erreur de Typage ligne "+ Nbligne+" : "+ "Lors de l'opération " + mode +  " le type " + t+ " n'est pas attendu ici" );
		throw new ErreurVerif();
	}

}

