package urbansim;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import urbansim.p2p.DeviceAgent;
import urbansim.physical.DeviceInterface;
import urbansim.physical.LongTermStorage;
import de.matthiasmann.continuations.Coroutine;
import de.matthiasmann.continuations.CoroutineProto;
import de.matthiasmann.continuations.SuspendExecution;

public class MyAgent extends ToXML  implements DeviceAgent {

	private DeviceInterface device;
	private int floodTTL = 9;
	private double lastTime = 0;
	private boolean satisfaction = false;
	private LongTermStorage storage;
	//no one has the file by default
	private boolean hasFile = false;
	private double timeBetweenReq = 5000;
	
	private void connect(List<DeviceInterface> inRange) throws SuspendExecution,StopException{
		//connect to them all
		for (DeviceInterface d : inRange) {
			device.connect(d);
			//System.out.println("connect");
		}
		
	}
	
	
	
	private void onOFF() throws SuspendExecution, StopException{
		
		if (device.getTime() - lastTime > 10000) {
			device.interfaceOff();
			lastTime = 0;
			device.sleep(100000);
		} 
		
	}
	
	
	
	
	
	// This method must be processor friendly. It must call sleep when it has
	// finished processing
	public void main() throws SuspendExecution, StopException {

		do {

			// turn wireless on
			device.interfaceOn();

			if (lastTime == 0) {
				lastTime = device.getTime();
			}

			// Attempt to have the maximum number of connections at all times.
			if (device.getMaxConnections() > device.getActiveConnections()
					.size()) {
				// Scan for devices
				List<DeviceInterface> inRange = device.scan();
				connect(inRange);
			}

			// flood requests to all connected agents.
			if (!hasFile) {
				
				
				// send to everyone we can.
				long minSleep = Long.MAX_VALUE;
				// flood the request for the file.
				for (DeviceInterface d : device.getActiveConnections()) {
					
					
					// System.out.println(device.getName());
					Flood flood = new Flood(floodTTL, device.getName(), null);
					Message msg = new Message(device, flood, d, 500);
					long ret = device.sendTo(d, msg);
					if (ret == 0) {
						System.out.println("Send Message");
					} else if (ret == -1) {
						System.out.println("Nonrecoverable Error");
					} else if (ret > 0) {
//						System.out
//								.println("Already sending a message must wait "
//										+ ret + "ms");
					}
				}

				lastTime = device.getTime();
			}
	
			
			//process recievd messages
			Message rcv = device.recv();
			while (rcv != null) {

				Flood flood = (Flood) rcv.obj;
				if (flood.target.equals(device.getName())) {
					// We have received a message for us.
					// Store the data we have received
					if (flood.data != null) {
						// we got data.
						storage.add(flood.data, flood.data.length());
						// we now have the file.
						hasFile = true;
						satisfaction = true;
					} else {
						// Our request has made it back to us empty ignore
					}
				} else {
					if (flood.data == null && hasFile) {
						// We have reveicved a request for the file and we have it
						
							// Send a response to the target
							Flood tmp = new Flood(floodTTL, flood.target,
									(String)storage.getYoungest());
							Message msg = new Message(device, tmp, rcv.src,
									rcv.size);
							// Send the data back to the person that we got the
							// request from
							long ret = device.sendTo(rcv.src, msg);
														
						
					}else {
						// forward the message
						flood.ttl--;
						if (flood.ttl > 0) {
							for (DeviceInterface d : device
									.getActiveConnections()) {
								if (d != rcv.src) {
									Flood tmp = new Flood(flood.ttl, flood.target,
											flood.data);
									Message msg = new Message(device,
											tmp, d, rcv.size);
									device.sendTo(d, msg);
									// device.sleep();
								}
							}
						}
					}
				}
				// get a new message
				rcv = device.recv();
			}

			// sleep when done
			device.sleep();
		} while (device.isRunning());
	}

	//This is run before main so that values can be read in from files if required.
	public void constructor(DeviceInterface device, Element generic, Element agentSpecific) {
		this.device = device;
		storage = device.getStorage();
		if(agentSpecific != null){
			System.out.println(device.getName() + " got agent specific data");
			hasFile = true;
			String data = new String("cookies");
			storage.add(data, data.length());
			satisfaction = true;
		}
		
	}

	@Override
	//This allow you to write data out to a file for logging purposes
	public Element toXML(Element root, Document doc) {
		Element agent = doc.createElement("userAgent");
		root.appendChild(agent);
		agent.setAttribute("satisfied",Boolean.toString(satisfaction));
		return agent;	
		
	}
	
	


}
