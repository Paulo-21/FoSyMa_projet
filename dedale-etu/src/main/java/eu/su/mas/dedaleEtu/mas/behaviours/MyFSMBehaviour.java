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
    private List<Couple<Location, String>> agentSeen;
    private HashMap<String, HashMap<Location, Integer>> ressources;

    public MyFSMBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
        super(myagent);
        this.sharedmyMap = new SharedMapRepresentation();
        this.ressources = new HashMap();
        this.ressources.put("Diamond", new HashMap());
        this.ressources.put("Gold", new HashMap());
        this.list_agentNames = agentNames;
        this.agentSeen =  new Vector<>();
        registerFirstState(new BroadCastBehaviour(myagent, this.sharedmyMap, this.agentSeen, this.ressources, this.list_agentNames), STATE_OBSERVE);

        //registerFirstState(new ObservationBehaviour(myagent, this.sharedmyMap, this.agentSeen, this.ressources), STATE_OBSERVE);
        registerState(new CommunicateBehaviour(myagent, this.agentSeen, this.ressources), STATE_COMMUNICATE);
        registerState(new ExploreBehaviour(myagent, this.sharedmyMap, this.list_agentNames, ressources), STATE_EXPLORE);

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
        private List<Couple<Location, String>> agentSeen;
        private List<String> list_agentNames;
        private HashMap<String, HashMap<Location, Integer>> ressources;
        private SharedMapRepresentation myMap;
        private ArrayList<String> knowledge;
        private HashMap<String, Integer> last_talk_knowlege;
        public BroadCastBehaviour(final AbstractDedaleAgent myagent, SharedMapRepresentation map, List<Couple<Location, String>> list,HashMap<String, HashMap<Location, Integer>> ressources, List<String> agentNames ) {
        	super(myagent);
        	this.ressources = ressources;
        	this.agentSeen = list;
        	this.myMap = map;
        	this.list_agentNames = agentNames;
        	this.knowledge = new ArrayList<String>();
        }
        
        public void action() {
            AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    		msg.setProtocol("ShareMap");
    		msg.setSender(this.myAgent.getAID());
    		msg.setContent("H");
    		for (String agentName : this.list_agentNames) {
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
                    	builder.append(myPosition.toString());
                    	builder.append(" ");
                    	builder.append(accessibleNode.toString());
                    	this.knowledge.add(builder.toString());
                    }
                    List<Couple<Observation, String>> observation = couple.getRight();
                    for (Couple<Observation,String> o : observation) {
                    	Observation obs_names = o.getLeft();
                    	String obs_value = o.getRight();
                    	if (obs_names == Observation.GOLD) {
                    		HashMap<Location, Integer> d = this.ressources.get("Gold");
                    		d.put(myPosition, Integer.parseInt(obs_value));
                    	} 
                    	else if (obs_names == Observation.DIAMOND) {
                    		HashMap<Location, Integer> d = this.ressources.get("Diamond");
                    		d.put(myPosition, Integer.parseInt(obs_value));
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
    		System.out.println("Hell");
            try {
                myAgent.doWait(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Hello");
            MessageTemplate template= MessageTemplate.and(
            MessageTemplate.MatchProtocol("ShareMap"),
            	MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            );
            ACLMessage msgr = new ACLMessage(ACLMessage.INFORM);
            while (true) {
            	msgr=this.myAgent.receive(template);
            	if (msgr == null) {
            		break;
            	}
            	AID id = msgr.getSender();
            	// Processing of the message
            	String textMessage = msgr.getContent();
            	/*ACLMessage msgrespond = new ACLMessage(ACLMessage.INFORM);
            	msgrespond.setProtocol("ShareMap");
            	msgrespond.setSender(this.myAgent.getAID());
        		msgrespond.addReceiver(id);
        		msgrespond.setContent("Heelee");
        		((AbstractDedaleAgent)this.myAgent).sendMessage(msgrespond);*/
            }
            exitValue = 2;
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
        public ObservationBehaviour(final AbstractDedaleAgent myagent, SharedMapRepresentation map, List<Couple<Location, String>> list,HashMap<String, HashMap<Location, Integer>> ressources ) {
        	super(myagent);
        	this.ressources = ressources;
        	this.agentSeen = list;
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
    private class CommunicateBehaviour extends OneShotBehaviour {
        /**
		 * 
		 */
		private static final long serialVersionUID = 7254034391860846188L;
		private int exitValue;
        private List<Couple<Location, String>> agentSeen;
        private HashMap<String, HashMap<Location, Integer>> ressources;
        
        public CommunicateBehaviour(final AbstractDedaleAgent myagent, List<Couple<Location, String>> list, HashMap<String, HashMap<Location, Integer>> ressources) {
        	super(myagent);
        	this.ressources = ressources;
        	this.agentSeen = list;
        }
        public void action() {
        	System.out.println("Communicate");
        	ACLMessage msg=new ACLMessage(ACLMessage.INFORM);//FIPA
        	msg.setSender( this .myAgent.getAID());
        	msg.setProtocol("SHARE-TOPO");
        	msg.setContent("Hello World");
        	
        	msg.addReceiver(new AID(this.agentSeen.get(0).getRight(),AID.ISLOCALNAME));
        	//msg.addReceiver(new AID("ReceiverName2",AID.ISLOCALNAME));
        	((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
        	
        	// just an example by gpt, will be replaced by our methode
            // Listen for shared topology messages
            /*MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = this.myAgent.receive(msgTemplate);

            // If a message is received, merge the map
            /*if (msgReceived != null) {
                try {
                    SerializableSimpleGraph<String, MapAttribute> sgreceived =
                            (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
                    myMap.mergeMap(sgreceived);
                    System.out.println(this.myAgent.getLocalName() + " merged the received map.");
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }*/
            
            exitValue = 2;  // Directly transition to EXPLORE instead of returning to OBSERVE
        }

        @Override
        public int onEnd() {
            return exitValue;
        }
		
        
    }

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
        private HashMap<String, HashMap<Location, Integer>> ressources;
        private List<String> list_agentNames;
        
        public ExploreBehaviour (final AbstractDedaleAgent myagent, SharedMapRepresentation myMap,List<String> agentNames, HashMap<String, HashMap<Location, Integer>> ressources) {
        	super(myagent);
        	
        	this.ressources = ressources;
        	this.myMap = myMap;
        	this.list_agentNames = agentNames;
        }
        public void action() {
        	MapRepresentation theMap = this.myMap.getMyMap();
        	
        	System.out.println("Explor");
        	
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

                /*MessageTemplate template= MessageTemplate.and(
                        MessageTemplate.MatchProtocol("ShareMap"),
                        	MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                        );
                ACLMessage msgr = new ACLMessage(ACLMessage.INFORM);
                while (msgr!=null) {
                	msgr=this.myAgent.receive(template);
                	AID id = msgr.getSender();
                	// Processing of the message
                	String textMessage = msgr.getContent();
                	ACLMessage msgrespond = new ACLMessage(ACLMessage.INFORM);
                	msgrespond.setProtocol("ShareMap");
                	msgrespond.setSender(this.myAgent.getAID());
            		msgrespond.addReceiver(id);
            		msgrespond.setContent("Heelee");
            		((AbstractDedaleAgent)this.myAgent).sendMessage(msgrespond);
                }*/
                myAgent.moveTo(new GsLocation(nextNodeId));
                
                exitValue = 3; // Continue back to OBSERVE state
            }
        }

        @Override
        public int onEnd() {
            return exitValue;
        }
    }
}