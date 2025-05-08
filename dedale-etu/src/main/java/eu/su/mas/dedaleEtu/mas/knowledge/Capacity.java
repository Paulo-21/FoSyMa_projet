package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.Agent;

public class Capacity {
	private int locking;
	private int strenght; 
	private int gold;
	private int diamond;
	
	public Capacity(Agent agent) {
		AbstractDedaleAgent myAgent = (AbstractDedaleAgent) agent;
		Set<Couple<Observation, Integer>> exp = myAgent.getMyExpertise();
		for (Couple<Observation, Integer> c : exp) {
			Observation l = c.getLeft();
			if (l == Observation.LOCKPICKING) {
				this.setLocking(c.getRight());
			}
			else if (l == Observation.STRENGH) {
				this.setStrenght(c.getRight());
			}
			else if (l == Observation.GOLD) {
				this.setGold(c.getRight());
			}
			else if (l == Observation.DIAMOND) {
				this.setDiamond(c.getRight());
			}
		}
	}

	public int getStrenght() {
		return strenght;
	}

	public void setStrenght(int strenght) {
		this.strenght = strenght;
	}

	public int getDiamond() {
		return diamond;
	}

	public void setDiamond(int diamond) {
		this.diamond = diamond;
	}

	public int getGold() {
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
}
