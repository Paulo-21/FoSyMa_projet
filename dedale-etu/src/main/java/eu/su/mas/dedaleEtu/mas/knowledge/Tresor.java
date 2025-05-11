package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

import jade.core.Agent;

public class Tresor implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6886905853940252551L;
	private int locking;
	private int strenght;
	private int gold;
	private int diamond;
	private boolean isOpen;
	
	public Tresor(int locking, int strenght) {
		this.locking = locking;
		this.strenght = strenght;
	}
	public Tresor(int locking, int strenght, int gold, int diamond) {
		this.locking = locking;
		this.strenght = strenght;
		this.gold = gold;
		this.diamond = diamond;
		this.isOpen = false;
	}
	public Tresor(int locking, int strenght, int gold, int diamond, boolean open) {
		this.locking = locking;
		this.strenght = strenght;
		this.gold = gold;
		this.diamond = diamond;
		this.isOpen = open;
	}
	public int getStrenght() {
		return this.strenght;
	}
	public int getLocking() {
		return this.locking;
	}
	public int getGold() {
		return gold;
	}
	public void setGold(int gold) {
		this.gold = gold;
	}
	public int getDiamond() {
		return diamond;
	}
	public void setDiamond(int diamond) {
		this.diamond = diamond;
	}
	public boolean isOpen() {
		return this.isOpen;
	}
	public void setIsOpen(boolean open) {
		this.isOpen = open;
	}
	public boolean is_capable(Capacity cap) {
		if( (cap.getLocking() >= this.locking || isOpen) && cap.getStrenght() >= this.strenght && (cap.getGold()*this.gold >0 || 
				this.getDiamond()*this.getDiamond() > 0 )) {
				System.out.println("Capable !");
			return true;
		}
		return false;
	}
	public void print() {
		System.out.println("Tresor : "+this.gold + " "+this.diamond+" "+this.isOpen+" "+this.locking+ " "+this.strenght);
	}
}
