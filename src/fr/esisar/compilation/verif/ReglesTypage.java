package fr.esisar.compilation.verif;

import fr.esisar.compilation.global.src.*;

/**
 * La classe ReglesTypage permet de définir les différentes règles 
 * de typage du langage JCas.
 */

public class ReglesTypage {

   /**
    * Teste si le type t1 et le type t2 sont compatibles pour l'affectation, 
    * c'est à dire si on peut affecter un objet de t2 à un objet de type t1.
    */

   static ResultatAffectCompatible affectCompatible(Type t1, Type t2) {
	   boolean ok = false;
	   boolean conv2 = false;
	   
	  if(t1.getNature()==t2.getNature()) {
		   	ok=true;
		   }
	  switch(t1.getNature()) {
	  
		  case Interval:
			  
			   switch (t2.getNature()) {
				   case Real:
					   	conv2=true;
					   	break;
				   default : break;}
			   break;
			   
		  case Array:
			  
			   switch(t2.getNature()) {
			   
				   case  Array:
					   if(t1.getIndice().getNature()==NatureType.Interval && t1.getIndice()==t2.getIndice() && t1.getBorneInf()== t2.getBorneInf() && t2.getBorneSup() == t2.getBorneSup()) {
						   if(affectCompatible(t1.getElement(),t2.getElement()).getOk()==true)
								   ok = true;
					   }
					   break;
				   default : 
						break;}
		 default:
			 break;
	  }
	  ResultatAffectCompatible Affect = new ResultatAffectCompatible();
	  Affect.setOk(ok);
	  Affect.setConv2(conv2);
	  return Affect;							 					  		   
 // A MODIFER
   }

   /**
    * Teste si le type t1 et le type t2 sont compatible pour l'opération 
    * binaire représentée dans noeud.
    */

   static ResultatBinaireCompatible binaireCompatible
      (Noeud noeud, Type t1, Type t2) {
	   Boolean conv1=false;
	   Boolean conv2=false;
	   Boolean ok = false;
	   Type TypeRes;
	   switch(noeud) {
	   	  case Ou:
	   	  case Et:
	   		  if(t1.getNature()==NatureType.Boolean && t2.getNature()==NatureType.Boolean) {
	   			  ok=true;
	   			  TypeRes=Type.Boolean;
	   		  }
		   case Egal:
		   case Inf:
		   case Sup:
		   case InfEgal:
		   case SupEgal:
		   case NonEgal: 
			   switch(t1.getNature()) {
				   case Interval :
					   switch (t2.getNature()) {
						   case Interval:
							   ok=true;
							   TypeRes=Type.Boolean;
							   break;
						   case Real:
							   conv1=true;
							   TypeRes=Type.Boolean;
							   break;
						   default:
							   break;
					   }
					   break;
				   case Real:
					   switch(t2.getNature()) {
						   case Interval :
							   conv2=true;
							   TypeRes=Type.Boolean;
							   break;
						   case Real:
							   ok=true;
							   TypeRes=Type.Boolean;
							   break;
							default :
								break;
					   }
					   break;
				   default:
					   break;
			   }
			   break;
		   case Plus:
		   case Moins:
		   case Mult :
			   switch(t1.getNature()) {
				   case Real:
					   switch (t2.getNature()) {
						   case Real:
							  ok=true;
							  TypeRes=Type.Real;
							  break;
						   case Interval:
							   conv2=true;
							   TypeRes=Type.Real;
							   break;
						   default :
							   break;
					   }
					   break;
				   case Interval:
					   switch(t2.getNature()) {
						   case Real:
							   conv1=true;
							   TypeRes=Type.Real;
							   break;
						   case Interval:
							   ok=true;
							   TypeRes=Type.Integer;
							   break;
					   }
					   break;
			   }
			   break;
		   case Quotient :
		   case Reste :
			   if(t1.getNature()==NatureType.Interval && t2.getNature()==NatureType.Interval) {
				   ok=true;
				   TypeRes=Type.Integer;
			   }
			   break;
		   case DivReel :
			  switch(t1.getNature()) {
				  case Interval :
					 switch(t2.getNature()) {
						 case Interval:
							 ok=true;
							 TypeRes=Type.Real;
							 break;
						 case Real:
							 conv1=true;
							 TypeRes=Type.Real;
						 default:
							break;
					 }
					 break;
				  case Real:
					  switch(t2.getNature()) {
						  case Interval:
							  	conv2=true;
							  	TypeRes=Type.Real;
							  	break;
						  case Real:
							     ok=true;
							     TypeRes=Type.Real;
							     break;
						   default:
							   break;
					  }
					  break;
				  default :
				     break;
			  }
			  break;
		   case Tableau :
				 if(t1.getNature()==NatureType.Array && t1.getIndice().getNature()==NatureType.Interval && t2.getNature()==NatureType.Interval) {
					 ok=true;
					 TypeRes=t1.getElement();
			     }
				 break;
		   default : 
			   	return null;
		   }
	      
   }

   /**
    * Teste si le type t est compatible pour l'opération binaire représentée 
    * dans noeud.
    */
   static ResultatUnaireCompatible unaireCompatible
         (Noeud noeud, Type t) {
      return null;
   }
         
}

