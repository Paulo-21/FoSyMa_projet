package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class Tresor implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6886905853940252551L;
	private int locking;
	private int strenght;
	public Tresor(int locking, int strenght) {
		this.locking = locking;
		this.strenght = strenght;
	}
	public int getStrenght() {
		return this.strenght;
	}
	public int getLocking() {
		return this.locking;
	}
}
