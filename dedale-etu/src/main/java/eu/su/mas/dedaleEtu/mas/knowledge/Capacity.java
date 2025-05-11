package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.Agent;

public class Capacity implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3595741522851796531L;
	private int locking;
	private int strenght; 
	private int gold;
	private int diamond;
	private AbstractDedaleAgent myAgent;
	public Capacity(Agent agent) {
		AbstractDedaleAgent myAgent = (AbstractDedaleAgent) agent;
		Set<Couple<Observation, Integer>> exp = myAgent.getMyExpertise();
		this.myAgent = myAgent;
		for (Couple<Observation, Integer> c : exp) {
			Observation l = c.getLeft();
			if (l == Observation.LOCKPICKING) {
				this.setLocking(c.getRight());
			}
			else if (l == Observation.STRENGH) {
				this.setStrenght(c.getRight());
			}
		}
	}

	public int getStrenght() {
		return strenght;
	}

	public void setStrenght(int strenght) {
		this.strenght = strenght;
	}
	public void update_capa() {
		for (Couple<Observation, Integer> c : myAgent.getBackPackFreeSpace()) {
			if(c.getLeft() == Observation.GOLD) {
				this.gold = c.getRight();
			}
			else if(c.getLeft() == Observation.DIAMOND) {
				this.diamond = c.getRight();
			}
		}
	}
	public int getDiamond() {
		this.update_capa();
		return this.diamond;
	}

	public void setDiamond(int diamond) {
		this.diamond = diamond;
	}

	public int getGold() {
		this.update_capa();
		return gold;
	}

	public void setGold(int gold) {
		this.gold = gold;
	}

	public int getLocking() {
		return locking;
	}

	public void setLocking(int locking) {
		this.locking = locking;
	}
	public void print() {
		System.out.println("Cap : "+locking+" "+strenght+ " "+this.getGold()+" " +this.getDiamond());
	}
}

/*public Capacity getCapacity(Set<Couple<Observation,Integer>> caps) {
int lock = 0;
int strenght = 0;
for (Couple<Observation, Integer> c : caps) {
	Observation obs = c.getLeft(); 
	if (obs == Observation.LOCKPICKING) {
		lock = c.getRight();
	} else if ( obs == Observation.STRENGH){
		strenght = c.getRight();
	}
}
Capacity cap = new Capacity(lock, strenght);
return cap;
}*/
