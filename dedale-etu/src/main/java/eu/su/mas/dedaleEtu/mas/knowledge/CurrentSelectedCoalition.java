package eu.su.mas.dedaleEtu.mas.knowledge;

public class CurrentSelectedCoalition {
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
