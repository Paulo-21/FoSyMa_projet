package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class CurrentSelectedCoalition implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1891334253780920295L;
	private boolean exist;
	private Coalition coalition;
	public CurrentSelectedCoalition () {
		
	}
	public Coalition getCoalition() {
		return coalition;
	}
	public void setCoalition(Coalition coalition) {
		this.coalition = coalition;
	}
	public boolean isExist() {
		return exist;
	}
	public void setExist(boolean exist) {
		this.exist = exist;
	}
	
}
