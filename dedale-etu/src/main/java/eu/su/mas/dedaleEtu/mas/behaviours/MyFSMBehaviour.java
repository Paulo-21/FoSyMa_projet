package eu.su.mas.dedaleEtu.mas.behaviours;

import jade.core.behaviours.DataStore;
import jade.core.AID;
import jade.core.Agent;
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
import eu.su.mas.dedaleEtu.mas.knowledge.AgentInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.BooleanWrapper;
import eu.su.mas.dedaleEtu.mas.knowledge.Capacity;
import eu.su.mas.dedaleEtu.mas.knowledge.Coalition;
import eu.su.mas.dedaleEtu.mas.knowledge.CurrentSelectedCoalition;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.SharedMapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.Tresor;
import eu.su.mas.dedaleEtu.mas.knowledge.WrapperBlockage;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
    private static final String STATE_OBJECTIF = "OBJECTIF";
    private static final String STATE_EXPLORE = "EXPLORE";

    private SharedMapRepresentation sharedmyMap;
    private ArrayList<AgentInfo> knowAgents;
    private List<String> list_agentNames;
    private HashMap<String, HashMap<String, Integer>> ressources;
    private HashMap<String, Couple<Tresor, ArrayList<Coalition>>> tresor_location;
    private ArrayList<String> knowledge;
    private HashMap<String, Integer> last_talk_knowlege;
    private StringBuilder destination;
    private HashMap<String, Location> siloPosition;
    private Capacity myCap;
    private CurrentSelectedCoalition currentCoalition;
    private WrapperBlockage blockageTools;
    private BooleanWrapper coalition_mode;
    
    public MyFSMBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
        super(myagent);
        this.sharedmyMap = new SharedMapRepresentation();
        this.ressources = new HashMap();
        this.ressources.put("Diamond", new HashMap());
        this.ressources.put("Gold", new HashMap());
        this.list_agentNames = agentNames;
        this.knowledge = new ArrayList<String>();
        this.last_talk_knowlege = new HashMap();
        this.tresor_location = new HashMap<String, Couple<Tresor, ArrayList<Coalition>>>();
        this.destination = new StringBuilder();
        this.myCap = new Capacity(myagent);
        this.currentCoalition = new CurrentSelectedCoalition();
        this.blockageTools = new WrapperBlockage(0, false);
        this.coalition_mode = new BooleanWrapper();
       
        for (String name : agentNames) { this.last_talk_knowlege.put(name, -1); }
        
        registerFirstState(new BroadCastBehaviour(myagent, this.last_talk_knowlege ,this.knowledge, this.sharedmyMap, this.ressources, this.list_agentNames, this.tresor_location, currentCoalition), STATE_OBSERVE);        
        registerState(new GetObjectifs(myagent, this.last_talk_knowlege, this.sharedmyMap, this.list_agentNames, this.ressources, this.knowledge, this.tresor_location, destination,this.blockageTools, currentCoalition, this.coalition_mode), STATE_OBJECTIF);
        registerState(new MoveAndInterblockage(myagent, this.last_talk_knowlege, this.sharedmyMap, this.list_agentNames, this.ressources, this.knowledge, this.destination, this.blockageTools), STATE_EXPLORE);

        // Define state transitions
        registerTransition(STATE_OBSERVE, STATE_OBJECTIF, 1);
        registerTransition(STATE_OBJECTIF, STATE_EXPLORE, 2);
        registerTransition(STATE_EXPLORE, STATE_OBSERVE, 3);
    }


	private class BroadCastBehaviour extends OneShotBehaviour {
        /**
		 * 
		 */
		private static final long serialVersionUID = 8120255717271441691L;

		private int exitValue = 1;
        private List<String> list_agentNames;
        private HashMap<String, HashMap<String, Integer>> ressources;
        private SharedMapRepresentation myMap;
        private ArrayList<String> knowledge;
        private HashMap<String, Integer> last_talk_knowlege;
        private HashMap<String, Couple<Tresor, ArrayList<Coalition>>> tresor_location;
        private CurrentSelectedCoalition currentCoalition;
        private HashMap<String, Capacity> agentsCap;
        
        public BroadCastBehaviour(final AbstractDedaleAgent myagent, HashMap<String, Integer> last_talk_knowlege,  ArrayList<String> knowledge,  SharedMapRepresentation map, HashMap<String, HashMap<String, Integer>> ressources, List<String> agentNames, HashMap<String, Couple<Tresor, ArrayList<Coalition>>> tresor_location, CurrentSelectedCoalition currentCoalition) {
        	super(myagent);
        	this.ressources = ressources;
        	this.myMap = map;
        	this.list_agentNames = agentNames;
        	this.knowledge = knowledge;
        	this.last_talk_knowlege = last_talk_knowlege;
        	this.tresor_location = tresor_location;
        	this.currentCoalition = currentCoalition;
        	this.agentsCap = new HashMap<String, Capacity>();
        	Capacity cap = new Capacity(this.myAgent);
        	this.agentsCap.put(myagent.getName(), cap);
        	
        }
        public void true_wait(int millis) {
        	AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
        	Date currentDate = new Date();
            long timestamp = currentDate.getTime();
            while(new Date().getTime() - timestamp <= millis) {
            	try {
                    myAgent.doWait(millis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        public void action() {
        	//System.out.println("size know: "+this.knowledge.size());
        	
            AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
            Location myPosition = myAgent.getCurrentPosition();
            if (myPosition == null) { return; }
            if (myAgent.getLocalName().equals("C3")) {
            	System.out.println(myPosition );
            }
            
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
            	
                // Get the list of observable nodes from the current position
                List<Couple<Location, List<Couple<Observation, String>>>> lobs = myAgent.observe();
                
                // 1️° Mark the current position as explored
                theMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
                // 2️° Check surrounding nodes and update the map
                String nextNodeId = null;
                for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                    Location accessibleNode = couple.getLeft();
                    boolean samepos = false;
                    if(myPosition.toString().equals(accessibleNode.toString())) {
                    	samepos = true;
                	}
                    boolean tresor_saw = false;
                    boolean isNewNode = theMap.addNewNode(accessibleNode.getLocationId());
                    if (isNewNode) {
                    	StringBuilder builder = new StringBuilder("E");
                    	//System.out.println("ADD node");
                    	builder.append(myPosition);
                    	builder.append(" ");
                    	builder.append(accessibleNode);
                    	this.knowledge.add(builder.toString());
                    }
                    List<Couple<Observation, String>> observation = couple.getRight();
                    int lock = 0;
                    int gold = 0;
                    int stren = 0;
                    int diamond = 0;
                    boolean islock = false;
                    for (Couple<Observation,String> o : observation) {
                    	
                    	Observation obs_names = o.getLeft();
                    	String obs_value = o.getRight();
                    	
                    	if (obs_names == Observation.GOLD) {
                    		Integer obs_value_parsed = Integer.parseInt(obs_value);
                    		tresor_saw = true;
                    		//System.out.println(obs_value);
                    		HashMap<String, Integer> d = this.ressources.get("Gold");
                    		if (d.get(myPosition.toString()) == obs_value_parsed) {
                    			continue;
                    		}
                    		d.put(myPosition.toString(), obs_value_parsed);
                    		StringBuilder builder = new StringBuilder("G");
                    		builder.append(accessibleNode.toString());
                    		builder.append(" ");
                    		builder.append(obs_value);
                    		builder.append(";");
                        	this.knowledge.add(builder.toString());
                        	gold = obs_value_parsed;
                    	}
                    	else if (obs_names == Observation.DIAMOND) {
                    		Integer obs_value_parsed = Integer.parseInt(obs_value);
                    		tresor_saw = true;
                    		//System.out.println(obs_value);
                    		diamond = obs_value_parsed;
                    		HashMap<String, Integer> d = this.ressources.get("Diamond");
                    		if (d.get(myPosition.toString()) == obs_value_parsed) {
                    			continue;
                    		}
                    		d.put(myPosition.toString(), obs_value_parsed);
                    		StringBuilder builder = new StringBuilder("D");
                    		builder.append(accessibleNode.toString());
                    		builder.append(" ");
                    		builder.append(obs_value);
                    		builder.append(";");
                        	this.knowledge.add(builder.toString());
                    	}
                    	else if (obs_names == Observation.LOCKPICKING) {
                    		lock = Integer.parseInt(obs_value);
                    	}
						else if (obs_names == Observation.STRENGH) {
						    stren = Integer.parseInt(obs_value);
						}
						else if(obs_names == Observation.LOCKSTATUS) {
							islock = Integer.parseInt(obs_value) == 1 ? true : false;
							//System.out.println(islock+ " "+obs_value);
						}
						else if(obs_names == Observation.AGENTNAME && obs_value.contains("Tank")) {
							myAgent.emptyMyBackPack(obs_value);
						}
                    }
                    if (tresor_saw) {
                    	System.out.println("TRESOR SAW ");
                    	Tresor tresor = new Tresor(lock, stren, gold, diamond, islock);
                    	ArrayList<Coalition> n = new ArrayList();
                    	
                    	tresor.print();
                    	
                    	this.agentsCap.get(this.myAgent.getName()).print();
                    	if (tresor.is_capable(this.agentsCap.get(this.myAgent.getName())) ) {
                    		System.out.println("YES im CAPABLE");
                    		ArrayList<String> agentco = new ArrayList();
                    		agentco.add(this.myAgent.getName());
                    		Coalition c = new Coalition(agentco, this.agentsCap);
                    		//c.setTresor(tresor);
                    		n.add(c);
                    	}
                    	if (this.tresor_location.get(accessibleNode) == null) {
                        	this.tresor_location.put(accessibleNode.toString(), new Couple<Tresor, ArrayList<Coalition>>(tresor, n));
                    	}
                    	else {
                    		Couple<Tresor, ArrayList<Coalition>> couple1 = this.tresor_location.get(accessibleNode.toString());
                    		Tresor t = couple1.getLeft();
                    		t.setGold(gold);
                    		t.setDiamond(diamond);
                    		t.setIsOpen(islock);
                    		
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
					if(this.tresor_location.get(accessibleNode) != null && !tresor_saw ) { //Remove 
						this.tresor_location.remove(accessibleNode);
					}
                }
                
           if(this.currentCoalition.isActive() && this.currentCoalition.getTresorLocation().equals(myPosition.toString())) {
        	   myAgent.openLock(Observation.ANY_TREASURE);
               int res = myAgent.pick();
               System.out.println("res of thresor "+res);
               System.out.println(this.currentCoalition.getTresorLocation()+ " "+myPosition);
               this.currentCoalition.setActive(false);        		
           }
           else if (this.tresor_location.get(myPosition.toString()) != null && /*!this.tresor_location.get(myPosition.toString()).getLeft().isOpen() &&*/ this.tresor_location.get(myPosition.toString()).getLeft().getLocking() <= this.agentsCap.get(myAgent.getName()).getLocking()) {
        	   myAgent.openLock(Observation.ANY_TREASURE);
           }
           
           true_wait(400);
            
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
            	if (this.last_talk_knowlege.get(id.getLocalName()) == this.knowledge.size()) { 
            		continue;
            	}
            	MyFSMBehaviour.parse_and_learnknowlege(textMessage, this.myMap, this.ressources);
            	String diff_knowledge = MyFSMBehaviour.get_unknow_knowledge(theMap, id.getLocalName(), this.knowledge, this.last_talk_knowlege);
            	ACLMessage msgrespond = new ACLMessage(ACLMessage.INFORM);
            	msgrespond.setProtocol("ShareMap");
            	msgrespond.setSender(this.myAgent.getAID());
        		msgrespond.addReceiver(id);
        		msgrespond.setContent(diff_knowledge);
        		((AbstractDedaleAgent)this.myAgent).sendMessage(msgrespond);
            }
            true_wait(50);
            MessageTemplate template2= MessageTemplate.and(
                    MessageTemplate.MatchProtocol("ShareMap"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            );
            while (true) {
            	ACLMessage msgr=this.myAgent.receive(template2);
            	if (msgr == null) {
            		break;
            	}
            	String textMessage = msgr.getContent();
            	MyFSMBehaviour.parse_and_learnknowlege(textMessage, this.myMap, this.ressources);
            }
            exitValue = 1;
        }
        
        @Override
        public int onEnd() {
            return this.exitValue;
        }
    }
    
    private class GetObjectifs extends OneShotBehaviour {
    	/**
		 * 
		 */
		private static final long serialVersionUID = -6932897830993073426L;
		int exitValue;
		private HashMap<String, Couple<Tresor, ArrayList<Coalition>>> tresor_location;
		private List<String> list_agentNames;
        private HashMap<String, HashMap<String, Integer>> ressources;
        private SharedMapRepresentation myMap;
        private ArrayList<String> knowledge;
        private HashMap<String, Integer> last_talk_knowlege;
        private StringBuilder destination;
        private WrapperBlockage block_tool;
        private CurrentSelectedCoalition currentCoalition;
        private BooleanWrapper coalition_mode;
        
    	public GetObjectifs(final AbstractDedaleAgent myagent, HashMap<String, Integer> last_talk_knowlege,  SharedMapRepresentation sharedmyMap,  List<String> list_agentNames, HashMap<String, HashMap<String, Integer>> ressources, List<String> agentNames, HashMap<String, Couple<Tresor, ArrayList<Coalition>>> tresor_location, StringBuilder destination, WrapperBlockage blockageTools, CurrentSelectedCoalition currentCoalition, BooleanWrapper coalition_mode) {
    		super(myagent);
    		this.exitValue = 2;
    		this.tresor_location = tresor_location;
        	this.ressources = ressources;
        	this.myMap = sharedmyMap;
        	this.list_agentNames = agentNames;
        	this.knowledge = knowledge;
        	this.last_talk_knowlege = last_talk_knowlege;
        	this.tresor_location = tresor_location;
        	this.destination = destination;
        	this.block_tool = blockageTools;
        	this.currentCoalition = currentCoalition;
        	this.coalition_mode = coalition_mode;
        }
        public void action() {
        	MapRepresentation map = this.myMap.getMyMap();
        	AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
        	
        	/*if(myAgent.getLocalName() == "Tank") {
        		
        	}*/

        	Location myPosition = myAgent.getCurrentPosition();
        	//System.out.println("BLOCK VALUE : "+ this.block_tool.try_solve_block() + " "+this.block_tool.nb_blockage());
        	if (this.block_tool.nb_blockage() > 0 && this.block_tool.try_solve_block()) {
        		this.block_tool.minus_nb_blockage(1);
        		if(map.hasOpenNode()) {
        			List<String> opennode = map.getOpenNodes();
            		Random rand = new Random();
                	int n = rand.nextInt(opennode.size());      
                	this.destination.setLength(0);
                	this.destination.append( map.getShortestPath(myPosition.getLocationId(), ""+opennode.get(n)).get(0));
        		} else {
        			Random rand = new Random();
                	int n = rand.nextInt(map.getNodeCount());               	
                	this.destination.append( map.getShortestPath(myPosition.getLocationId(), ""+n).get(0));
        		}
        		return;
        	}
            
            int current_coalition_size = (int) Math.pow(2, 64);
            int current_surplus_cap = (int) Math.pow(2, 64);
            Coalition choosen_coalition = null;
            String choosen_location = null;
            for (String  loca :  tresor_location.keySet()) {
            	Couple <Tresor, ArrayList<Coalition>> tresor_tuple = tresor_location.get(loca);
            	Tresor t = tresor_tuple.getLeft();
            	List<Coalition> lc = tresor_tuple.getRight();
            	int dist = map.getShortestPath(myPosition.toString(), loca).size();
            	for (Coalition coalition : lc) {
            		if (coalition.isConfirmed() && (coalition.getCoalitionSize() < current_coalition_size || (coalition.getCoalitionSize() == current_coalition_size && coalition.getSurplusCap(t) < current_surplus_cap))) {
            			choosen_location = loca;
            			choosen_coalition = coalition;
            			this.currentCoalition.setActive(true);
            			this.currentCoalition.setTresorLocation(choosen_location);
            			System.out.println("COALITION CHOOSSEN");
            			System.out.println(coalition.getCoalitionSize()+" "+coalition.getLocking());
            		}
            	}
            }
            if (choosen_location == null && map.hasOpenNode()) {
            	choosen_location = map.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
            	
            }
            this.destination.setLength(0);
            if (choosen_location != null) {// On prend le noeud d'un trésor ou d'un opend node
            	this.destination.append(choosen_location); 
            } else { //Sinon on prend un node au hasar ;
            	Random rand = new Random();
            	int n = rand.nextInt(map.getNodeCount());               	
            	this.destination.append( map.getShortestPath(myPosition.getLocationId(), ""+n).get(0));
            }
        }
        @Override
        public int onEnd() {
            return this.exitValue;
        }
    }
    
    private class MoveAndInterblockage extends OneShotBehaviour {

		private static final long serialVersionUID = -3794632052972440256L;
		private int exitValue;
        private SharedMapRepresentation myMap;
        private HashMap<String, HashMap<String, Integer>> ressources;
        private List<String> list_agentNames;
        private ArrayList<String> knowledge;
        private HashMap<String, Integer> last_talk_knowlege;
        private StringBuilder destination;
        private WrapperBlockage block_tool;
        public MoveAndInterblockage (final AbstractDedaleAgent myagent, HashMap<String, Integer> last_talk_knowlege,  SharedMapRepresentation myMap, List<String> list_agentNames, HashMap<String, HashMap<String, Integer>> ressources, ArrayList<String> knowledge, StringBuilder destination, WrapperBlockage blockageTools) {
        	super(myagent);
        	this.ressources = ressources;
        	this.myMap = myMap;
        	this.knowledge = knowledge;
        	this.last_talk_knowlege = last_talk_knowlege;
        	this.list_agentNames = list_agentNames;
        	this.destination = destination;
        	this.block_tool = blockageTools;
 
        }
        public void action() {
        	
        	//System.out.println("BLOCK 2 : "+this.block_tool.try_solve_block() + " "+this.block_tool.nb_blockage());
        	if (this.destination.length() != 0) {
            	if (this.block_tool.nb_blockage() == 1) {
                	true_wait(50);
                } else if (this.block_tool.nb_blockage() == 3) {
                	this.block_tool.set_try_solve_block(true);
                	//sendInterBlockageNotification(this.destination.toString());
                }
                else if (this.block_tool.nb_blockage <= 0) {
                	this.block_tool.try_solve_block = false;
                	this.block_tool.nb_blockage = 0;
                }
                AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
               	if (!myAgent.moveTo(new GsLocation(this.destination.toString())) && !this.block_tool.try_solve_block) {
               		this.block_tool.nb_blockage += 1;
               	}
               	
        	}
            this.exitValue = 3; // Continue back to OBSERVE state
        }
        
        void sendInterBlockageNotification(String wantToGo) {
        	AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
            ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
    		msg.setProtocol("Interblockage");
    		msg.setSender(this.myAgent.getAID());
    		msg.setContent(myAgent.getName() +" "+myAgent.getCurrentPosition().toString()+" "+wantToGo);
    		for (String agentName : this.list_agentNames) {
    			if (this.last_talk_knowlege.get(agentName) == this.knowledge.size()) {
    				continue;
    			}
    			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
    		}
    		myAgent.sendMessage(msg);
        }
        @Override
        public int onEnd() {
            return this.exitValue;
        }
        public void true_wait(int millis) {
        	AbstractDedaleAgent myAgent = (AbstractDedaleAgent) this.myAgent;
        	Date currentDate = new Date();
            long timestamp = currentDate.getTime();
            while(new Date().getTime() - timestamp <= millis) {
            	try {
                    myAgent.doWait(millis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private static String get_unknow_knowledge(MapRepresentation myMap, String agentname, ArrayList<String> knowledge, HashMap<String, Integer> last_talk_knowlege) {
    	List<String> openodes = myMap.getOpenNodes();
    	StringBuilder builder = new StringBuilder();
    	builder.append('M');
    	int i = knowledge.size();
    	int last = last_talk_knowlege.get(agentname);
    	for (String k : knowledge.reversed()) {
    		if (i == last) {
    			break;
    		}
    		String sub = k.substring(1);
    		String[] spl = sub.split(" ");
    		builder.append("E");
    		if (openodes.contains(spl[0]) ) { builder.append("O"); } 
    		else { builder.append("C"); }
    		builder.append(spl[0]);
    		builder.append(" ");
			if (openodes.contains(spl[1]) ) { builder.append("O"); }
			else { builder.append("C"); }
			builder.append(spl[1]);
			
    		builder.append(',');
    		i--;
    	}
    	last_talk_knowlege.put(agentname, i);
    	
    	return builder.toString();
    }
    private static void parse_and_learnknowlege(String sharemap_result, SharedMapRepresentation myMap, HashMap<String, HashMap<String, Integer>> ressource_value) {
    	MapRepresentation theMap = myMap.getMyMap();
    	String[] myArray = sharemap_result.split(",");
    	for (String s : myArray) {
    	  if (s.charAt(0) == 'E') {
    		  String e = s.substring(1);
    		  String[] slp = e.split(" ");
    		  char openstate1 = slp[0].charAt(0);
    		  char openstate2 = slp[1].charAt(0);
    		  MapRepresentation.MapAttribute node_attri1 = openstate1 == 'O' ? MapRepresentation.MapAttribute.open : MapRepresentation.MapAttribute.closed;
    		  MapRepresentation.MapAttribute node_attri2 = openstate2 == 'O' ? MapRepresentation.MapAttribute.open : MapRepresentation.MapAttribute.closed;

    		  String node1 = slp[0].substring(1);
    		  String node2 = slp[1].substring(1);
    		  
    		  theMap.addNode(node1, node_attri1);
    		  theMap.addNode(node2, node_attri2);
    		  theMap.addEdge(node1, node2);
    	  }
    	  else if (s.charAt(0) == 'G') {
    		  String[] spl = s.substring(1).split(" ");
    		  String location = spl[0];
    		  String value = spl[1];
    		  if (ressource_value.get("Gold").get(location) != null && ressource_value.get("Gold").get(location) > Integer.parseInt(value)) {
    			  ressource_value.get("Gold").put(location, Integer.parseInt(value));
    		  }
    	  }
    	  else if (s.charAt(0) == 'D') {
    		  String[] spl = s.substring(1).split(" ");
    		  String location = spl[0];
    		  String value = spl[1];
    		  if (ressource_value.get("Diamond").get(location) != null && ressource_value.get("Diamond").get(location) > Integer.parseInt(value)) {
    			  ressource_value.get("Diamond").put(location, Integer.parseInt(value));
    		  }
    	  }
    	}
    }
}