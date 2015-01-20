package warehouse;

import java.io.File;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import warehouse.dummies.*;

/**
 * <!--INITIAL CONFIGURATION CLASS-->
 * <p>
 * Responsible for creating and/or loading the initial configuration file used
 * by the {@link WarehouseAgent}.
 * </p>
 * <b>Attributes:</b>
 * <ul>
 * <li><i>warehouse:</i> A Warehouse type object used when reading the XML
 * configuration file.</li>
 * </ul>
 * 
 * @author [DNA] Diego, Nicolas, Argentina
 */
public class InitConfig {

	Warehouse warehouse;

	/**
	 * <!--ORDER AGENT CLASS-->
	 * <p>
	 * Creates a configuration file used by the {@link WarehouseAgent}.
	 * </p>
	 * 
	 * @param orders
	 *            : Amount of orders to be generated.
	 * @param robots
	 *            : Amount of robots to be generated.
	 * @param shelves
	 *            : Amount of shelves to be generated.
	 * @param pickers
	 *            : Amount of pickers to be generated.
	 * 
	 * 
	 * @author [DNA] Diego, Nicolas, Argentina
	 */
	void createXML(int orders, int robots, int shelves, int pickers) {
		Warehouse wh = new Warehouse();

		// Creating Orders
		Orders x = new Orders(orders);
		Robots y = new Robots(robots);
		Shelves z = new Shelves(shelves);
		wh.setOrders(x);
		wh.setRobots(y);
		wh.setShelves(z);

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Warehouse.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			File XMLfile = new File("conf/warehouse/kiva5.config.xml");
			jaxbMarshaller.marshal(wh, XMLfile);
			// jaxbMarshaller.marshal(wh, System.out);
			// System.out.println("Configuration created");

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <!--ORDER AGENT CLASS-->
	 * <p>
	 * Reads a default configuration file called kiva5.config.xml. Configuration
	 * files must be stored in the folder conf/warehouse/
	 * </p>
	 * 
	 * @author [DNA] Diego, Nicolas, Argentina
	 */
	void readXML() {
		try {

			// create JAXB context and initializing Marshaller
			JAXBContext jaxbContext = JAXBContext.newInstance(Warehouse.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			// specify the location and name of xml file to be read
			File XMLfile = new File("conf/warehouse/kiva5.config.xml");

			// this will create Java object - warehouse from the XML file
			this.warehouse = (Warehouse) jaxbUnmarshaller.unmarshal(XMLfile);

			// System.out.println("Configuration read succesfuly.");

		} catch (JAXBException e) {
			// some exception occured
			e.printStackTrace();
		}

	}

	/**
	 * Read a configuration file.
	 * 
	 * Configuration files must be stored in the folder conf/warehouse/
	 * 
	 * @param xml
	 *            name of the xml file to be read. e.g. "kiva.config.xml"
	 * @author [DNA] Diego, Nicolas, Argentina
	 */
	void readXML(String xml) {
		try {

			// create JAXB context and initializing Marshaller
			JAXBContext jaxbContext = JAXBContext.newInstance(Warehouse.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			// specify the location and name of xml file to be read
			File XMLfile = new File("conf/warehouse/" + xml);

			// this will create Java object - warehouse from the XML file
			this.warehouse = (Warehouse) jaxbUnmarshaller.unmarshal(XMLfile);

		} catch (JAXBException e) {
			// some exception occured
			e.printStackTrace();
		}
	}

	void printOrders(ArrayList<Order> ol) {

		for (Order o : ol) {
			System.out.println(o.toString());
		}

	}

	/**
	 * Returns an array of Objects containing the arguments required to
	 * construct all Order Agents found in the configuration file. Each element in
	 * orderArgs ArrayList is composed of an Object[] args.
	 * 
	 * @return an ArrayList containing the required arguments for each
	 *         OrderAgent.The first argument contains a HashMap <String,Integer>
	 *         with the part list the second contains the order uid.
	 */
	ArrayList<Object[]> getOrderArgs() {

		Orders orders = this.warehouse.getOrders();
		ArrayList<Order> ol = orders.getOrderList();
		ArrayList<Object[]> orderArgs = new ArrayList<Object[]>();
		// System.out.println(ol.toString());
		for (Order o : ol) {
			Object[] args = new Object[ol.size()];
			args[0] = o.getPartList();
			args[1] = o.getUID();
			orderArgs.add(args);
		}
		return orderArgs;
	}

	/**
	 * Returns an array of Objects containing the arguments required to
	 * construct all Shelf Agents found in the configuration file. Each element in
	 * shelfArgs ArrayList is composed of an Object[] args.
	 * 
	 * @return an ArrayList containing the required arguments for each
	 *         Shelf Agent.The first argument contains a HashMap <String,Integer>
	 *         with the inventory, the second contains the shelf uid.
	 */
	
	ArrayList<Object[]> getShelfArgs() {

		Shelves shelves = this.warehouse.getShelves();
		ArrayList<Shelf> sl = shelves.getShelfList();
		ArrayList<Object[]> shelfArgs = new ArrayList<Object[]>();
		// System.out.println(ol.toString());
		for (Shelf s : sl) {
			Object[] args = new Object[sl.size()];
			args[0] = s.getPartList();
			args[1] = s.getUID();
			shelfArgs.add(args);
		}
		return shelfArgs;
	}
	
	/**
	 * Returns an array of Objects containing the arguments required to
	 * construct all Robot Agents found in the configuration file. Each element in
	 * robotArgs ArrayList is composed of an Object[] args.
	 * 
	 * @return an ArrayList containing the required arguments for each
	 *         Robot Agent.The first argument contains the robot uid.
	 */

	ArrayList<Object[]> getRobotArgs() {

		Robots robots = this.warehouse.getRobots();
		ArrayList<Robot> rl = robots.getRobotList();
		ArrayList<Object[]> robotArgs = new ArrayList<Object[]>();
		// System.out.println(ol.toString());
		for (Robot r : rl) {
			Object[] args = new Object[rl.size()];
			args[0] = r.getUID();
			robotArgs.add(args);
		}
		return robotArgs;
	}

}
