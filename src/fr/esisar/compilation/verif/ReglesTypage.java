package fr.esisar.compilation.verif;

import fr.esisar.compilation.global.src.*;

/**
 * La classe ReglesTypage permet de définir les différentes règles 
 * de typage du langage JCas.
 */

public class ReglesTypage  {

   /**
    * Teste si le type t1 et le type t2 sont compatibles pour l'affectation, 
    * c'est à dire si on peut affecter un objet de t2 à un objet de type t1.
    */

   static ResultatAffectCompatible affectCompatible(Type t1, Type t2,int Numligne) throws ErreurReglesTypage,ErreurVerif{
	   boolean ok = false;
	   boolean conv2 = false;
	   
	  if(!(t1.getNature() == NatureType.Array) && t1.getNature()==t2.getNature()) {
		   	ok=true;
		   }
	  else {
	  switch(t1.getNature()) {
	  	
		  case Real:
			  
			   switch (t2.getNature()) {
				   case Interval:
					   ok=true;
					   	conv2=true;
					   	break;
				   default : throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"affectation");
			   }
			   break;
			   
		  case Array:
			  
			   switch(t2.getNature()) { 
				   case  Array:
					   System.out.println("BITT"+ t1.getIndice().getNature());
					   if(t1.getIndice().getNature()==NatureType.Interval && t1.getIndice().getNature()==t2.getIndice().getNature() && t1.getIndice().getBorneInf()== t2.getIndice().getBorneInf() && t2.getIndice().getBorneSup() == t2.getIndice().getBorneSup()) {
						   ResultatAffectCompatible res1 =affectCompatible(t1.getElement(),t2.getElement(),Numligne);
						   
						   if(res1.getOk())
								   ok = true;
						   if(res1.getConv2())
							   	    conv2=true;
					   }
					   else {
						   throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"affectation");}
					   break;
				   default : 
					   throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"affectation");
				}
			   break;
		 default: //System.out.println("coucou");
			 throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"affectation");
			 
	  }
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
      (Noeud noeud, Type t1, Type t2,int Numligne) throws ErreurReglesTypage,ErreurVerif{
	   Boolean conv1=false;
	   Boolean conv2=false;
	   Boolean ok = false;
	   Type TypeRes=Type.Boolean; //ATTENTION INITIALISATION NON SURE
	   //System.out.println("Bonjour  " +t1.getNature()+ t2.getNature());
	   switch(noeud) {
	   	  case Ou:
	   	  case Et:
	   		  if(t1.getNature()==NatureType.Boolean && t2.getNature()==NatureType.Boolean) {
	   			  ok=true;
	   			  TypeRes=Type.Boolean;
	   		  }
	   		  else
	   			throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération "+noeud.toString());
	   		  break;
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
							   ok=true;
							   conv1=true;
							   TypeRes=Type.Boolean;
							   break;
						   default:
							   throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération "+noeud.toString());
					   }
					   break;
				   case Real:
					   switch(t2.getNature()) {
						   case Interval :
							   ok=true;
							   conv2=true;
							   TypeRes=Type.Boolean;
							   break;
						   case Real:
							   ok=true;
							   TypeRes=Type.Boolean;
							   break;
							default :
								throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération "+noeud.toString());
					   }
					   break;
				   default:
					   throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération "+noeud.toString());
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
							   ok=true;
							   conv2=true;
							   TypeRes=Type.Real;
							   break;
						   default :
							   throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération "+noeud.toString());
					   }
					   break;
				   case Interval:
					   switch(t2.getNature()) {
						   case Real:
							   ok=true;
							   conv1=true;
							   TypeRes=Type.Real;
							   break;
						   case Interval:
							   ok=true;
							   TypeRes=Type.Integer;
							   break;
							default:
								throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération "+noeud.toString());
					   }
					   break;
					default:
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
							 ok=true;
							 conv1=true;
							 TypeRes=Type.Real;
						 default:
							 throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération binaire");
					 }
					 break;
				  case Real:
					  switch(t2.getNature()) {
						  case Interval:
							    ok=true;
							  	conv2=true;
							  	TypeRes=Type.Real;
							  	break;
						  case Real:
							     ok=true;
							     TypeRes=Type.Real;
							     break;
						   default:
							   throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération binaire");
							   
					  }
					  break;
				  default :
					  throw new ErreurReglesTypage(t1.getNature().toString(),t2.getNature().toString(),Numligne,"opération binaire");
				     
			  }
			  break;
		   case Index :
				 if(t1.getNature()==NatureType.Array && t1.getIndice().getNature()==NatureType.Interval && t2.getNature()==NatureType.Interval) {
					 ok=true;
					 TypeRes=t1.getElement();
			     }
				 break;
		  
		   default : // Inserer une exception adaptée
			   throw new ErreurVerif();
			   	
		   }
	   ResultatBinaireCompatible Bin= new ResultatBinaireCompatible();
	   Bin.setConv1(conv1);
	   Bin.setConv2(conv2);
	   Bin.setOk(ok);
	   Bin.setTypeRes(TypeRes);
	   return Bin;  
   }

   /**
    * Teste si le type t est compatible pour l'opération binaire représentée 
    * dans noeud.
    */
   static ResultatBinaireCompatible VerifFor // Permet de vérifier les opérations de décrémentation et incrémentation
   (Noeud noeud,Type t1,Type t2, Type t3,int Numligne) throws ErreurReglesTypage,ErreurVerif{
	   Boolean ok = false;
	   Boolean conv1=false;
	   Boolean conv2=false;

	   Type TypeRes=Type.Boolean;
	   switch(noeud) {
	   		case Increment:
	   		case Decrement:
	   			if (t1.getNature()==NatureType.Interval && t1.getNature()==t2.getNature() && t2.getNature()==t3.getNature()) {
	   				ok=true;
	   				TypeRes=Type.Integer;
	   			}
	   			else {
	   				if(!(t1.getNature()==NatureType.Interval))
	   					throw new ErreurReglesTypage(t1.getNature().toString(),Numligne,1);
	   				else
	   					throw new ErreurReglesTypage(t1.getNature().toString(),Numligne,2);
	   			}
	   			break;
	   		default:
	   			break;
	   }
	   ResultatBinaireCompatible Bin = new ResultatBinaireCompatible();
	   Bin.setConv1(conv1);
	   Bin.setConv2(conv2);
	   Bin.setOk(ok);
	   return Bin;
	   
   }
   static ResultatUnaireCompatible unaireCompatible
         (Noeud noeud, Type t,int Numligne) throws ErreurReglesTypage,ErreurVerif{
	    Boolean ok = false;
	    Type TypeRes=Type.Boolean;
	    switch (noeud) {
		    case Non:
		    	if(t.getNature()==NatureType.Boolean) {
		    		ok=true;
		    		TypeRes=Type.Boolean;
		    	}
		    	else {
		    		throw new ErreurReglesTypage(t.getNature().toString(),Numligne,"Non");
		    	}
		    	break;
		    case MoinsUnaire :
		    case PlusUnaire :
		    	switch(t.getNature()) {
		    	case Real :
		    		ok =true;
		    		TypeRes=Type.Real;
		    		break;
		    	case Interval :
		    		ok=true;
		    		TypeRes=Type.Integer;
		    		break;
		    	default :
		    		throw new ErreurReglesTypage(t.getNature().toString(),Numligne,noeud.toString());
		    		
		    	}
		    	break;
		    
		    default:
		    	break;
	    }
	    ResultatUnaireCompatible Unaire = new ResultatUnaireCompatible();
	    Unaire.setOk(ok);
	    Unaire.setTypeRes(TypeRes);
	    return Unaire;
  
   }
         
}

