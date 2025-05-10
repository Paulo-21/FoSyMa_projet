package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


public class Coalition implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6713739261135933643L;
	boolean confirmed;
	int tot_locking;
	int tot_strenght;
	ArrayList<String> agentsNames;
	public Coalition(ArrayList<String> agentsNames, HashMap<String, AgentInfo> know_agent) {
		this.agentsNames = agentsNames;
		this.tot_locking = 0;
		this.tot_strenght = 0;
		if(this.agentsNames.size() == 1) {
			this.confirmed = true;
		}
		for (String name : agentsNames) {
			AgentInfo info = know_agent.get(name);
			this.tot_locking += info.getLocking();
			this.tot_strenght += info.getStrenght();
		}
	}
	
	public int getSurplusCap(Tresor t) {
		return this.tot_locking - t.getLocking() + this.tot_strenght - t.getStrenght();
	}
	public int getLocking() {
		return this.tot_locking;
	}
	public int getStrenght() {
		return this.tot_strenght;
	}
	public int getCoalitionSize() {
		return this.agentsNames.size();
	}
	public boolean isConfirmed() {
		return this.confirmed;
	}
}
