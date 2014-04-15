package urbansim;

import it.polito.appeal.traci.Vehicle;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.network.Edge;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.Bag;
import sim.util.Double2D;
import urbansim.p2p.DeviceAgent;
import urbansim.physical.Battery;
import urbansim.physical.DeviceInterface;
import urbansim.physical.LongTermStorage;
import urbansim.physical.PhysicalComponent;
import urbansim.physical.WirelessConnection;
import de.matthiasmann.continuations.Coroutine;
import de.matthiasmann.continuations.CoroutineProto;
import de.matthiasmann.continuations.SuspendExecution;

public class Device extends ToXML implements Steppable, PhysicalComponent,
		DeviceInterface, CoroutineProto {

	public PhysicalComponent phy;
	public Vehicle v;
	private Double2D positionActual; // actual
	
	private String deviceType;
	private String agentType;
	private String interfaceType;
	private String batteryType;
	private String storageType;
	
	public boolean coRun = true;
	
	private long agentID;
	private UrbanSim simState;
	private DeviceAgent userAgent;
	public WirelessConnection wirelessInterface;
	public LongTermStorage storage;
	private Battery battery;
	
	private int stepTime = 1000;

	private int range = 50;

	private Lock running = new ReentrantLock();
	

	private double startTime = 0;
	private double agentTime = 0;

	private int sleepTime = 0;

	private SimplePortrayal2D portrayal;
	private Color colour;
	
	
	// java-continuation
	private Coroutine co;

	private List<Message> sendBuffer = new ArrayList<Message>();
	private Map<Message,Integer> ammountSent = new HashMap<Message,Integer>();
	
	
	private PriorityQueue<Message> recvBuffer = new PriorityQueue<Message>();

	
	private List<Message> logSent = new ArrayList<Message>();
	private List<Message> logReceived = new ArrayList<Message>();

	private List<Edge> activeConnections = new ArrayList<Edge>();
	private Map<Edge,Long> messageSentTime = new HashMap<Edge,Long>();

	/*
	 * Checks that this devices still has time left otherwise it pauses execution of the agent.
	 */
	private void checkGotTime() throws SuspendExecution, StopException {
		isRunning();
		if (agentTime > startTime + stepTime) {
			Coroutine.yield();
		}
	}

	
	/*
	 * Attempts to send messages that are send buffer. Checks that receiving
	 * devices are in range and that a connection to the still exists. Once the
	 * message is sent they are removed from the send buffer.
	 */
	private void processSendBuffer() {
		List<Message> toRemove = new ArrayList<Message>();
		List<Message> needsSent = new ArrayList<Message>();
		synchronized (simState.connection) {
			// lock from others
			synchronized (sendBuffer) {
				// Remove messages that have been sent
				synchronized (ammountSent) {
					for (Message m : sendBuffer) {
						
						if (m.recvtime != 0 && m.recvtime < agentTime) {
							// Message was sent last timestep remove it.
							toRemove.add(m);
						}
						
					}
					// Remove
					for (Message m : toRemove) {
						sendBuffer.remove(m);
						ammountSent.remove(m);
					}

					toRemove.clear();

					for (Message m : sendBuffer) {
						
						//The message might have already been sent, but can't be remove due to timing
						Integer sent = ammountSent.get(m);
						if(sent != null){
							if(sent == m.size){
								//Skip this message it has already been sent by sendMessage
								continue;
							}
						}
						
						
						
						Device dst = (Device) m.dst;
						// Check that the recvr is still in range and connected
						// to
						if (inRange(dst) && connectedTo(dst)) {
							int toSend = m.size - ammountSent.get(m);
							int bitsSent = sendTime(dst, m, toSend);

							// add the number of bits sent.
							ammountSent.put(m, (bitsSent + ammountSent.get(m)));

							if(ammountSent.get(m)== m.size){
								needsSent.add(m);
							}
							
							if(bitsSent<0){
								System.out.println("Sent negative bits - how");
							}
							
							
							Edge c = findEdge(dst);
							if (c == null) {
								System.out
										.println("Connection does not exist?");
							} else {
								Double dataSent = (Double) c.getInfo();
								
								dataSent += bitsSent;
								if(dataSent>1500){
									System.out.println("Sent more than possible ever");
								}
								c.setInfo(dataSent);
							}
							// System.out.println("ProcessBuffer "
							// + ammountSent.get(m) + " of " + m.size
							// + " Sent");
						} else {
							// We are no longer connected or out of range remove
							// the message
							toRemove.add(m);

						}
					}
					
					for(Message m: needsSent){
						((Device)m.dst).receive(m);
						// Log the message as sent.
					}

					// Remove out of range on unconnected messages

					for (Message m : toRemove) {
						sendBuffer.remove(m);
						ammountSent.remove(m);
					}

				}
			}
		}
	}

	/*
	 * Constructor used when an agent has a fixed position
	 * Calls sumo constructor and then sets the devices position.
	 */
	public Device(Element agentElement, Long id, SimState state,
			File deviceType, Map<String, File> agentTypes,
			Map<String, File> interfaceTypes,
			Map<String, File> agentData,
			Map<String, File> batteryTypes,
			Map<String, File> storageTypes) {
		this(
			id, 
			state, deviceType, 
			agentTypes, 
			interfaceTypes,
			agentData,
			batteryTypes,
			storageTypes);
		// Load the position
		Element ePos = (Element) agentElement.getElementsByTagName("position")
				.item(0);
		setPosition(state, Utils.readAttributeDouble2D(ePos));
	}

	/*
	 * Constructor used when the agent position will be updated later.
	 */
	public Device(Long id, SimState state, File deviceType,
			Map<String, File> agentTypes, 
			Map<String, File> interfaceTypes,
			Map<String, File> agentData,
			Map<String, File> batteryTypes,
			Map<String, File> storageTypes) {
		agentID = id;
		simState = (UrbanSim)state;

		// Read agent and interface in.
		readAgent(deviceType, agentTypes, interfaceTypes,agentData,batteryTypes,storageTypes);

		co = new Coroutine(this);

		// Add the device to the connected network
		UrbanSim urbansim = (UrbanSim) simState;
		urbansim.connected.addNode(this);
		
		
		
		
	}

	public String getAgentType() {
		return agentType;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public long getID() {
		return agentID;
	}

	/*
	 * Returns the unique name for this device.
	 * 
	 */
	public String getName() {
		String tmp = new String(deviceType + "@" + agentType + "@" + agentID);
		// System.out.println(tmp);
		return tmp;
	}

	public Double2D currentPosition() {
		return positionActual;
	}

	public Color getColour(){
		return colour;		
	}
	public SimplePortrayal2D getPortrayal(){
		//Reflection to set paint colour and fill.
		try {
			Field field = portrayal.getClass().getDeclaredField("paint");
			field.set(portrayal,getColour());
			if(hasPower() == false){
				field = portrayal.getClass().getDeclaredField("filled");
				field.set(portrayal,false);			
			}
			
			
			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return portrayal;
	}
	
	
	/*
	 * Set the device to the supplied position and update the ui.
	 */
	public void setPosition(SimState state, Double2D newPosition) {
		positionActual = newPosition;
		UrbanSim urbansim = (UrbanSim) state;
		Double2D pos = new Double2D(newPosition.getX(), newPosition.getY());
		urbansim.agentPos.setObjectLocation(this, pos);

	}

	/*
	 * Sets the agent to the beginning of the simulation step if it's time is less
	 */
	private void updateTime() {
		// get time

		double newStartTime = simState.schedule.getTime() * stepTime;
		if (newStartTime > startTime) {
			//new cycle update stuff
			
			updateConsumables();
			
			//Stops the gui looking bad and these would be detected by the device
			removeDudConnections();
			
			processSendBuffer();

			startTime = newStartTime;
			if (agentTime < startTime) {
				agentTime = startTime;
			}
		}

	}
	/*
	 * Updates battery drain and resets bandwidth.
	 */
	private void updateConsumables() {
		// get time
		startTime = simState.schedule.getTime() * stepTime;
		if (agentTime <= startTime) {
			// calculate cpu idle time and drain battery
			battery.calculateSleepDrain(startTime - agentTime);
			// Reset interface bandwidth
			
			}
		wirelessInterface.resetBandwidth();
		// Reset connection bandwidth
		
		resetConnectionBandwidthUI();
		
	}
	
	

	/*
	 * This is the function that the simulation engine will call once per time step.
	 * If the agent time is before this step the agent time is increased. If the agent time is after 
	 * the agent is asleep or has used more processing time and must wait.
	 * The battery power for the device is also checked to make sure that the device has battery
	 * the main function is timed for battery drain (it is often so short that it's not measurable)
	 */
	public void step(SimState state) {
		//update time
		updateTime();
		
			
		
		if (agentTime < (startTime + stepTime) && battery.hasPower()) {
			running.lock();
			try {
				if (co.getState() == Coroutine.State.NEW
						|| co.getState() == Coroutine.State.SUSPENDED) {
					battery.startTimer(); 
					
					do {
						double startTime = agentTime;
						co.run();
						double stopTime = agentTime;
						battery.mainloop(stopTime-startTime);
						agentTime++;
					} while ((agentTime < (startTime + stepTime))
							&& !recvBuffer.isEmpty() && coRun);
					// update connections ui
					 
					battery.stopTimer(); 
				}
		} finally {
			running.unlock();
		}

			
		}else if(battery.hasPower() == false && activeConnections.isEmpty() != true){
			resetConnections();
			System.out.println("                        Battery Ran OUT");

		}
		

	}

	
	
	
	/*
	 * Reads in configuration options from the input files.
	 * Class specific data is passed to their corresponding constructors
	 */
	public void readAgent(File deviceType, 
			Map<String, File> agentTypes,
			Map<String, File> interfaceTypes,
			Map<String, File> agentData,
			Map<String, File> batteryTypes,
			Map<String, File> storageTypes) {
		// agentID = Utils.readAttributeLong("id", eAgent);
		File agent = null;
		File deviceInterface = null;
		File deviceBattery = null;
		File storageFile = null;
		
		Double cpusleepDrain = 0.0;
		Double cpuDrain = 0.0;

		// Read the device file
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(deviceType);

			doc.getDocumentElement().normalize();

			 Element d = doc.getDocumentElement();

			this.deviceType = Utils.stripExtension(deviceType.getName());

			// Read the agent type
			Element a = Utils
					.getChildElement("agent", doc.getDocumentElement());
			// Get the agent file
			agentType = Utils.readAttributeString("type", a);
			agent = agentTypes.get(agentType);

			// Read the interface type
			Element i = Utils.getChildElement("interface",
					doc.getDocumentElement());
			interfaceType = Utils.readAttributeString("type", i);
			deviceInterface = interfaceTypes.get(interfaceType);
			
			// Read the battery Type
			Element b = Utils.getChildElement("battery", doc.getDocumentElement());
			batteryType = Utils.readAttributeString("type",b);
			deviceBattery = batteryTypes.get(batteryType);
			
			
			//Read in portrayal inf
			Element p = Utils.getChildElement("portrayal", doc.getDocumentElement());
			readPortrayal(p);
			
			// Read the storage Type
			Element s = Utils.getChildElement("storage", doc.getDocumentElement());
			storageType = Utils.readAttributeString("type",s);
			storageFile = storageTypes.get(storageType);
			
			
			cpusleepDrain = Utils.readAttributeDouble("mahs",Utils.getChildElement("cpusleepdrain", doc.getDocumentElement()));
			cpuDrain = Utils.readAttributeDouble("mahs",Utils.getChildElement("cpudrain", doc.getDocumentElement()));
			
			
					
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create agent
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(agent);

			doc.getDocumentElement().normalize();

			//Create agent based on class name suppiled
			Element flood = Utils.getChildElement("class",
					doc.getDocumentElement());
			String className = Utils.readAttributeString("name", flood);
			Class<?> clazz = Class.forName(className);
			userAgent = (DeviceAgent) clazz.newInstance();
			
			Element agentGenericElement = doc.getDocumentElement();
			
			File agentFile = agentData.get(getName());
			Element agentDataElement = null;
			if(agentFile!=null){
				DocumentBuilderFactory aFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder aBuilder = aFactory.newDocumentBuilder();
				Document adoc = aBuilder.parse(agentFile);
				agentDataElement = adoc.getDocumentElement();				
			}
			
			
			
			
			userAgent.constructor(this, agentGenericElement,agentDataElement);

		} catch (Exception e) {
			e.printStackTrace();
		}

		
		//Create storage
		
		storage = new LongTermStorage(storageFile);
		
		// Create Interface

		wirelessInterface = new WirelessConnection(deviceInterface);
		
		//Create Battery
		battery = new Battery(deviceBattery,
					cpuDrain,
					cpusleepDrain,
						wirelessInterface.getScanDrain(),
						wirelessInterface.getDrain());
		
		// System.out.println(wirelessInterface.connectionTime());

	}

	private void readPortrayal(Element root){
		
		String shape = Utils.readAttributeString("shape",root);
		int scale = Utils.readAttributeInt("scale",root);
		if(shape.equals("circle")){
			portrayal = new OvalPortrayal2D(scale);
		}else if(shape.equals("rectangle")){
			portrayal = new RectanglePortrayal2D(scale);
		}
		
		Element c = Utils.getChildElement("colour",root);
		int r = Utils.readAttributeInt("r",c);
		int g = Utils.readAttributeInt("g",c);
		int b = Utils.readAttributeInt("b",c);
		colour = new Color(r,g,b);	
	}
	
	/*
	 * Reset the bandwidth of the connection.
	 */
	
	//TODO
	public void resetConnectionBandwidthUI() {
		synchronized (simState.connection) {
			for (Edge e : activeConnections) {
				e.setInfo(new Double(0.0));
			}
		}
	}
	
	/*
	 * Search for existing connection between the two devices. 
	 * Returns the edge if found, otherwise returns null;
	 */
	//TODO

	private Edge checkForExistingConnection(Object nodeA,Object nodeB){
		UrbanSim urbansim = (UrbanSim) simState;
		//get all edges that are associated with this node.
		Bag knownConnections = urbansim.connected.getEdgesIn(nodeA);
		for(Object o:knownConnections){
			Edge e = (Edge)o;
			if(e.getOtherNode(nodeA).equals(nodeB)){
				return e;
			}
			
		}
		return null;
	}

	/*
	 * Remove connections in the ui that no longer exist
	 */
	private void removeOldConnectionsUI(){
		UrbanSim urbansim = (UrbanSim) simState;
		//get all edges that are associated with this node.
		Bag knownConnections = urbansim.connected.getEdgesIn(this);
		List<Edge> toRemove = new ArrayList<Edge>();
		for(Object o:knownConnections){
			Edge e = (Edge)o;
			if(!connectedTo((Device)e.getOtherNode(this))){
				//The connection does not exist add it to the remove list
				toRemove.add(e);
			}			
		}		
		//Remove the dead edges
		for(Edge e:toRemove){
			urbansim.connected.removeEdge(e);
		}
		
	}
	
	
	/*
	 * Called by a device than wants to connect to us.
	 * Could cause deadlocks when using syncronized to used trylock instead,
	 */
	public boolean acceptConnection(Device d) {
		boolean result;
		synchronized(simState.connection){
		if (activeConnections.size() < wirelessInterface.maxConnections()) {
			// Can accept connection
			Edge e = new Edge(this, d, new Double(0));
			activeConnections.add(e);
			result = true;
		} else {
			result = false;
		}
		return result;
		}
		
	}
	
	
	
	/*
	 * Adds a connection to this device and the one that we are connecting to
	 */
	private boolean addConnection(Device d) {
		boolean result;
		//Check that the device has not been destroyed
		if(!simState.allAgents.contains(d)){
			return false;
		}
		
		
		Edge e = new Edge(this, d, new Double(0));
		// System.out.println(activeConnections.size());
		synchronized(simState.connection){
			// can we make the connection
			if ((activeConnections.size() < wirelessInterface.maxConnections() && !connectedTo(d))) {
				// Did the other device acceptConnection
				if (d.acceptConnection(this)) {
					activeConnections.add(e);

					result = true;
				} else {
					result = false;
				}
			} else {
				result = false;
			}
		}
		return result;
	}
/*
 * Returns true if there is a connection to the device otherwise false	
 */
	public boolean connectedTo(Device d) {
		synchronized(simState.connection){
		if (findEdge(d) != null) {			
			return true;
		} else {
			return false;
		}	
		}
	}

	/*
	 * returns true if there is a message being sent to the device.
	 *  and it's rcv time is greater than agent time or not set
	 */
	public boolean sendingMessageTo(Device d) {
		synchronized (sendBuffer) {
			for (Message m : sendBuffer) {
				if (m.dst == d && (m.recvtime == 0 || m.recvtime > agentTime)) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	/*
	 * Remove the device from the list of active connections on this device. Remove any pending sent or receievd messages
	 */
	public boolean removeConnection(Device b) {
		boolean result = false;
		synchronized (simState.connection) {
			Edge e = findEdge(b);
			if (e != null) {

				// connection is removed
				activeConnections.remove(e);

				// Remove pending sent messages as these can never arrive

				synchronized (sendBuffer) {
					List<Message> toRemove = new ArrayList<Message>();
					for (Message m : sendBuffer) {
						if (m.dst == b) {
							toRemove.add(m);
						}
					}

					for (Message m : toRemove) {
						sendBuffer.remove(m);
						ammountSent.remove(m);
					}
				}

				// Remove received messages that will arrive after the current agent time

				synchronized (recvBuffer) {
					List<Message> toRemove = new ArrayList<Message>();
					for (Message m : recvBuffer) {
						if (m.src == b && m.recvtime > agentTime) {
							toRemove.add(m);
						}
					}
					for (Message m : toRemove) {
						recvBuffer.remove(m);
					}

				}

				// removed the connection
				return true;
			}

		}

		return false;
	}

	/*
	 * Resets all connections with this device.
	 * 
	 */
	public void resetConnections() {
		UrbanSim urbansim = (UrbanSim) simState;
		synchronized (simState.connection) {
			List<Edge> toRemove = new ArrayList<Edge>(activeConnections); 
			for(Edge e: toRemove){
				//inform the other device.
				//((Device)e.getOtherNode(this)).removeConnection(this);
				disconnect(((Device)e.getOtherNode(this)));
			}
			activeConnections.clear();
		}
	}
	
	/*
	 *Update the ui. This is not thread safe as it is done by a single task
	 */
	public void updateUI() {
		UrbanSim urbansim = (UrbanSim) simState;
		// synchronized (urbansim.connected) {
		// Removing node destroys all the edges
		// urbansim.connected.removeNode(this);
		// Add the node back.
		// urbansim.connected.addNode(this);
		// Add all the edges
		// Remove connections that nolonger exist
		removeOldConnectionsUI();

		for (Edge e : activeConnections) {
			// get the other end of the connection
			Device other = (Device) e.getOtherNode(this);
			
			
			Double ourValue = (Double) e.getInfo();

			Edge otherEdge = other.findEdge(this);
			if(otherEdge==null){
				//Other device does not have the connection. Remove it from the ui
				Edge tmp = checkForExistingConnection(this, other);	
				urbansim.connected.removeEdge(tmp);
				//done for this edge
				continue;
			}
			
			
			Double otherValue = (Double) otherEdge.getInfo();
			Double sum = new Double(otherValue + ourValue);
			if(sum<0){
				System.out.println("No such thing as negative bandwidth");
			}
			
			
			//get the edge
			Edge tmp = checkForExistingConnection(this, other);		
			
			if (tmp == null) {
				// Create the edge as it does not exist
				Edge ui = new Edge(this, other, sum);
				urbansim.connected.addEdge(ui);
			} else {
			
				//get the value of the edge.
				//TODO track total bandwith per connection
				//Double value = (Double)tmp.getInfo();
				//sum +=value;
				
				//Update the value
				urbansim.connected.updateEdge(tmp,this,other,sum);

			}

			// else the edge exists and has already got values

		}

	}
	
	
	public Edge findEdge(Device b) {
		Edge found = null;
		synchronized(simState.connection){
		for (Edge e : activeConnections) {
			if (e.getOtherNode(this) == b) {
				// System.out.println("Found edge");
				found = e;
				break;
			}
		}
		}

		return found;
	}

	private boolean inRange(Device b) {
		Double2D aPos = this.currentPosition();
		if (aPos.distance(b.currentPosition()) <= wirelessInterface.maxRange()) {
			return true;
		} else {
			// That agent is out of range.
			return false;
		}
	}

	/*
	*This function is called by a devices that is sending us a message. It is the responsibility of the
	*other device to make sure that we can received the message, in range, bandwidth etc. This function will
	*wake up the device if possible.
	*/
	private void receive(Message msg) {
		UrbanSim urbansim = (UrbanSim) simState;
		synchronized (recvBuffer) {
			// System.out.println("Device Got Message!");
			recvBuffer.add(msg);
		}

		synchronized (logReceived) {
			logReceived.add(msg);
		}

		// Check if this device can be started if not already running
		// and that the message is not in the next simulation cycle.
		
		
		if (running.tryLock()) {
			try {
				updateTime();
				
				if (msg.recvtime < (startTime + stepTime)) {

					// device has time remanining.
					if (agentTime < (startTime + stepTime)) {
						if (co.getState() == Coroutine.State.NEW
								|| co.getState() == Coroutine.State.SUSPENDED) {
							// the agent has woken up to a msg set agenttime.
							// agentTime = msg.recvtime;
							double startTime = agentTime;
							co.run();
							double stopTime = agentTime;
							battery.mainloop(stopTime - startTime);
						}
					}

				}
			} finally {
				running.unlock();
			}
		}

	}
	/*
	 * This function calculates if the message can be sent this simulation cycle or the next because of 
	 * limited bandwidth. It does not check if the devices are within range. 
	 * 
	 * If the message can be sent the receiving devices receive method is called.
	 * returns the number of bits sent this time interval
	 */
	private int sendTime(Device dst, Message msg, int bitsToSend) {

//		System.out.println("Bits to send " + bitsToSend);
		// calculate distance
		Double distance = currentPosition().distance(dst.currentPosition());
		// Time bits
		int sendTime = wirelessInterface.timeToSend(distance, bitsToSend);
//		System.out.println("SendTime " + sendTime);
		
		double continueSending = agentTime;
		
		if(agentTime>(startTime+stepTime)){
			//The agent has slept through a time interval , but will still send pending messages from the start of that interval
			continueSending = startTime;
		}
		
		if ((continueSending + sendTime) > (startTime + stepTime)) {
			// The devices could move closer or further apart causing
			// bitrate to change, send what we can.

			int timeRemaning = (int) ((startTime + stepTime) - continueSending);
			// System.out.println("TimeRemaining "+ timeRemaning);
			int maxbitsSent = wirelessInterface.bitsSent(distance, timeRemaning);
			int actualBitsSent = getBandwidth(dst, maxbitsSent);
			// return the actual number of bits sent.
//			System.out.println("ActualBitsSent Top" + actualBitsSent);
			if(actualBitsSent<0){
				System.out.println("Negative bits");
			}
			return actualBitsSent;
		} else {
			// The message could be completely sent in this interval

			int actualBitsSent = getBandwidth(dst, bitsToSend);
			int bitsSentAlready = 0;
			synchronized (ammountSent) {
				if (ammountSent.containsKey(msg)) {
					bitsSentAlready = ammountSent.get(msg);
				}
			}

			if (actualBitsSent + bitsSentAlready >= msg.size) {
				// The entire message has been sent
				msg.recvtime = agentTime + sendTime;
				// give the message to the other device
				//dst.receive(msg);
			}

			// Add the amount sent to the connection total.
//			System.out.println("ActualBitsSent Botum" + actualBitsSent);

			if(actualBitsSent<0){
				System.out.println("Negative bits");
			}
			
			return actualBitsSent;

		}

	}
	
	/* This requests bandwidth from both interfaces. The one with the least
	 * bandwidth is the limiting factor,so only that amount of bandwidth is
	 * committed to the connection on both devices.
	 */
	private int getBandwidth(Device dst,int bits){
		synchronized(wirelessInterface){
		int local = wirelessInterface.requestBandwidth(bits);
		int remote = dst.wirelessInterface.requestBandwidth(bits);
		if(local > remote){
			dst.wirelessInterface.commitBandwidth(remote);
			wirelessInterface.commitBandwidth(remote);
//			System.out.println("Got " + remote);
			return remote;
		}else{
			dst.wirelessInterface.commitBandwidth(local);
			wirelessInterface.commitBandwidth(local);
//			System.out.println("Got " + local);
			return local;
		}
		}
	}
	/*
	 * Checks that the receiving device is in range, an active connection exists, 
	 * and that there is not already a message waiting in the send buffer.
	 * 
	 * Messages are left in the send buffer and cleared on the next simulation step, 
	 * this stops the devices from dissconnecting before the message is sent.
	 */
	private long sendMessage(DeviceInterface a, Message msg) {

		Device dst = (Device) a;
		// System.out.println("Send To!");
		synchronized (simState.connection) {

			if (connectedTo(dst) && (msg != null) && inRange(dst)
					&& !sendingMessageTo(dst)) {

				// Time the message was sent
				msg.sendtime = agentTime;

				// System.out.println("Agent Time " +
				// Double.toString(msg.sendtime));

				int sentbits = sendTime(dst, msg, msg.size);
				if(sentbits<0){
					System.out.println("Sent negative bits - how");
				}

				synchronized (ammountSent) {
					synchronized (sendBuffer) {
						// put the message in the send buffer and the number of
						// bits sent
						sendBuffer.add(msg);
						ammountSent.put(msg, sentbits);
					}
				}

				if (sentbits == msg.size) {
					((Device) msg.dst).receive(msg);
				}

				// System.out.println("Send Message " + sentbits + " of "
				// + msg.size + " Sent");
				// System.out.println("Recv Time " +
				// Double.toString(msg.recvtime));

				Edge c = findEdge(dst);
				if (c == null) {
					System.out.println("Connection does not exist");
				} else {
					Double dataSent = (Double) c.getInfo();
					if(dataSent>1500){
						System.out.println("Sent more than possible ever");
					}
					dataSent += sentbits;
					c.setInfo(dataSent);
				}

				// Log the message as sent.
				synchronized (logSent) {
					logSent.add(msg);
				}
				return 0;

			} else if (!connectedTo(dst) || !inRange(dst)) {
				// Error sending message not connected
				agentTime += 5;
				return -1;
			} else if (sendingMessageTo(dst)) {
				for (Message m : sendBuffer) {
					if (m.dst == dst && (m.recvtime == 0 || m.recvtime > agentTime)) {
						if (m.recvtime == 0 ) {
							// message can't be sent this interval, wait till
							// next
							return (long) ((startTime + stepTime)-agentTime);
						} else {
							// m.recvtime > agentTime, but less that next step
							// message will be sent this interval return the
							// sleep time for the next earliest  message
							return (long) ((m.recvtime + 1)-agentTime);
						}
					}
				}
			}
		}

		// should not get here, but return an error
		return -1;

	}

	// Wrapper function for deviceInterface
	/*Sends a message to the other device only if there are no other messages being sent to that device, 
	 * we are connected to that device and within range.
	 * returns 0 on success
	 * returns >0 when there is a message being sent already, value is the earliest time that the device might accept a message
	 * returns -1 when there is no connection or device is out of range
	 */
	public long sendTo(DeviceInterface a, Message msg) throws SuspendExecution,StopException {
		isRunning();
		// check that a connection exists
		long result = sendMessage(a, msg);
		checkGotTime();
		return result;

	}

	// Wrapper function for deviceInterface
	public Message recv() throws SuspendExecution,StopException {
		isRunning();
		// time to receive a message
		checkGotTime();
		return getMessage(recvBuffer);
	}
	
	//Gets a message out of the receive buffer only if the agent time is greater than the time the message was received.
	public Message getMessage(Queue<Message> buffer) {
		Message msg;
		synchronized (recvBuffer) {
			if (recvBuffer.isEmpty()) {
				// System.out.println("Empty Buffer");
				msg = null;
			} else {
				// System.out.println("Got a message");
				//Check that the agent has recievd the message
				if (recvBuffer.peek().recvtime < (agentTime) ) {
					msg = recvBuffer.remove();
				} else {
					msg = null;
				}
			}
		}
		return msg;
	}

	

	// This is for the hack cast I'm aware, its bad but needed
	/*
	 * The scan will remove any connections that are impossible, ie out of range
	 * 
	 */
	@SuppressWarnings("unchecked")
	public List<DeviceInterface> scan() throws SuspendExecution,StopException {
		isRunning();
		// Time to perform a scan
		agentTime += wirelessInterface.scanTime();
		battery.wifiScan(wirelessInterface.scanTime());
		
		UrbanSim urbansim = (UrbanSim) simState;
		List<Device> inRange = urbansim.inRange(this, wirelessInterface);
		
		
		
		checkGotTime();
		// This is a hack cast, but should be OK
		return (List<DeviceInterface>) (List<?>) inRange;
	}
	
	
	
	public void removeDudConnections() {
		synchronized(simState.connection){			
		
			// Check the list of active connections and remove any that arn't in
			// range.
			List<Edge> toRemove = new ArrayList<Edge>();
			for (Edge e : activeConnections) {
				if (!inRange((Device) e.getOtherNode(this))) {
					toRemove.add(e);
				}
			}			
			// Remove the dead connections
			for (Edge e : toRemove) {
				removeConnection((Device)e.getOtherNode(this));
				((Device)e.getOtherNode(this)).removeConnection(this);
			}	
			
		}
	}

	public WirelessConnection getInterface() {
		return wirelessInterface;
	}
	
	public Battery getBattery(){return battery;}

	/*
	 * Causes the agent to sleep at it current position of execution.Receiving a
	 * new message will wake up the agent.
	 */
	public void sleep() throws SuspendExecution,StopException {
		isRunning();
		Coroutine.yield();
	}

	/*
	 * Increases agentTime by the corresponding amount. Causes the agent to not
	 * be runnable till after this time.
	 */
	public void sleep(long millaseconds) throws SuspendExecution,StopException {
		isRunning();
		agentTime += millaseconds;
		Coroutine.yield();
	}

	@Override
	public void coExecute() throws SuspendExecution {
		try{
		userAgent.main();
		}catch(StopException s){
			return;
		}

			
	}
	
	/*
	 * Wrapper function that connects to another device.
	 * 
	 */
	public boolean connect(DeviceInterface d) throws SuspendExecution,StopException {
		isRunning();
		agentTime += wirelessInterface.connectionTime();
		checkGotTime();
		if(addConnection((Device) d) ){
			return true;
		}else{
			return false;
		}
	}

	/*
	 * Wrapper function that disconnects nicely from another device
	 * 
	 */
	public boolean disconnect(DeviceInterface d) {
		synchronized (simState.connection) {
				if (!sendingMessageTo((Device) d) && connectedTo((Device) d)) {
					// Can only disconnect if not sending messages and connected
					// to the device
					removeConnection((Device) d);
					// This is an unkind disconnect and will cause all messages
					// to be dropped
					((Device) d).removeConnection(this);
					return true;
				} else {
					return false;
				}
			}
	}
	
	@Override
	//Wrapper for activeCon() can't have locks in a method that throws suspendexecution
	public List<DeviceInterface> getActiveConnections() throws SuspendExecution,StopException {
		isRunning();
		return activeCon();
	}

	/*
	 * Returns a list of all connections that the devices thinks are still
	 * active, does not check if the agents are still in range
	 */
	private List<DeviceInterface> activeCon() {
		List<DeviceInterface> connectedTo = new ArrayList<DeviceInterface>();
		synchronized(simState.connection){
			
			for (Edge e : activeConnections) {
				connectedTo.add((DeviceInterface) e.getOtherNode(this));
			}
			
		} 
		return connectedTo;
	}

	

	@Override 
	public Element toXML(Element root, Document doc) {
		// Write to file

		// create agent element
		Element agentElement = doc.createElement("agent");
		root.appendChild(agentElement);

		// set attributes
		agentElement.setAttribute("agentType", getAgentType());
		agentElement.setAttribute("devicetype", getDeviceType());
		agentElement.setAttribute("id", Long.toString(getID()));

		Element position = doc.createElement("position");

		position.setAttribute("x", Double.toString(currentPosition().getX()));
		position.setAttribute("y", Double.toString(currentPosition().getY()));

		
		//Log battery
		battery.toXML(agentElement,doc);
		wirelessInterface.toXML(agentElement,doc);
		
		
		agentElement.appendChild(position);

		Element elogSent = doc.createElement("Sent");
		agentElement.appendChild(elogSent);
		for (Message m : logSent) {
			m.isSender = true;
			m.toXML(elogSent, doc);
		}
		logSent.clear();

		Element elogRecv = doc.createElement("Recv");
		agentElement.appendChild(elogRecv);
		for (Message m : logReceived) {
			m.isSender = false;
			m.toXML(elogRecv, doc);
		}
		logReceived.clear();
		return agentElement;

	}

	
	

	public boolean hasPower(){
		return battery.hasPower();
	}

	@Override
	public LongTermStorage getStorage() {
		return storage;
	}
	
	public int getMaxConnections(){
		return wirelessInterface.maxConnections();
	}


	@Override
	public boolean isRunning() throws StopException {
		if(coRun == false){
			throw new StopException("Full Stop");
		}
		return coRun;
	}
	
	
	
}
