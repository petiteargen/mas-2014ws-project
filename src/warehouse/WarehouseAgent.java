/**
COPYRIGHT NOTICE (C) 2014. All Rights Reserved.   
Project: KivaSolutions
@author: Argentina Ortega Sainz, Nicol�s Laverde Alfonso & Diego Enrique Ramos Avila
@version: 1.0 
@since 09.11.2014 
HBRS - Multiagent Systems
All Rights Reserved.  
 **/

package warehouse;

//import order.OrderAgent;

import jade.core.AID;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class WarehouseAgent extends Agent {
	// The order list maps the available lists to it's status (pending or
	// finished)
	private Hashtable<Integer, String> orderListStatus;
	private AID[] pickers;
	
	/* Agent initialization */
	protected void setup() {
		AgentContainer c = getContainerController();
		System.out.println(getLocalName()+": Started.");
		orderListStatus = new Hashtable<Integer, String>();

		// Get the list of orders as a start-up argument
		Object[] args = getArguments();

		// Add behaviours
		addBehaviour(new CreateOrder());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println(getAID().getLocalName()+": Terminating.");
	}

	public void updateOrderList(final int orderNum, final String parts) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				orderListStatus.put(orderNum, new String(parts));

			}
		});

	}

	private class CreateOrder extends CyclicBehaviour {
		public void action() {
			AgentContainer c = getContainerController();
			Object[] args = new Object[2];
			args[0] = "3";
			args[1] = "Allo there";

			MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.INFORM);

			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) { // Message received. Process it String order =
				int orderNum = Integer.parseInt(msg.getContent());
				System.out.println(myAgent.getLocalName()+": Received order...");

				try {
					System.out.println(myAgent.getLocalName()+":Attepting to create OrderAgent");
					AgentController a = c.createNewAgent(
							Integer.toString(orderNum), "warehouse.OrderAgent",
							args);
					//System.out.println("Attempting to start OrderAgent");
					a.start();
					System.out.println(myAgent.getLocalName()+": Created new order succesfully");
					
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				block();
			}

		}
	}
	
	private class availablePicker extends CyclicBehaviour {
		public void action() {
			
			
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			
			// Search for pickers who are free.
			sd.setType("freePicker");
			template.addServices(sd);
			try {
				// Searching process.
				DFAgentDescription[] result = DFService.search(myAgent, template); 
				
				pickers = new AID[result.length];
				System.out.println(myAgent.getLocalName()+": Found the following pickers");
				System.out.println("------------------------------------\n");
				
				// Found Agents.
				for (int i = 0; i < result.length; ++i) {
					// Listing the agents ID's found.
					pickers[i] = result[i].getName();
					System.out.println(pickers[i].getName());
				}
				System.out.println("------------------------------------\n");
				
				/* Sending Messages to the found agents. */
				ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
				for (int i = 0; i < result.length; ++i) {
					query.addReceiver(result[i].getName());
				}
				query.setContent("Are you free?");
				myAgent.send(query);				
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}

			MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.CONFIRM);
			
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println(myAgent.getLocalName()+": Picker "+msg.getSender()+" is available.");
			
			}else {
				block();
			}
			
		}
	}

}
