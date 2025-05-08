package eu.su.mas.dedaleEtu.mas.knowledge;

public class AgentInfo {
	int locking;
	int strenght;
	String Name;
	public AgentInfo(int locking, int strenght) {
		this.locking = locking;
		this.strenght = strenght;
	}
	public int getLocking() {
		return this.locking;
	}
	public int getStrenght() {
		return this.strenght;
	}
	public String getNames() {
		return this.Name;
	}
}