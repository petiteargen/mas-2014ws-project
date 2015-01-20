/**
COPYRIGHT NOTICE (C) 2014. All Rights Reserved.   
Project: KivaSolutions
@author: Argentina Ortega Sainz, Nicol�s Laverde Alfonso & Diego Enrique Ramos Avila
@version: 1.0 
@since 09.11.2014 
HBRS - Multiagent Systems
All Rights Reserved.  
**/

package shelf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import utilities.Pose;

//import jade.core.AID;
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

/**
 * 
 * @author Diego, Nicolas, Argentina
 *
 */
public class ShelfAgent extends Agent {
	
	public static String shelfDir = "conf/shelves/shelf";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<String, Integer> inventory;
	//private boolean busy;
	protected Pose position;
	
	protected DFAgentDescription dfd;
	protected String uid;
	
	
	@SuppressWarnings("unchecked")
	protected void setup(){
		position = new Pose();
		position.randomInit(false);
		System.out.println(getLocalName()+": started at ("+position.parsePose()+").");
		//inventory = new HashMap<String, Integer>();
		Object[] args = this.getArguments();
		inventory = (HashMap<String,Integer>)args[0];
		uid = (String) args[1];
		//String inventoryType;
		//if (args != null && args.length > 0) {
			//inventoryType = (String) args[0];
		//}else {
			//inventoryType = "DEFAULT";
		//}
		//initInventory(inventoryType);
		//this.busy = false;

		this.dfd = new DFAgentDescription();
		this.dfd.setName(getAID());
		
		ServiceDescription sdOffer = new ServiceDescription();
		sdOffer.setType("offer-pieces");
		sdOffer.setName("Offer-Service");
		this.dfd.addServices(sdOffer);
		
		ServiceDescription sdLocate = new ServiceDescription();
		sdLocate.setType("send-location");
		sdLocate.setName("Locate-Service");
		this.dfd.addServices(sdLocate);
		
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new OrderRequestServer());
		addBehaviour(new waitForExternalMessages());
	}
	
	protected void takeDown(){
		System.out.println("Shelf Agent " + getAID().getName() + " terminating.");
	}
	
	/**
	 * @description The inventory should be updated each time the order picker takes pieces
	 *              from the shelf.
	 * @param piece
	 * @param amount
	 */
	public void updateInventory(final String piece, final int amount) {
		addBehaviour(new OneShotBehaviour() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void action() {
				inventory.put(piece, inventory.get(piece) - amount);
				System.out.println(myAgent.getName() + ": Only " + inventory.get(piece) + " " + piece + "s left.");

			}
		} );
	}
	
	public boolean checkPieceInInventory(String piece, int amount){
		boolean answer = false;
		if(this.inventory.containsKey(piece)){
			//if(this.inventory.get(piece) >= amount)
			if(this.inventory.get(piece) > 0)
				answer = true;
		}
		return answer;
	}
	
	@SuppressWarnings("rawtypes")
	public void restock(int amount){
		Set orderSet = inventory.entrySet();
		Iterator iter = orderSet.iterator();
		while(iter.hasNext()){
			@SuppressWarnings("unchecked")
			Map.Entry<String, Integer> lookup = (Map.Entry<String, Integer>)iter.next();
			inventory.put(lookup.getKey(), lookup.getValue() + amount);
			System.out.println(this.getName() + ": Only " + inventory.get(lookup.getKey()) + " " + lookup.getKey() + "s left.");
		}
	}
	
	@SuppressWarnings("rawtypes")
	public boolean checkWholeInventory(HashMap<String, Integer> order){
		boolean answer = true;
		Set orderSet = order.entrySet();
		Iterator iter = orderSet.iterator();
		while(iter.hasNext()){
			@SuppressWarnings("unchecked")
			Map.Entry<String, Integer> lookup = (Map.Entry<String, Integer>)iter.next();
			if(!checkPieceInInventory(lookup.getKey(), lookup.getValue()))
				return false;
		}
		
		return answer;
	}
	
	@SuppressWarnings("rawtypes")
	public int checkAvailabilityPercentage(HashMap<String, Integer> order){
		int availablePieces = 0;
		Set orderSet = order.entrySet();
		Iterator iter = orderSet.iterator();
		while(iter.hasNext()){
			@SuppressWarnings("unchecked")
			Map.Entry<String, Integer> lookup = (Map.Entry<String, Integer>)iter.next();
			if(checkPieceInInventory(lookup.getKey(), lookup.getValue())){
				if(lookup.getValue() > inventory.get(lookup.getKey())){
					availablePieces = availablePieces + inventory.get(lookup.getKey());
				}else{
					availablePieces = availablePieces + lookup.getValue();
				}
			}
		}
		
		return availablePieces;
	}
	
	@SuppressWarnings("rawtypes")
	public void updateWholeInventory(final HashMap<String, Integer> order){
		addBehaviour(new OneShotBehaviour() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;	

			@SuppressWarnings("unchecked")
			@Override
			public void action() {
				Set orderSet = order.entrySet();
				Iterator iter = orderSet.iterator();
				while(iter.hasNext()){
					Map.Entry<String, Integer> lookup = (Map.Entry<String, Integer>)iter.next();
					String piece = lookup.getKey();
					int amount = lookup.getValue();
					inventory.put(piece, inventory.get(piece) - amount);
					System.out.println(myAgent.getName() + ": Only " + inventory.get(piece) + " " + piece + "s left.");
				}
			}						
		});
	}
	
	@SuppressWarnings("rawtypes")
	public void updateRequestedInventory(final HashMap<String, Integer> order){
		addBehaviour(new OneShotBehaviour() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;	

			@SuppressWarnings("unchecked")
			@Override
			public void action() {
				Set orderSet = order.entrySet();
				Iterator iter = orderSet.iterator();
				while(iter.hasNext()){
					Map.Entry<String, Integer> lookup = (Map.Entry<String, Integer>)iter.next();
					String piece = lookup.getKey();
					int amount = lookup.getValue();
					if(inventory.containsKey(piece)){
						if(inventory.get(piece) >= amount){
							inventory.put(piece, inventory.get(piece) - amount);
						}else{
							inventory.put(piece, 0);
						}
					}
					System.out.println(myAgent.getName() + ": Only " + inventory.get(piece) + " " + piece + "s left.");
				}
			}						
		});
	}
	
	public void initInventory(String inventoryType){
		BufferedReader in;
		try {
			File inventoryFile = new File(shelfDir + inventoryType + ".txt");
			if(!inventoryFile.exists())
				inventoryType = "Default";
			in = new BufferedReader(new FileReader(shelfDir + inventoryType + ".txt"));
			String line = "";
			System.out.println(this.getLocalName() + ": Initializing inventory of type -- " + inventoryType + " --.");

			while ((line = in.readLine()) != null) {
			    String parts[] = line.split(",");
			    inventory.put(parts[0], Integer.parseInt(parts[1]));
			}
	        in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(inventory.toString());
        
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap<String, Integer> copyHM(HashMap<String, Integer> hm){
		HashMap<String, Integer> newHM = new HashMap<String, Integer>();
		
		Set orderSet = hm.entrySet();
		Iterator iter = orderSet.iterator();
		while(iter.hasNext()){
			Map.Entry<String, Integer> lookup = (Map.Entry<String, Integer>)iter.next();
			String piece = lookup.getKey();
			int amount = lookup.getValue();
			newHM.put(piece, amount);
		}
		
		return newHM;
	}
	
	
	/**
	 * @description Verifies if the pieces requested are available.
	 * @author diego
	 *
	 */
	private class OrderRequestServer extends CyclicBehaviour {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unchecked")
		public void action() {
			MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage message = myAgent.receive(template);
			if (message != null) {
				System.out.println(myAgent.getLocalName() +": Order request received");
				deregisterService();
				HashMap<String, Integer> mappy;
				try {
					mappy = (HashMap<String, Integer>)message.getContentObject();
					ACLMessage reply = message.createReply();
					reply.setPerformative(ACLMessage.CFP);
					int availablePieces = checkAvailabilityPercentage(mappy);
					//if(checkWholeInventory(mappy)){
					if(availablePieces > 0){
						String sAvailablePieces = String.valueOf(availablePieces);
						System.out.println(myAgent.getLocalName() + ": Some pieces availabe. Sending proposal...");			
						reply.setPerformative(ACLMessage.PROPOSE);
						//reply.setContent("Enough pieces available");
						reply.setLanguage(sAvailablePieces);
						double myPosition[] = position.poseToArray();
						reply.setContentObject(myPosition);
						myAgent.send(reply);
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						MessageTemplate informTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
						ACLMessage informMessage = myAgent.receive(informTemplate);
						if(informMessage != null){
							if(informMessage.getContent().matches("REREGISTER")){
								registerService();
							}else if(informMessage.getContent().matches("UPDATE-REREGISTER-BE-HAPPY")){
								//updateWholeInventory(mappy);
								updateRequestedInventory(mappy);
								registerService();
							}else if(informMessage.getContent().matches("YOU-ARE-THE-ONE")){
								System.out.println(myAgent.getLocalName() + ": I've been selected. Preparing to provide service..");
								String sName = informMessage.getLanguage();
								AID orderID = new AID(sName, AID.ISGUID);
								ACLMessage notify = new ACLMessage(ACLMessage.REQUEST);
								notify.setOntology("Check Part List");
								notify.addReceiver(orderID);
								HashMap<String, Integer> copyInventory = new HashMap<String, Integer>();
								copyInventory = copyHM(inventory);
								notify.setContentObject(copyInventory);
								send(notify);
								//addBehaviour(new cyclicMessageWaiter(myAgent, mappy));
							}
						}else{
							addBehaviour(new cyclicMessageWaiter(myAgent, mappy));
							//block();
						}
					}else{
						registerService();
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("Not enough pieces available");
						System.out.println(myAgent.getLocalName() + ": Insufficient pieces");
						myAgent.send(reply);
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//registerService();
	
			}
			else {
				block();
			}
		}
		
		public void registerService(){
			try {
				DFService.register(myAgent, dfd);
				System.out.println(myAgent.getLocalName() + ": registering service.");
			}
			catch (FIPAException fe) {
				//fe.printStackTrace();
			}
		}
		
		public void deregisterService(){
			try {
				DFService.deregister(myAgent);
				System.out.println(myAgent.getLocalName() + ": deregistering service.");

			}
			catch (FIPAException fe) {
				//fe.printStackTrace();
			}
		}
	}  
	
	private class cyclicMessageWaiter extends SimpleBehaviour {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		protected HashMap<String, Integer> order = new HashMap<String, Integer>();
		
		public cyclicMessageWaiter(Agent a, HashMap<String, Integer> mappy) {
			super(a);
			this.order = mappy;
		}
		
		public void action(){
			boolean terminationFlag = false;
			
			while(!terminationFlag){
				MessageTemplate informTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage informMessage = myAgent.receive(informTemplate);
				if(informMessage != null){
					if(informMessage.getContent().matches("REREGISTER")){
						terminationFlag = true;
						registerService();
					}else if(informMessage.getContent().matches("UPDATE-REREGISTER-BE-HAPPY")){
						//updateWholeInventory(this.order);
						updateRequestedInventory(this.order);
						terminationFlag = true;
						registerService();
					}else if(informMessage.getContent().matches("YOU-ARE-THE-ONE")){
						System.out.println(myAgent.getLocalName() + ": I've been selected. Preparing to provide service..");
						try {
							String sName = informMessage.getLanguage();
							AID orderID = new AID(sName, AID.ISGUID);
							ACLMessage notify = new ACLMessage(ACLMessage.REQUEST);
							notify.setOntology("Check Part List");
							notify.addReceiver(orderID);
							HashMap<String, Integer> copyInventory = new HashMap<String, Integer>();
							copyInventory = copyHM(inventory);
							notify.setContentObject(copyInventory);
							send(notify);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
				}else{
					//System.out.println("NULL BLAH!");
					block();
				}
			}
		}
		
		public void registerService(){
			try {
				DFService.register(myAgent, dfd);
				System.out.println(myAgent.getLocalName() + ": registering service.");
			}
			catch (FIPAException fe) {
				//fe.printStackTrace();
			}
		}

		@Override
		public boolean done() {
			return true;
		}
		
		
	}
	
	public class waitForExternalMessages extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			//System.out.println("Waiting!");
			MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchOntology("RESTOCK"));
			ACLMessage message = myAgent.receive(template);
			if (message != null) {
				System.out.println(myAgent.getLocalName() + ": Received external message!! =)");
				int amount = Integer.valueOf(message.getContent());
				System.out.println(amount);
				restock(amount);
			}
			block();
		}
		
	}


	


}
