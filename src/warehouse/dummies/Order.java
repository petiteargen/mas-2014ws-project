package warehouse.dummies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace = "warehouse.dummies.Warehouse")
@XmlType(propOrder = {"UID","products"})
public class Order {	
		String ordNum;
		HashMap <String,Integer> partList;
		private ArrayList<Product> productList;
		
		Order(){
			//Empty constructor
		}
		
		public Order(String ordNum){
			this.ordNum = ordNum;
			productList = new ArrayList<Product>();
			
			Random rnd = new Random();
			int a = rnd.nextInt(4)+1;
			
			partList = new HashMap <String,Integer>();
			partList.put("motor", a*2);
			partList.put("base", 1);
			partList.put("arms", a);
			partList.put("wires", a*4);
			partList.put("esc", a*2);
			partList.put("nazam", 1);
			partList.put("rx", 1);
			partList.put("gcu", 1);
			partList.put("pmu", 1);
			partList.put("iosd", 1);
			partList.put("cables", a*4);
			partList.put("landinggear", 1);
			partList.put("imu", 1);
			partList.put("globalmount", 1);
			partList.put("vtx", 1);
			partList.put("gimbal", 1);
			partList.put("cover", 1);
			partList.put("blade", a+1);
			
			
			for (Map.Entry<String, Integer> entry : partList.entrySet()) { 
				//System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
				Product m = new Product(entry.getKey(),entry.getValue());
				productList.add(m);
				}
			
			//System.out.println(productList.size());
		}
		
		public String getUID(){
			return ordNum;
		}
		
		@XmlElement(name="uid")
		public void setUID(String on){
			this.ordNum = on;
		}
		
		@XmlElementWrapper(name = "products")
		@XmlElement(name = "product")
		public void setProducts(ArrayList<Product> pl){
			this.productList = pl;
		}
		
		public ArrayList<Product> getProducts(){
			return productList;
		}
		
		public HashMap<String,Integer> getPartList(){
			return this.partList;
		}
		
		@Override
        public String toString() {
			String result = "-------------\nOrder "+ordNum+"\n"+printProducts();
            return result;
        }
		
		String printProducts(){
			String result ="";
			for (Product p:productList){
				result = result.concat(p.toString()+"\n");
				//System.out.println(result);
			}
			result=result.concat("-------------");
			//System.out.println(result);
			return result;
		}
	
}