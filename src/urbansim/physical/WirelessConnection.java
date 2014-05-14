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

	private double drainRate;
	private double scanRate;
	private String type;
	private Set<String> compatibleWith = new HashSet<String>();
	private int scanTime;
	private int concurrentConnections = 0;
	private double maxRange;
	private int handshake;
	private TreeMap<Double,Integer> distanceToBitrate = new TreeMap<Double,Integer>();
	//Maximum bandwidth that this device can serve
	private int maxBandwidth ;
	private int avalibleBandwidth;
	private int startUpTime = 0;
	private int shutdownTime = 0;
	private boolean on = true; //start with the interface on
	
	
	
	public int getMaxBandwidth(){
		return maxBandwidth;		
	}
	
	public int getAvalibleBandwidth(){
		synchronized(this){
		return avalibleBandwidth;
		}
	}
	
	public void resetBandwidth(){
		synchronized(this){
		avalibleBandwidth = maxBandwidth;
		}
	}
	
	
	
	public int commitBandwidth(int bandwidth){
		synchronized(this){
		int ret = requestBandwidth(bandwidth);
		avalibleBandwidth -= ret;
		return ret;
		}
		
	}
	
	
	public int requestBandwidth(int bandwidth){
		synchronized(this){
		int result;
		//System.out.println("Avalible " + avalibleBandwidth);
		//System.out.println("Requested " + bandwidth);
		if(avalibleBandwidth - bandwidth > 0){
			//full amount is avalible			
			result = bandwidth;			
		}else{
			//return what ever is left.
			result = avalibleBandwidth;
		}
		
		return result;
		}
		
	}
	
	
	public int bitsSent(double distance, long time){
		return (int) Math.floor(
				bitrateAtDistance(distance)*(double)time/1000);
	}
	
	
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
		drainRate = Utils.readAttributeDouble("mahs",Utils.getChildElement("drain",root));
		scanRate = Utils.readAttributeDouble("mahs",Utils.getChildElement("scanDrain",root));
		scanTime = Utils.readAttributeInt("ms",Utils.getChildElement("scanTime",root));
		maxBandwidth = Utils.readAttributeInt("kbps",Utils.getChildElement("bandwidth",root));
		startUpTime = Utils.readAttributeInt("ms",Utils.getChildElement("startuptime",root));
		shutdownTime = Utils.readAttributeInt("ms",Utils.getChildElement("shutdowntime",root));
		avalibleBandwidth = maxBandwidth;	
		
		
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
			double result = (lowValue + (highValue - lowValue)
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
		return (int)Math.ceil(
				((double)bits)/bitrateAtDistance(distance)*1000
				);	
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
	
	public double getScanDrain(){
		return scanRate;
	}
	
	public double getDrain(){
		return drainRate;
	}
	
	public double getTurnOnTime(){
		return startUpTime;
	}
	
	public double getTurnOffTime(){
		return shutdownTime;
	}
		
	
	public  boolean isOn(){
		synchronized(this){
		return on;
		}
	}
	public void turnOff(){
		synchronized(this){
		on = false;
		}
	}
	public void turnOn(){
		synchronized(this){
		on = true;
		}
	}
	

	@Override
	public Element toXML(Element root, Document doc) {
		// create agent element
		Element einterface = doc.createElement("interface");
		root.appendChild(einterface);
		einterface.setAttribute("active", Boolean.toString(on));
		einterface.setAttribute("type", type);
		einterface.setAttribute("maxBandwidth", Integer.toString(maxBandwidth));
		einterface.setAttribute("avalibleBandwidth", Integer.toString(avalibleBandwidth));
		return einterface;
	}
	
	
}
