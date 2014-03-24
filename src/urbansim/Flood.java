package urbansim;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Flood extends ToXML {
	public int ttl;
	public  String target;
	public Flood(int TTL,String target){
		this.ttl = TTL;
		this.target = target;
	}
	@Override
	public Element toXML(Element root, Document doc) {
		Element flood = doc.createElement("flood");
		root.appendChild(flood);
		flood.setAttribute("TTL",Integer.toString(ttl));
		flood.setAttribute("Target",target);
		return flood;
	}	
}
