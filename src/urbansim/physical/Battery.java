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
	
	
	private long cpuTime = 0;
	

	
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

	// Use for cpu timing
	public void startTimer() {
		cpuTime = getUserTime();
	}

	// cpu timing
	public void stopTimer() {
		//System.out.println(cpuTime);
		long time = getUserTime() - cpuTime;
		//System.out.println(time);
	}

	public void wifiScan(int ms) {
		if(maxcapacity == -1){
			return;
		}
		capacityremaining = capacityremaining - (ms*wifiScanDrain);
		//System.out.println(capacityremaining);
	}
	
	public void mainloop(double ms){
		capacityremaining = capacityremaining - (cpuDrain*ms);
	}

	public boolean hasPower() {
		//System.out.println(capacityremaining);
		if (capacityremaining > 0 || maxcapacity == -1) {
			return true;
		} else {
			return false;
		}
	}

	// This calculates the total drain for the simulated device.
	// Using unacounted time as sleep cpu and the current Wireless state
	public void calculateSleepDrain(Double ms) {
		if(maxcapacity == -1){
			return;
		}
		capacityremaining = capacityremaining - (ms*cpuSleepDrain);
	}
	
	public long getUserTime( ) {
	    ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    return bean.isCurrentThreadCpuTimeSupported( ) ?
	        bean.getCurrentThreadUserTime( ) : 0L;
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
