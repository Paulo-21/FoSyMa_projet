package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class BooleanWrapper  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -665213584580620494L;
	public boolean value;
	public BooleanWrapper() {
		this.value = false;
	}
	public BooleanWrapper(boolean v) {
		this.value = v;
	}
	public void set(boolean v) {
		this.value = v;
	}
	public boolean get() {
		return this.value;
	}
}
