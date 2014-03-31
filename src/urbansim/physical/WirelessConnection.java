package urbansim.physical;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import urbansim.Device;
import urbansim.ToXML;
import urbansim.Utils;

public class WirelessConnection extends ToXML{

	
	private String type;
	private Set<String> compatibleWith = new HashSet<String>();
	private int scanTime;
	private int concurrentConnections = 0;
		private double maxRange;
	private int handshake;
	private TreeMap<Double,Integer> distanceToBitrate = new TreeMap<Double,Integer>();
	
	
	
	
	
	public WirelessConnection(File file){
		try {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(file);

		doc.getDocumentElement().normalize();

		Element root = doc.getDocumentElement();
		
		
		//Read in the values
		type = root.getTagName();
		concurrentConnections = Utils.readAttributeInt("c",Utils.getChildElement("concurentConnections",root));
		//System.out.println(concurrentConnections);
		scanTime = Utils.readAttributeInt("ms",Utils.getChildElement("scanTime",root));
		
		
		Element B = Utils.getChildElement("bitrateAtDistance",root);
		
		NodeList nList = B.getElementsByTagName("point");	
		
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) nNode;
					double distance = Utils.readAttributeDouble("meters", e);
					int  kbps = Utils.readAttributeInt("kbps", e);
					distanceToBitrate.put(distance,kbps);
				}
			}
		
		
		// read maxrange from graph
		maxRange = distanceToBitrate.lastEntry().getKey();
		
		Element C = Utils.getChildElement("compatableWith",root);
		NodeList cList = C.getChildNodes();
		for (int i = 0; i < cList.getLength(); i++) {
			Node nNode = cList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) nNode;
				String eName = e.getTagName();
				if(!compatibleWith.contains(eName)){
					compatibleWith.add(eName);
				}
				
			}
		}
		
		//System.out.println(compatibleWith);
		
		handshake = Utils.readAttributeInt("ms",Utils.getChildElement("handshake",root));
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	// get the name of this interface type
	public String getType(){
		return type;
		
	}
	 //Calculate the bandwidth at a set range
	public double bitrateAtDistance(double m) {
		//System.out.println("Distance: " +m);
		Map.Entry<Double, Integer> low = distanceToBitrate.floorEntry(m);
		Map.Entry<Double, Integer> high = distanceToBitrate.ceilingEntry(m);
		if (low.getKey() == distanceToBitrate.lastEntry().getKey()) {
			// Outside the range
			return distanceToBitrate.lastEntry().getValue();
		} else if (high.getKey() == distanceToBitrate.firstEntry().getKey()) {
			// Devices are 0m apart
		
			return distanceToBitrate.firstEntry().getValue();
		} else if (low != null && high != null) {
			double lowKey = low.getKey();
			//System.out.println("lowKey: " +lowKey);
			
			double lowValue = low.getValue();
			double highKey = high.getKey();
			//System.out.println("HighKey: " +highKey);
			
			double highValue = high.getValue();
			double fraction = (m - lowKey) / (highKey - lowKey);
			double result = (double)(lowValue + (highValue - lowValue)
					* fraction);
			//System.out.println("Calculated Bitrate: " + Double.toString(result)+"kbps");
			return result;

		} else {
			// Should never get here
			return -1;
		}
	}
	
	 //Calculate the time to send x bits a distance m returns milliseconds
	public int timeToSend(double distance, int bits){
		return (int)Math.ceil((double)bits/bitrateAtDistance(distance));
	} 
	//Check if a devices is compatable with this one.
	public boolean isCompatible(WirelessConnection other){
		return compatibleWith.contains(other.getType());
	}
	// The maximum range that devices can connect to this interface
	public double maxRange(){
		return maxRange;
	}
	// Time it takes for this network interface to detect others
	public int scanTime(){
		return scanTime;
	}
	// Time it takes for this network interface to detect others
	public int connectionTime(){
		return handshake;
	}
	//The maximum number of devices that can be considered active connections
	public int maxConnections(){
		return concurrentConnections;
	}

	@Override
	public Element toXML(Element root, Document doc) {
		// create agent element
		Element einterface = doc.createElement("interface");
		root.appendChild(einterface);
		einterface.setAttribute("type", type);
		return einterface;
	}
	
	
}
