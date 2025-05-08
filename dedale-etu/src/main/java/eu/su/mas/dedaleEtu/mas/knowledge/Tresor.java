package eu.su.mas.dedaleEtu.mas.knowledge;

public class Tresor {
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
