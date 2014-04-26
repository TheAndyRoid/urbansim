package urbansim;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Flood extends ToXML {
	public int ttl;
	//The intended final destination
	public  String target;
	//Data null is a request else this is a response
	public String data = null;
	public Flood(int TTL,String target,String data){
		this.ttl = TTL;
		this.target = target;
		this.data = data;
	}
	@Override
	public Element toXML(Element root, Document doc) {
		Element flood = doc.createElement("flood");
		root.appendChild(flood);
		flood.setAttribute("TTL",Integer.toString(ttl));
		flood.setAttribute("Target",target);
		if(data == null){
			flood.setAttribute("Data",null);
		}else{
			flood.setAttribute("Data",data);
		}
		return flood;
	}	
}
