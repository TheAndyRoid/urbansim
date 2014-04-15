package urbansim;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import urbansim.p2p.DeviceAgent;
import urbansim.physical.DeviceInterface;
import de.matthiasmann.continuations.Coroutine;
import de.matthiasmann.continuations.CoroutineProto;
import de.matthiasmann.continuations.SuspendExecution;

public class MyAgent implements DeviceAgent {

	private DeviceInterface device;
	private int floodTTL = 9;

	private void connect(List<DeviceInterface> inRange) throws SuspendExecution,StopException{
		//connect to them all
		for (DeviceInterface d : inRange) {
			device.connect(d);
			//System.out.println("connect");
		}
		
	}
	
	// This method must be processor friendly. It must call sleep when it has
	// finished processing
	public void main() throws SuspendExecution,StopException {
		do {

			//Get 
			if(device.getMaxConnections() > device.getActiveConnections().size()){
				// Scan for devices
				List<DeviceInterface> inRange = device.scan();
				connect(inRange);
			}
			
			
			
			
			//System.out.println(device.getName());

			// send message if we are the source.
			if (device.getName().equals("busStop@flood@34")) {				
				System.out.println("Source");
				//System.out.println(device.getName());
				Flood flood = new Flood(floodTTL, "busStop@flood@20");
				// send to everyone we can.
				for(DeviceInterface d:device.getActiveConnections()){
					Message msg = new Message(device, flood, d, 500);
					
					if(device.sendTo(d, msg)){
						System.out.println("Send Message");	
					}
					
				device.sleep(1);	
				}
			} else {
				// System.out.println("Not Source");
				Message rcv = device.recv();
				while(rcv != null) {	
					
					if (((Flood) rcv.obj).target.equals(device.getName())) {
						// hack exit
						System.out.println("Message            Recived");
						System.out.println("Message            Recived");
						System.out.println("Message            Recived");
						//System.exit(0);
					} else {
												
						Flood recvd = (Flood) rcv.obj;
						//System.out.println(recvd.ttl);
						if (recvd.ttl <= 0) {
							//System.out.println("Dropped");
							// don't forward the message
						} else {
							//System.out.println(recvd.ttl);
							// decrease ttl;
							Flood tmp = new Flood(recvd.ttl--, recvd.target);
							for (DeviceInterface d : device.getActiveConnections()) {
								if (d != rcv.src) {
									//System.out.println("Forwarded");
									Message msg = new Message(device, tmp, d, rcv.size);
									device.sendTo(d, msg);
									//device.sleep();
								}
							}
						}
						

					}
					rcv = device.recv();	
				}
			}
			// sleep when done
			device.sleep();
		} while (device.isRunning());
	}

	@Override
	public void constructor(DeviceInterface device, Element generic, Element agentSpecific) {
		if(agentSpecific != null){
			System.out.println(device.getName() + " got agent specific data");
		}
		this.device = device;
	}

}
