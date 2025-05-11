package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class CurrentSelectedCoalition implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1891334253780920295L;
	private boolean activate;
	private Coalition coalition;
	private String tresor_location;
	public CurrentSelectedCoalition (/*String pos, Coalition c*/) {
		/*this.tresor_location = pos;
		this.coalition = c;
		this.activate = false;*/
	}
	public Coalition getCoalition() {
		return coalition;
	}
	public void setCoalition(Coalition coalition) {
		this.coalition = coalition;
	}
	public boolean isActive() {
		return activate;
	}
	public void setActive(boolean exist) {
		this.activate = exist;
	}
	public void setTresorLocation(String loc) {
		this.tresor_location = loc;
	}
	public String getTresorLocation() {
		return this.tresor_location;
	}
	
}
