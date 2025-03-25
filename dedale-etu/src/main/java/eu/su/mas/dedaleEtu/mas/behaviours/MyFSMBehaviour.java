package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.behaviours.DataStore;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.SharedMapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * This FSM behavior manages the agent's cooperative exploration in a structured manner.
 * It includes observation, communication, and exploration as separate states.
 * The goal is to explore the environment efficiently while sharing knowledge with other agents.
 */
public class MyFSMBehaviour extends FSMBehaviour {

    private static final long serialVersionUID = 1L; //not sure what it is
    
    //Deceleration of three states: observe, communicate, explore
    private static final String STATE_OBSERVE = "OBSERVE";
    private static final String STATE_COMMUNICATE = "COMMUNICATE";
    private static final String STATE_EXPLORE = "EXPLORE";

    private SharedMapRepresentation sharedmyMap;
    private List<String> list_agentNames;
    private HashMap<String, HashMap<String, Integer>> ressources;
    private ArrayList<String> knowledge;
    private HashMap<String, Integer> last_talk_knowlege;

    public MyFSMBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
        super(myagent);
        this.sharedmyMap = new SharedMapRepresentation();
        this.ressources = new HashMap();
        this.ressources.put("Diamond", new HashMap());
        this.ressources.put("Gold", new HashMap());
        this.list_agentNames = agentNames;
        this.knowledge = new ArrayList<String>();
        this.last_talk_knowlege = new HashMap();
        
        for (String name : agentNames) { this.last_talk_knowlege.put(name, -1); }
        
        registerFirstState(new BroadCastBehaviour(myagent, this.last_talk_knowlege ,this.knowledge, this.sharedmyMap, this.ressources, this.list_agentNames), STATE_OBSERVE);

        //registerFirstState(new ObservationBehaviour(myagent, this.sharedmyMap, this.agentSeen, this.ressources), STATE_OBSERVE);
        
        registerState(new ExploreBehaviour(myagent, this.last_talk_knowlege, this.sharedmyMap, this.list_agentNames, this.ressources, this.knowledge), STATE_EXPLORE);

        // Define state transitions
        registerTransition(STATE_OBSERVE, STATE_COMMUNICATE, 1);  
        registerTransition(STATE_OBSERVE, STATE_EXPLORE, 2);  
        registerTransition(STATE_COMMUNICATE, STATE_EXPLORE, 2);  
        registerTransition(STATE_EXPLORE, STATE_OBSERVE, 3);  // when agent finished explore, it goes back to the observe state

    }
    
    private class BroadCastBehaviour extends OneShotBehaviour {
        /**
		 * 
		 */
		private static final long serialVersionUID = 8120255717271441691L;

		private int exitValue;
        private List<String> list_agentNames;
        private HashMap<String, HashMap<String, Integer>> ressources;
        private SharedMapRepresentation myMap;
        private ArrayList<String> knowledge;
        private HashMap<String, Integer> last_talk_knowlege;
        
        public BroadCastBehaviour(final AbstractDedaleAgent myagent, HashMap<String, Integer> last_talk_knowlege,  ArrayList<String> knowledge,  SharedMapRepresentation map, HashMap<String, HashMap<String, Integer>> ressources, List<String> agentNames ) {
        	super(myagent);
        	this.ressources = ressources;
        	this.myMap = map;
        	this.list_agentNames = agentNames;
        	this.knowledge = knowledge;
        	this.last_talk_knowlege = last_talk_knowlege;
        }
        
        public void action() {
            AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
    		msg.setProtocol("ShareMap");
    		msg.setSender(this.myAgent.getAID());
    		msg.setContent("Hello");
    		for (String agentName : this.list_agentNames) {
    			if (this.last_talk_knowlege.get(agentName) == this.knowledge.size()) {
    				continue;
    			}
    			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
    		}
    		myAgent.sendMessage(msg);
    		MapRepresentation theMap = this.myMap.getMyMap();
            Location myPosition = myAgent.getCurrentPosition();
            if (myPosition != null) {
                // Get the list of observable nodes from the current position
                List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();

                // 1️° Mark the current position as explored
                theMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
                // 2️° Check surrounding nodes and update the map
                String nextNodeId = null;
                for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                    Location accessibleNode = couple.getLeft();
                    boolean isNewNode = theMap.addNewNode(accessibleNode.getLocationId());
                    if (isNewNode) {
                    	StringBuilder builder = new StringBuilder("E");
                    	builder.append(myPosition);
                    	builder.append(" ");
                    	builder.append(accessibleNode);
                    	this.knowledge.add(builder.toString());
                    }
                    List<Couple<Observation, String>> observation = couple.getRight();
                    for (Couple<Observation,String> o : observation) {
                    	Observation obs_names = o.getLeft();
                    	String obs_value = o.getRight();
                    	if (obs_names == Observation.GOLD) {
                    		HashMap<String, Integer> d = this.ressources.get("Gold");
                    		d.put(myPosition.toString(), Integer.parseInt(obs_value));
                    		StringBuilder builder = new StringBuilder("G");
                    		builder.append(accessibleNode.toString());
                    		builder.append(" ");
                    		builder.append(obs_value);
                        	this.knowledge.add(builder.toString());
                    	} 
                    	else if (obs_names == Observation.DIAMOND) {
                    		HashMap<String, Integer> d = this.ressources.get("Diamond");
                    		d.put(myPosition.toString(), Integer.parseInt(obs_value));
                    	}
                    }
                    // Ensure we do not mark the current position as an edge node
                    if (!myPosition.getLocationId().equals(accessibleNode.getLocationId())) {
                    	theMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());

                        // Select the first unexplored directly reachable node
                        if (nextNodeId == null && isNewNode) {
                            nextNodeId = accessibleNode.getLocationId();
                        }
                    }
                }
            }
            try {
                myAgent.doWait(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            MessageTemplate template= MessageTemplate.and(
            MessageTemplate.MatchProtocol("ShareMap"),
            	MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            );
            while (true) {
            	ACLMessage msgr=this.myAgent.receive(template);
            	if (msgr == null) {
            		break;
            	}
            	AID id = msgr.getSender();
            	// Processing of the message
            	String textMessage = msgr.getContent();
            	/*if (!textMessage.startsWith("M")) {
            		break;
            	}*/
            	if (this.last_talk_knowlege.get(id.getLocalName()) == this.knowledge.size()) { 
            		continue;
            	}
            	this.parse_and_learnknowlege(textMessage);
            	String diff_knowledge = this.get_unknow_knowledge(id.getLocalName());
            	ACLMessage msgrespond = new ACLMessage(ACLMessage.INFORM);
            	msgrespond.setProtocol("ShareMap");
            	msgrespond.setSender(this.myAgent.getAID());
        		msgrespond.addReceiver(id);
        		msgrespond.setContent(diff_knowledge);
        		((AbstractDedaleAgent)this.myAgent).sendMessage(msgrespond);
            }
            exitValue = 2;
        }
        private String get_unknow_knowledge(String agentname) {
        	StringBuilder builder = new StringBuilder();
        	int i = this.knowledge.size();
        	int last = this.last_talk_knowlege.get(agentname);
        	for (String k : this.knowledge.reversed()) {
        		if (i == last) {
        			break;
        		}
        		builder.append(k);
        		builder.append(',');
        		i--;
        	}
        	this.last_talk_knowlege.put(agentname, i);
        	return builder.toString();
        }
        private void parse_and_learnknowlege(String sharemap_result) {
        	MapRepresentation theMap = this.myMap.getMyMap();
        	String[] myArray = sharemap_result.split(",");
        	for (String s : myArray) {
        	  if (s.charAt(0) == 'E') {
        		  String e = s.substring(1);
        		  String[] slp = e.split(" ");
        		  theMap.addNewNode(slp[0]);
        		  theMap.addNewNode(slp[1]);
        		  theMap.addEdge(slp[0], slp[1]);
        	  }
        	  else if (s.charAt(0) == 'G') {
        		  String[] spl = s.substring(1).split(" ");
        		  String location = spl[0];
        		  String value = spl[1];
        		  if (this.ressources.get("Gold").get(location) != null && this.ressources.get("Gold").get(location) > Integer.parseInt(value)) {
        			  this.ressources.get("Gold").put(location, Integer.parseInt(value));
        		  }
        	  }
        	  else if (s.charAt(0) == 'D') {
        		  String[] spl = s.substring(1).split(" ");
        		  String location = spl[0];
        		  String value = spl[1];
        		  if (this.ressources.get("Diamond").get(location) != null && this.ressources.get("Diamond").get(location) > Integer.parseInt(value)) {
        			  this.ressources.get("Diamond").put(location, Integer.parseInt(value));
        		  }
        	  }
        	}
        }
        @Override
        public int onEnd() {
            return this.exitValue;
        }

    }

    /** 
     * Observation state: The agent scans the surrounding environment, updates the map,
     * and decides whether to communicate or explore.
     */
    /** 
     * Observation state: The agent scans the surrounding environment, updates the map,
     * and decides whether to communicate or explore.
     */
    private class ObservationBehaviour extends OneShotBehaviour {
        /**
		 * 
		 */
		private static final long serialVersionUID = 8430689706443796558L;
		private int exitValue;
        private List<Couple<Location, String>> agentSeen;
        private HashMap<String, HashMap<Location, Integer>> ressources;
        private SharedMapRepresentation myMap;
        public ObservationBehaviour(final AbstractDedaleAgent myagent, SharedMapRepresentation map, HashMap<String, HashMap<Location, Integer>> ressources ) {
        	super(myagent);
        	this.ressources = ressources;
        	this.myMap = map;
        }
        
        public void action() {
        	System.out.println("hejzedoj");
            AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
            // Initialize the map if not already created
            try {
                myAgent.doWait(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            boolean agentDetected = false;
            // Retrieve the current position
            Location myPosition = myAgent.getCurrentPosition();
            if (myPosition != null) {
                // Get the observable surrounding nodes
                List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();
                Iterator<Couple<Location, List<Couple<Observation, String>>>> iter=lobs.iterator();
    			while(iter.hasNext()){
    				Couple<Location, List<Couple<Observation, String>>> next_obs = iter.next();
    				Location accessibleNode=next_obs.getLeft();
    				List<Couple<Observation, String>> list_obs = next_obs.getRight();
    				Iterator<Couple<Observation,String>> iter_obs_next = list_obs.iterator();
    				while(iter_obs_next.hasNext()) {
    					Couple<Observation, String> couple = iter_obs_next.next();
    					Observation obs = couple.getLeft();
    					String Value = couple.getRight();
    					if (obs == Observation.AGENTNAME) {
    						agentDetected = true;
    						this.agentSeen.add(new Couple(accessibleNode, obs.toString()));
    					}
    				}
    			}
            }
                if (agentDetected) {
                    this.exitValue = 1;  // Move to COMMUNICATE state
                } else {
                    this.exitValue = 2;  // Move to EXPLORE state
                }
        }

        @Override
        public int onEnd() {
            return this.exitValue;
        }

    }

    /**
     * Communication state: The agent listens for map-sharing messages from other agents
     * and merges the received maps.
     */
    
    /**
     * Exploration state: The agent moves to the closest unexplored node,
     * following the shortest path.
     */
    private class ExploreBehaviour extends OneShotBehaviour {
        /**
		 * 
		 */
		private static final long serialVersionUID = -3794632052972440256L;
		private int exitValue;
        private SharedMapRepresentation myMap;
        private HashMap<String, HashMap<String, Integer>> ressources;
        private List<String> list_agentNames;
        private ArrayList<String> knowledge;
        private HashMap<String, Integer> last_talk_knowlege;

        public ExploreBehaviour (final AbstractDedaleAgent myagent, HashMap<String, Integer> last_talk_knowlege,  SharedMapRepresentation myMap, List<String> list_agentNames, HashMap<String, HashMap<String, Integer>> ressources, ArrayList<String> knowledge) {
        	super(myagent);
        	this.ressources = ressources;
        	this.myMap = myMap;
        	this.knowledge = knowledge;
        	this.last_talk_knowlege = last_talk_knowlege;
        	this.list_agentNames = list_agentNames;
        }
        public void action() {
        	MapRepresentation theMap = this.myMap.getMyMap();
            AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
            Location myPosition = myAgent.getCurrentPosition();

            if (myPosition != null) {
                // Get the list of observable nodes from the current position
                List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();
                // Introduce a small delay for debugging purposes
                try {
                   //myAgent.doWait(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 1️° Mark the current position as explored
                theMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
                // 2️° Check surrounding nodes and update the map
                String nextNodeId = null;
                for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                    Location accessibleNode = couple.getLeft();
                    boolean isNewNode = theMap.addNewNode(accessibleNode.getLocationId());

                    // Ensure we do not mark the current position as an edge node
                    if (!myPosition.getLocationId().equals(accessibleNode.getLocationId())) {
                    	theMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());

                        // Select the first unexplored directly reachable node
                        if (nextNodeId == null && isNewNode) {
                            nextNodeId = accessibleNode.getLocationId();
                        }
                    }
                }
                // 3️° Check if the exploration is complete
                if (!theMap.hasOpenNode()) {
                    System.out.println(this.myAgent.getLocalName() + " - Exploration successfully completed.");
                    myAgent.doDelete();
                    return;
                }
                // 4️⃣ Select the next node to move to
                if (nextNodeId == null) {
                    // No directly accessible unexplored node, compute the shortest path to the closest open node
                    nextNodeId = theMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
                }
                // 5️⃣ Move to the next selected node
                System.out.println(myAgent.getLocalName() + " moving to: " + nextNodeId);

                MessageTemplate template= MessageTemplate.and(
                        MessageTemplate.MatchProtocol("ShareMap"),
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
                );
                while (true) {
                	ACLMessage msgr=this.myAgent.receive(template);
                	if (msgr == null ) {
                		break;
                	}
                	String textMessage = msgr.getContent();
                	/*if (!textMessage.startsWith("Hello")) {
                		break;
                	}*/
                	AID id = msgr.getSender();
                	ACLMessage msgrespond = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                	msgrespond.setProtocol("ShareMap");
                	msgrespond.setSender(this.myAgent.getAID());
            		msgrespond.addReceiver(id);
            		msgrespond.setContent("M");
            		((AbstractDedaleAgent)this.myAgent).sendMessage(msgrespond);
                }
                if (!myAgent.moveTo(new GsLocation(nextNodeId)) ) { // Inter blockage Random
                	List<String> opennode = theMap.getOpenNodes();
                	Random rand = new Random();
                	int n = rand.nextInt(opennode.size());
                	//theMap.getShortestPathToClosestOpenNode(""+n).get(0);
                	System.out.println(opennode.get(n));
                	nextNodeId = theMap.getShortestPath(myPosition.getLocationId(), opennode.get(n)).get(0);
                	myAgent.moveTo(new GsLocation(nextNodeId));
                }
                
                exitValue = 3; // Continue back to OBSERVE state
            }
        }

        @Override
        public int onEnd() {
            return exitValue;
        }
    }
}