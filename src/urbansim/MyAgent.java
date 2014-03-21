package urbansim;

import java.util.ArrayList;
import java.util.List;

import de.matthiasmann.continuations.Coroutine;
import de.matthiasmann.continuations.CoroutineProto;
import de.matthiasmann.continuations.SuspendExecution;

public class MyAgent implements DeviceAgent {

	private Device device;
	private int floodTTL = 9;

	public MyAgent(Device device) {
		this.device = device;
	}

	// This method must be processor friendly. It must call sleep when it has
	// finished processing
	public void main() throws SuspendExecution {
		do {

			//Remove all old connections
			for(Device d:device.activeConnections()){
				device.disconnect(d);
			}
			// Scan for devices
			List<Device> inRange = device.scan();
			
			//connect to them all
			for (Device d : inRange) {
				device.connect(d);
			}
		

			// send message if we are the source.
			if (device.getName().equals("static@34")) {
				// System.out.println("Source");
				Flood flood = new Flood(floodTTL, "static@20");
				// send to everyone we can.
				for(Device d:device.activeConnections()){
					Message msg = new Message(device, flood, d, 0);
					device.sendTo(d, msg);
					//System.out.println("Send Message");
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
							System.out.println("Dropped");
							// don't forward the message
						} else {
							//System.out.println(recvd.ttl);
							// decrease ttl;
							Flood tmp = new Flood(recvd.ttl--, recvd.target);
							for (Device d : inRange) {
								if (d != rcv.src) {
									//System.out.println("Forwarded");
									Message msg = new Message(device, tmp, d, 0);
									device.sendTo(d, msg);
									device.sleep();
								}
							}
						}
						

					}
					rcv = device.recv();	
				}
			}
			// sleep when done
			device.sleep();
		} while (true);
	}

}
