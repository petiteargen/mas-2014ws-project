/**
COPYRIGHT NOTICE (C) 2015. All Rights Reserved.   
Project: KivaSolutions
@author: Argentina Ortega Sainz, Nicolas Laverde Alfonso & Diego Enrique Ramos Avila
@version: 6.5.
@since 20.01.2015 
HBRS - Multiagent Systems
All Rights Reserved.  
 **/

package station;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import utilities.PrinterUtil;
import utilities.Pose;
import shelf.ShelfAgent;
import warehouse.OrderAgent;


/**
 * <!--PICKER AGENT CLASS-->
 * Picker agent which is in charge of the dynamic between the {@link OrderAgent}, the
 * {@link RobotAgent} and the {@link ShelfAgent}. It picks an order, broadcasts the needed 
 * parts to the shelves and picks the robot nearest to a chosen shelf; it then commands 
 * the robot to fetch the shelf.
 * <p>
 * <b>Attributes:</b>
 * <ul>
 * 	<li> <i>activeAgent:</i> array of IDs which store found free RobotAgent. </li>
 * 	<li> <i>printer:</i> utility to implement colored messages. </li>
 *  <li> <i>position:</i> an instance of the class {@link Pose} with the picker position. </li>
 * 	<li> <i>busy:</i> status of the picker agent. </li>
 * </ul>
 * </p>
 * @author [DNA] Diego, Nicolas, Argentina
 */
@SuppressWarnings("serial")
public class PickerAgent extends Agent {
	
	private AID[] activeAgent;
	private PrinterUtil printer;
	protected Pose position;
	protected boolean busy;
	protected String uid;

	protected void setup() {
		Object [] args = getArguments();
		if (args.length==1){
		uid = (String) args[0];
		}else{
			uid = "";
		}
		this.printer = new PrinterUtil(5);
		busy = false;
		// PRINTOUTS: Initialization Messages.
		System.out.println("\n--PICKER-------------");
		System.out.println("Agent: " + this.getAID().getLocalName());
		System.out.println("Picker Launched!");
		printer.print("Try");
		this.position = new Pose();
		this.position.randomInit(true);
		System.out.println("---------------------\n");
		// Behaviors for the pickerAgent.
		this.addBehaviour(new GetNewOrder());
		this.addBehaviour(new UpdatePickerStatus());
		this.addBehaviour(new OrderUpdate());
	}
	
	/**
	 * <!--GET ROBOT AGENTS BEHAVIOUR-->
	 * This agent will query for {@link RobotAgent} which are registered to the DF, 
	 * description facilitator, and are offering the service of "fetch". Then,
	 * it will choose the robot nearest to a selected {@link ShelfAgent},
	 * and send to it the coordinates of the target.
	 * <p>
	 * <b>Attributes:</b>
	 * <ul>
	 * 	<li> <i>repliesCnt:</i> a counter to retrieve every reply from the robot agents.</li>
	 *  <li> <i>currentMinDistance:</i> shorter distance from the picker to the robots found.</li>
	 *  <li> <i>closestRobot:</i> ID of the robot which happens to be closest.</li>
	 *  <li> <i>closetsShelf:</i> ID of the shelf which happens to be closest.</li>
	 *  <li> <i>orderAgent:</i> ID of the order which happens to be assigned to the picker.</li>
	 * 	<li> <i>target:</i> an instance of {@link Pose} with the chosen shelf position.</li>
	 * </ul>
	 * </p>
	 * @author [DNA] Diego, Nicolas, Argentina
	 **/
	private class GetRobotAgents extends SimpleBehaviour {
		
		protected int repliesCnt = 0;
		protected double currentMinDistance = Double.MAX_VALUE;
		protected AID closestRobot;
		protected AID closestShelf;
		protected AID orderAgent;
		protected Pose target;
		/**
		 * Override constructor of a SimpleBehaviour.
		 * @param a				this agent.
		 * @param targetS		location of the target shelf as an instance of {@link Pose}.
		 * @param closestShelf	ID of the shelf selected.
		 * @param orderAgent	ID of the order assigned.
		 */
		public GetRobotAgents(Agent a, Pose targetS, AID targetID, AID order) {
			super(a);
			this.target = targetS;
			this.closestShelf = targetID;
			this.orderAgent=order;
		}
		
		/**
		 * Main action of the behavior. It implements the following thread of 
		 * actions:
		 * <ol>
		 * 	<li> Search for {@link RobotAgent}s which offer the service "fetch".
		 * Such robots are said to be free. </li>
		 * 	<li> Once the IDs of the robots are extracted, it ask the agents for
		 * their current position. </li>
		 *  <li> The closest robot to the shelf is chosen, according to the replies.
		 * 	<li> Coordinates of the chosen {@link ShelfAgent} are sent to the robot. </li>
		 *  <li> Once the robot replies, announcing the arrival of the shelf, the
		 *  behavior ends.
		 * </ol>
		 */
		public void action() {
			DFAgentDescription[] result = null;
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			// Creation of Template for the search.
			sd.setType("fetch");
			template.addServices(sd);
			/* We simulate now a TickBehavior. Every 15 seconds, this agent
			 * will query for robots which are free; i.e., robots which are
			 * registered to the description facilitator and offering the
			 * service.
			 */
			try {
				//--------------------------------------------------------------------------------
				// STEP 1. Searching process.
				boolean found = false;
				while (!found) {
					result = DFService.search(myAgent, template);
					// PRINTOUTS: Agents found.
					//System.out.println("------------------------------------");
					System.out.print(myAgent.getLocalName() + " [searching robots]: ");
					//System.out.println("Active agents:");
					activeAgent = new AID[result.length];
					// If not agents are found, do wait 15 seconds and repeat.
					if (result.length == 0) {
						System.out.println("no free agents.\n");
						//System.out.println("------------------------------------\n");
						Thread.sleep(15033);
					}
					else {
						System.out.println(result.length + " robots found.\n");
						found = true;
					}
				}
				//---------------------------------------------------------------------------------
				// STEP 2. Asking for the found agents' positions.
				for (int i = 0; i < result.length; ++i) {
					// Listing the agents ID's found.
					activeAgent[i] = result[i].getName();
					//System.out.println("  > " + activeAgent[i].getName());
				}
				//System.out.println("Robot picking will take place.");
				//System.out.println("------------------------------------\n");
				ACLMessage query = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < result.length; ++i) {
					query.addReceiver(result[i].getName());
				}
				// Fill the message's body to request the positions of the found agents.
				query.setOntology("localization");
				query.setConversationId("select-robot");
				myAgent.send(query);
				//---------------------------------------------------------------------------------
				// STEP 3. Choosing the robot closest to the shelf.
				MessageTemplate selectRobotTemplate = MessageTemplate.MatchConversationId("select-robot");
				while(this.repliesCnt < activeAgent.length) {
					ACLMessage reply = myAgent.receive(selectRobotTemplate);
					if (reply != null) {
						// Reply received
						if (reply.getPerformative() == ACLMessage.PROPOSE) {
							// Comparing distances and choosing the shortest.
							double robotPosition[] = (double[]) reply.getContentObject();
							Pose robotPose = new Pose();
							robotPose = robotPose.arrayToPose(robotPosition);
							double distance = robotPose.distance(position);
							if(distance <= currentMinDistance){
								this.currentMinDistance = distance;
								this.closestRobot = reply.getSender();
							}
						}
						this.repliesCnt++;
					}
					else {
						block();
					}	
				}
				//PRINTOUTS: Informing which agent was the closest one.
				Thread.sleep(1000);
				//System.out.println("------------------------------------");
				System.out.print(myAgent.getLocalName() + " [commanding to fetch]: ");
				System.out.println(this.closestRobot.getLocalName() + " selected.\n");
				//System.out.println("  > Fetch sent to the chosen agent.");
				//System.out.println("  > Waiting for finalization.");
				//System.out.println("------------------------------------");
				
				//--------------------------------------------------------------------------------
				// STEP 3.5. Commanding the re-register to the not-worthy robots.
				ACLMessage register_msj = new ACLMessage(ACLMessage.INFORM);
				int equal;
				for (int i = 0; i < result.length; ++i) {
					equal = result[i].getName().getName().compareTo(this.closestRobot.getName());
					if (equal != 0) {
						register_msj.addReceiver(result[i].getName());
					}
				}
				// Fill the message's body to request the positions of the found agents.
				register_msj.setOntology("register");
				//register_msj.setConversationId("select-robot");
				myAgent.send(register_msj);
				
				//--------------------------------------------------------------------------------
				// STEP 4. Commanding the fetch action.
				ACLMessage command = new ACLMessage(ACLMessage.CFP);
				command.addReceiver(this.closestRobot);
				command.setOntology("fetch");
				command.setConversationId("shelf-here");
				// Positions of this agent and the target shelf, in this order.
				String content = String.format("%s,%s", position.parsePose(), this.target.parsePose());
				command.setContent(content);
				myAgent.send(command);
				//---------------------------------------------------------------------------------
				//STEP 5. Waiting for the shelf to arrive.
				boolean here = false;
				MessageTemplate shelfHereTemplate = MessageTemplate.MatchConversationId("shelf-here");
				while (!here) {
					ACLMessage confimation = myAgent.receive(shelfHereTemplate);
					if (confimation != null) {
						//System.out.println("------------------------------------");
						System.out.print(myAgent.getLocalName() + " [status]: ");
						System.out.println("shelf here, proceeding with order.\n");
						//System.out.println("------------------------------------\n");
						here = true;
						Thread.sleep(5000);
						// Informing the shelf that it has been chosen. The shelf then
						// will communicate to the order about its parts inventory.
						ACLMessage selectMsg = new ACLMessage(ACLMessage.INFORM);
						String theId = orderAgent.getName();
						selectMsg.setLanguage(theId);
						selectMsg.setContent("YOU-ARE-THE-ONE");
						selectMsg.addReceiver(closestShelf);
						myAgent.send(selectMsg);
					}
				}
			} catch (FIPAException fe) {
				System.err.println(myAgent.getLocalName()
						+ ": Error sending the message.");
			} catch (UnreadableException e) {
				System.err.println(myAgent.getLocalName()
						+ ": Error.");
			} catch (InterruptedException e) {
				System.err.println(myAgent.getLocalName()
						+ ": Interrumption Exception.");
			}
			
		}
		
		/**
		 * Once the shelf is in the station, the processing of updating the inventory
		 * of the shelf starts, aiming to the completion of the order.
		 */
		public boolean done(){
			ACLMessage command = new ACLMessage(ACLMessage.CFP);
			command.addReceiver(this.closestRobot);
			command.setOntology("return");
			try {
				command.setContentObject(this.closestShelf);
			} catch (IOException e1) {
				System.err.println("Error setting Shelf ID");
			}
			myAgent.send(command);
			try {
				// Print out purposes. Just a delay to avoid truncated print outs.
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("Thread could not be put to sleep.");
			}
			return true;
		}
	}

	/**
	 * <!--TAKEDOWN-->
	 * <p>Safe delete of the agent.</p>
	 */
	protected void takeDown() {
		System.out.println("PickerAgent Killed!!!!!!!!.");
	}

/************************************************************************************
 ********************************** Di'Argen's Block **********************************
 ************************************************************************************/
	
	/**
	 * <!--UPDATE PICKER STATUS-->
	 * <p> Description </p>
	 * @author [DNA] Diego, Nicolas, Argentina
	 */
	private class UpdatePickerStatus extends CyclicBehaviour {
		
		private static final long serialVersionUID = 1L;
		private int repliesCnt = 0;
		//private AID closestShelf;
		private AID richestShelf;
		//private double currentMinDistance = 10000;
		private double currentMaxAvailablePieces = 0;
		private Pose currentBestPose = new Pose();
		private AID orderAgent;
		@SuppressWarnings("unchecked")
		public void action() {
			repliesCnt = 0;
			currentMaxAvailablePieces = 0;
			richestShelf = null;
			currentBestPose = new Pose();
			boolean doSearchAgents = true;
			
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
					MessageTemplate.MatchOntology("requestParts"));
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null){
				this.orderAgent = msg.getSender();
				//System.out.println(myAgent.getLocalName()	+ ": Received order. Status: busy.");
				
				/////////////// Just a test until received message is fixed ////////////////
				//HashMap<String, Integer> mappy = initMap();
				HashMap<String, Integer> mappy = new HashMap<String, Integer>();
				//////////////////////////////////////////////////////////////////////
				
				try {
					mappy = (HashMap<String, Integer>)msg.getContentObject();
				} catch (UnreadableException e2) {
					e2.printStackTrace();
				}				
		
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				// Search for agents who offer pieces (offer-pieces service).
				sd.setType("offer-pieces");
				template.addServices(sd);
				
				//////////////////////// MAGIC WHILE
				while(doSearchAgents){
					repliesCnt = 0;
					// Searching process.
					DFAgentDescription[] result = null;
					try {
						boolean found = false;
						while (!found) {
							result = DFService.search(myAgent, template);
							if(result.length == 0){
								//System.out.println(myAgent.getLocalName() + ": no free shelves found");
							}else{
								found = true;
							}
						}
						
						//System.out.println("\n\n-SEARCHING FOR AGENTS---------------");
						System.out.print(myAgent.getLocalName() + " [searching shelves]: ");
						activeAgent = new AID[result.length];
						System.out.println(result.length + " shelves found.\n");
						// Found Agents.
						for (int i = 0; i < result.length; ++i) {
							// Listing the agents ID's found.
							activeAgent[i] = result[i].getName();
							//System.out.println("  > " + activeAgent[i].getName());
						}
						//System.out.println("------------------------------------\n");
						/* Sending Messages to the found agents. */
						ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
						for (int i = 0; i < result.length; ++i) {
							cfp.addReceiver(result[i].getName());
						}
	
						
						cfp.setContentObject(mappy);
						cfp.setConversationId("select-shelf");
						
						myAgent.send(cfp);
	
						//////////////////////////////////////////////////////////////////////////////////////////////
						MessageTemplate selectShelfTemplate = MessageTemplate.MatchConversationId("select-shelf");
						ArrayList<AID> viableAgents = new ArrayList<AID>();
						while(repliesCnt < activeAgent.length){
							ACLMessage reply = myAgent.receive(selectShelfTemplate);
							if (reply != null) {
								
								// Reply received
								if (reply.getPerformative() == ACLMessage.PROPOSE) {
									doSearchAgents = false;
									viableAgents.add(reply.getSender());
									// This is an offer 
									double shelfPosition[] = (double[]) reply.getContentObject();
									int iAvailablePieces = Integer.valueOf(reply.getLanguage());
									Pose shelfPose = new Pose();
									shelfPose = shelfPose.arrayToPose(shelfPosition);
									//double distance = shelfPose.distance(position);
									if(iAvailablePieces >= currentMaxAvailablePieces){
										currentMaxAvailablePieces = iAvailablePieces;
										richestShelf = reply.getSender();
										currentBestPose = shelfPose;
									}
									/**
									if(distance <= currentMinDistance){
										currentMinDistance = distance;
										closestShelf = reply.getSender();
										currentBestPose = shelfPose;
									}**/
								}
								repliesCnt++;
							
							}
							else {
								block();
							}
						}
						System.out.print(myAgent.getLocalName() + " [requesting pieces]: ");
						System.out.println(richestShelf.getLocalName() + " selected.\n");
						//System.out.println(myAgent.getLocalName() + ": Selected Closest Shelf: " + closestShelf);
						
						ACLMessage informMsg = new ACLMessage(ACLMessage.INFORM);
						for (int i = 0; i < viableAgents.size(); i++) {
							//if(viableAgents.get(i) != closestShelf){
							if(viableAgents.get(i) != richestShelf){
								informMsg.addReceiver(viableAgents.get(i));
							}
						}
						//informMsg.setConversationId("command-register");
						informMsg.setContent("REREGISTER");
						myAgent.send(informMsg);
						
						/**
						ACLMessage selectMsg = new ACLMessage(ACLMessage.INFORM);
						String theId = msg.getSender().getName();
						selectMsg.setLanguage(theId);
						selectMsg.setContent("YOU-ARE-THE-ONE");
						selectMsg.addReceiver(richestShelf);
						myAgent.send(selectMsg);
						*/
						
						//ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
						//notify.setOntology("Check Part List");
						//notify.addReceiver(msg.getSender());
						//send(notify);
						
						if(richestShelf != null)
							addBehaviour(new GetRobotAgents(myAgent, currentBestPose, richestShelf, orderAgent));
						
						/////////////////////////////////////////////////////////////////////////////////////////////
					} catch (FIPAException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (UnreadableException e) {
						e.printStackTrace();
					} 
					
					
					busy = true;
					try {
						Thread.sleep(10000);
					}catch(Exception e){
						
					}
				//////////////////////////////// END MAGIC WHILE
				}
			}else{
				block();
			}
		}

	}
	
	/**
	 * <!--GET NEW ORDER-->
	 * Behaviour that looks for any available OrderAgent subscribed in the DF and requesting 
	 * one of them being assigned to him.
	 * @author [DNA] Diego, Nicolas, Argentina
	 */
	private class GetNewOrder extends OneShotBehaviour {

		public void action() {
			boolean doSearch = true;
			boolean first = true;
			// Update the list of robot agents.
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			// Search for agents who offer a fetch service.
			sd.setType("order");
			template.addServices(sd);
			//System.out.println("------------------------------------");
			while (doSearch) {
				try {
					// Searching process.
					DFAgentDescription[] orders = DFService.search(myAgent,
							template);
					//System.out.println("Found the following orders:");
					activeAgent = new AID[orders.length];
					// Found Agents.
					if (orders.length == 0) {
						if (first) {
							System.out.print(myAgent.getLocalName() 	+ " [searching orders]: ");
							System.out.println(" no available orders.");
							first = false;
						}
						Thread.sleep(15013);
					} 
					else {
						doSearch = false;
						System.out.print(myAgent.getLocalName() 	+ " [searching orders]: ");
						for (int i = 0; i < orders.length; ++i) {
							// Listing the agents ID's found.
							activeAgent[i] = orders[i].getName();
							//System.out.println("  > " + activeAgent[i].getName());
						}
						//System.out.println("------------------------------------\n");
						System.out.println(orders.length + " orders found.\n");
						//Requesting order assignment
						ACLMessage assign = new ACLMessage(ACLMessage.REQUEST);
						assign.addReceiver(orders[orders.length -1].getName());
						assign.setOntology("assignment");
						myAgent.send(assign);
						System.out.println(getLocalName()+" [request]: " + orders[orders.length-1].getName().getLocalName()+".\n");
					}


				} catch (FIPAException fe) {
					System.err.println(myAgent.getLocalName() + "[ERR]: Error sending the message.\n");
				} catch (InterruptedException e) {
					System.err.println(myAgent.getLocalName() + "[ERR]: interruption exception.\n");
				}
			}
		}
	}

	
	
	
	private class OrderUpdate extends CyclicBehaviour{
		public void action(){
			MessageTemplate orderMT = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
					MessageTemplate.MatchOntology("Final Shelf"));
			
			//MessageTemplate shelfMT = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchOntology("Shelf on place"));
			

			//What happens when they don't arrive at the same time?
			//MessageTemplate completeMT = MessageTemplate.and(orderMT, shelfMT);
			
			//ACLMessage completeMsg = myAgent.receive(completeMT);
			ACLMessage orderMsg = myAgent.receive(orderMT);
			//ACLMessage shelfMsg = myAgent.receive(shelfMT);
			ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
			reply.setOntology("Completed Order");
			
			if(orderMsg != null){
				//System.out.println(myAgent.getLocalName()+": Requesting new order.");
				///System.out.println(orderMsg.getSender());
				reply.addReceiver(orderMsg.getSender());
				send(reply);
				addBehaviour(new GetNewOrder());	
			}else{
				block();
			}			
		}
	}
}
