package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class SharedMapRepresentation implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3416821454422313794L;
	private MapRepresentation myMap;
	public SharedMapRepresentation () {
		
	}
	public MapRepresentation getMyMap() {
		if(this.myMap==null) {
			this.myMap= new MapRepresentation();
		}
		return this.myMap;
	}
}
