package fr.esisar.compilation.gencode;
import fr.esisar.compilation.global.src3.Operande;
public class Operande2{

		private String Id;
		private Operande operande;
		private int emplacement;
		public int time;
		
		
		public Operande2(String Id,Operande operande,int emplacement) {
			this.Id=Id;
			this.operande=operande;
			this.emplacement=emplacement;
			this.time=0;
			
		}
		
		public int getEmplacement() {
			return this.emplacement;
		}
	
		public String getId() {
			return this.Id;
		}
		
		public Operande getOperande() {
			return this.getOperande();
		}
		
		
}
