package urbansim.physical;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import urbansim.ToXML;
import urbansim.Utils;

public class WirelessConnection extends ToXML{

	
	private String type;
	private Set<String> compatibleWith = new HashSet<String>();
	private int scanTime;
	private int concurrentConnections;
	private int bitrate;
	private double maxRange;
	private int handshake;
	
	public WirelessConnection(String filePath){
		try {

		File fXmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);

		doc.getDocumentElement().normalize();

		Element root = doc.getDocumentElement();
		
		
		//Read in the values
		type = root.getTagName();
		concurrentConnections = Utils.readAttributeInt("c",Utils.getChildElement("concurentConnections",root));
		scanTime = Utils.readAttributeInt("ms",Utils.getChildElement("scanTime",root));
		
		
		//TODO read bitrate from graph data file into a data structure than supports fast access for larger and smaller.
		bitrate = Utils.readAttributeInt("bps",Utils.getChildElement("bitrate",root));
		//TODO read maxrange from graph
		maxRange = Utils.readAttributeDouble("m",Utils.getChildElement("maxrange",root));
		
		//TODO read in all the compatible types
		compatibleWith.add(type);
		
		
		handshake = Utils.readAttributeInt("ms",Utils.getChildElement("handshake",root));
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	// get the name of this interface type
	public String getType(){
		return null;
		
	}
	 //Calculate the bandwidth at a set range
	public double bitrateAtDistance(double m){
		//TODO update this to interpolate graph data
		return bitrate;
	}
	 //Calculate the time to send x bits a distance m returns milliseconds
	public int timeToSend(double distance, int bits){
		return (int)Math.ceil((double)bits/(double)bitrate*1000);
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
