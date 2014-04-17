package urbansim.physical;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import urbansim.ToXML;
import urbansim.Utils;

public class Battery extends ToXML{

	private int maxcapacity;
	private double capacityremaining;
	
	private double cpuSleepDrain;
	private double cpuDrain;
	private double wifiDrain;
	private double wifiScanDrain;
	
	private double stepTime = 1000;
	
	
	private double cpuTime = 0;
	private boolean cpuActive = true;

	private double interfaceTime = 0;
	private boolean interfaceActive = true;
	
	

	
	public double getRemainingCapacity(){return capacityremaining;}
	public double getMaxCapacity(){return maxcapacity;}
	
	public Battery(File file,
				Double cpuRate,
						Double cpuSleepRate,
						Double wifiScanRate,
						Double wifiRate) {
		
		cpuDrain = cpuRate;
		cpuSleepDrain = cpuSleepRate;
		wifiScanDrain = wifiScanRate;
		wifiDrain = wifiRate;
		//System.out.println("Scan Drain Rate"+wifiScanRate);
		
		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();

			Element capacitymax = Utils.getChildElement("maxcapacity", root);
			maxcapacity = Utils.readAttributeInt("mah", capacitymax);
			//TODO introduce rnd() into current capacity
			capacityremaining = maxcapacity;
			
			
			//System.out.println(maxcapacity);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		

		
		
	}
	
	
	
	public void setTime(double time){
		  cpuTime = time;
		  cpuActive = false;
		  interfaceTime = time;
		  interfaceActive = false;
	}
	
	
	private double calcDrain(double agentTime,double time,double drainRate){
		double ret  = (agentTime - time)*(drainRate/stepTime);
		if(capacityremaining - ret < 0){
			capacityremaining =0;
		}else{
			capacityremaining -= ret;
		}
		return ret;
	}

	// device start
	public void deviceActive(double agentTime) {
		if(maxcapacity == -1){
			return;
		}
		if (cpuActive) {
			//device was already active
			calcDrain(agentTime,cpuTime,cpuDrain);
		}else{			
			//Device was asleep
			calcDrain(agentTime,cpuTime,cpuSleepDrain);
		}
		//drain sleep rate till active time

		cpuActive = true;
		cpuTime = agentTime;
	}

	

	
	// cpu timing
	public void deviceSleep(double agentTime) {
		if (maxcapacity == -1) {
			return;
		}
 		if (!cpuActive) {
		// device is still sleeping
			 calcDrain(agentTime,cpuTime,cpuSleepDrain);
		}else{
		// device was awake
			 calcDrain(agentTime,cpuTime,cpuDrain);
		}

 		cpuActive = false;
		cpuTime= agentTime;
	}

	public void interfaceActive(double agentTime) {
		if(maxcapacity == -1){
			return;
		}
		if (interfaceActive) {
			//interface already active
			calcDrain(agentTime, cpuTime, wifiDrain);
		} else {
			// device was asleep
			//no drain
		}
		interfaceActive = true;
		interfaceTime = agentTime;

	}

	public void interfaceSleep(double agentTime) {
		if(maxcapacity == -1){
			return;
		}
		if(interfaceActive){
			calcDrain(agentTime,interfaceTime,wifiDrain);
		}
		interfaceActive = false;
		interfaceTime = agentTime;

	}
	
	
	public void update(double agentTime) {
		if(maxcapacity == -1){
			return;
		}
		if (cpuActive){
			deviceActive(agentTime);
		}else{
			deviceSleep(agentTime);
		}

		if (interfaceActive){
			interfaceActive(agentTime);
		}else{
			interfaceSleep(agentTime);
		}

	}
	

	public void wifiScan(int ms) {
		if(maxcapacity == -1){
			return;
		}
		double ret =  ms*(wifiScanDrain/stepTime);
		if(capacityremaining - ret < 0){
			capacityremaining =0;
		}else{
			capacityremaining -= ret;
		}
		//System.out.println(capacityremaining);
	}
	

	public boolean hasPower() {
		//System.out.println(capacityremaining);
		if (capacityremaining > 0 || maxcapacity == -1) {
			return true;
		} else {
			return false;
		}
	}


	public boolean isCPUSleeping(){
		return cpuActive;
	}

	

	// 1 full battery 0 empty
	public double batteryRemaining() {
		if (maxcapacity == -1) {
			return 1;
		} else {
			return capacityremaining / maxcapacity;
		}
	}
	
	@Override
	public Element toXML(Element root, Document doc) {
		Element ebattery = doc.createElement("battery");
		root.appendChild(ebattery);
		// create agent element

		
		ebattery.setAttribute("maxcapacity", Long.toString(maxcapacity));
		if (maxcapacity == -1) {
			// This has unlimited power.
			return ebattery;

		}else{
			ebattery.setAttribute("capacityremaining", Double.toString(capacityremaining));

			return ebattery;
		}		
	}
}
