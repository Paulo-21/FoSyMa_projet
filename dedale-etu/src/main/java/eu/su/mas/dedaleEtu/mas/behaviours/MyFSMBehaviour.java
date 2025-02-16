package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
import java.util.Iterator;
import java.util.List;

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

    private MapRepresentation myMap;
    private List<String> list_agentNames;

    public MyFSMBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
        super(myagent);
        this.myMap = myMap;
        this.list_agentNames = agentNames;

        // Register FSM states
        registerFirstState(new ObserveBehaviour(), STATE_OBSERVE);
        registerState(new CommunicateBehaviour(), STATE_COMMUNICATE);
        registerState(new ExploreBehaviour(), STATE_EXPLORE);

        // Define state transitions
        registerTransition(STATE_OBSERVE, STATE_COMMUNICATE, 1);  
        registerTransition(STATE_OBSERVE, STATE_EXPLORE, 2);  
        registerTransition(STATE_COMMUNICATE, STATE_EXPLORE, 2);  
        registerTransition(STATE_EXPLORE, STATE_OBSERVE, 3);  // when agent finished explore, it goes back to the observe state
    }

    /** 
     * Observation state: The agent scans the surrounding environment, updates the map,
     * and decides whether to communicate or explore.
     */
    /** 
     * Observation state: The agent scans the surrounding environment, updates the map,
     * and decides whether to communicate or explore.
     */
    private class ObserveBehaviour extends OneShotBehaviour {
        private int exitValue;

        public void action() {
            AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;

            // Initialize the map if not already created
            if (myMap == null) {
                myMap = new MapRepresentation();
                myAgent.addBehaviour(new ShareMapBehaviour(myAgent, 500, myMap, list_agentNames));
            }

            // Retrieve the current position
            Location myPosition = myAgent.getCurrentPosition();
            if (myPosition != null) {
                // Get the observable surrounding nodes
                List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();

                // Introduce a small delay for debugging purposes
                try {
                    myAgent.doWait(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Mark the current node as explored
                myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);

                // Process neighboring nodes
                boolean agentDetected = false;
                boolean explorationFinished = false;
                String nextNodeId = null;

                for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                    Location accessibleNode = couple.getLeft();
                    List<Couple<Observation, String>> observations = couple.getRight();

                    // Check if there is another agent in the observed area
                    for (Couple<Observation, String> obs : observations) {
                        if (obs.getLeft().equals(Observation.AGENTNAME)) {
                            System.out.println(myAgent.getLocalName() + " detected another agent: " + obs.getRight());
                            agentDetected = true;
                            break;
                        }
                    }

                    // Update map with new nodes
                    boolean isNewNode = myMap.addNewNode(accessibleNode.getLocationId());
                    if (!myPosition.getLocationId().equals(accessibleNode.getLocationId())) {
                        myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
                        if (nextNodeId == null && isNewNode) nextNodeId = accessibleNode.getLocationId();
                    }
                }

                // If all nodes have been explored, stop the exploration behavior but do not delete the agent
                if (!myMap.hasOpenNode()) {
                    explorationFinished = true;
                    System.out.println(this.myAgent.getLocalName() + " - Exploration successfully done, stopping exploration.");
                }

                // Decide the next state: Communicate if an agent was found, otherwise explore (unless exploration is done)
                if (explorationFinished) {
                    exitValue = 3;  // Stay in OBSERVE state (or another state for idle waiting, if necessary 
                } else if (agentDetected) {
                    exitValue = 1;  // Move to COMMUNICATE state
                } else {
                    exitValue = 2;  // Move to EXPLORE state
                }
            }
        }

        @Override
        public int onEnd() {
            return exitValue;
        }
    }

    /**
     * Communication state: The agent listens for map-sharing messages from other agents
     * and merges the received maps.
     */
    private class CommunicateBehaviour extends OneShotBehaviour {
        private int exitValue;
        // just an example with gpt, will be replaced with our method
        
        public void action() {
        	/** just an example by gpt, will be replaced by our methode
            // Listen for shared topology messages
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = this.myAgent.receive(msgTemplate);

            // If a message is received, merge the map
            if (msgReceived != null) {
                try {
                    SerializableSimpleGraph<String, MapAttribute> sgreceived =
                            (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
                    myMap.mergeMap(sgreceived);
                    System.out.println(this.myAgent.getLocalName() + " merged the received map.");
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
            */
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
        private int exitValue;

        public void action() {
            AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
            Location myPosition = myAgent.getCurrentPosition();

            if (myPosition != null) {
                // Get the list of observable nodes from the current position
                List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();
                // Introduce a small delay for debugging purposes
                try {
                    myAgent.doWait(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 1️⃣ Mark the current position as explored
                myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
                // 2️⃣ Check surrounding nodes and update the map
                String nextNodeId = null;
                for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                    Location accessibleNode = couple.getLeft();
                    boolean isNewNode = myMap.addNewNode(accessibleNode.getLocationId());

                    // Ensure we do not mark the current position as an edge node
                    if (!myPosition.getLocationId().equals(accessibleNode.getLocationId())) {
                        myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());

                        // Select the first unexplored directly reachable node
                        if (nextNodeId == null && isNewNode) {
                            nextNodeId = accessibleNode.getLocationId();
                        }
                    }
                }
                // 3️⃣ Check if the exploration is complete
                if (!myMap.hasOpenNode()) {
                    System.out.println(this.myAgent.getLocalName() + " - Exploration successfully completed.");
                    myAgent.doDelete();
                    return;
                }
                // 4️⃣ Select the next node to move to
                if (nextNodeId == null) {
                    // No directly accessible unexplored node, compute the shortest path to the closest open node
                    nextNodeId = myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
                }
                // 5️⃣ Move to the next selected node
                System.out.println(myAgent.getLocalName() + " moving to: " + nextNodeId);
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