package urbansim;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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

import de.matthiasmann.continuations.*;
import it.polito.appeal.traci.StepAdvanceListener;
import it.polito.appeal.traci.Vehicle;
import sim.engine.*;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.network.Edge;
import urbansim.p2p.DeviceAgent;
import urbansim.physical.Battery;
import urbansim.physical.DeviceInterface;
import urbansim.physical.LongTermStorage;
import urbansim.physical.PhysicalComponent;
import urbansim.physical.WirelessConnection;

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
	
	private long agentID;
	private SimState simState;
	private DeviceAgent userAgent;
	public WirelessConnection wirelessInterface;
	public LongTermStorage storage;
	private Battery battery;
	
	private int stepTime = 1000;

	private int range = 50;

	private Lock running = new ReentrantLock();
	private Lock connection = new ReentrantLock();

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

	/*
	 * Checks that this devices still has time left otherwise it pauses execution of the agent.
	 */
	private void checkGotTime() throws SuspendExecution {
		if (agentTime > startTime + stepTime) {
			Coroutine.yield();
		}
	}

	
	/*
	 * Attempts to send messages that are send buffer. 
	 * Checks that receiving devices are in range and that a connection to the still exists.
	 * Once the message is sent they are removed from the send buffer.
	 */
	private void processSendBuffer() {
		List<Message> toRemove = new ArrayList<Message>();
		synchronized (sendBuffer) {
			synchronized (ammountSent) {
				for (Message m : sendBuffer) {
					Device dst = (Device) m.dst;
					// Check that the recvr is still in range and calculate
					// bandwidth
					if (inRange(dst) && connectedTo(dst)) {
						int toSend = m.size - ammountSent.get(m);
						int bitsSent =sendTime(dst, m, toSend); 
						if(m.size <= toSend+bitsSent ){
							//The message has been sent remove it
							toRemove.add(m);
						}else{
							//The message has not been sent leave it in the buffer and update the number of bits sent
							ammountSent.put(m,(bitsSent + ammountSent.get(m)));
						}
						System.out.println(ammountSent.get(m)+ " of " +m.size+ " Sent");
					} else {
						//We are no longer connected or out of range.
						toRemove.add(m);
					}
				}
				for(Message m:toRemove){
					sendBuffer.remove(m);
					ammountSent.remove(m);
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
		simState = state;

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
		startTime = simState.schedule.getTime() * stepTime;
		if (agentTime < startTime) {			
			agentTime = startTime;			
		}
	}
	/*
	 * Updates battery drain and resets bandwidth.
	 */
	private void updateConsumables() {
		// get time
		startTime = simState.schedule.getTime() * stepTime;
		if (agentTime < startTime) {
			// calculate cpu idle time and drain battery
			battery.calculateSleepDrain(startTime - agentTime);
			// Reset connection bandwidth
			wirelessInterface.resetBandwidth();
		}
	}
	
	

	/*
	 * This is the function that the simulation engine will call once per time step.
	 * If the agent time is before this step the agent time is increased. If the agent time is after 
	 * the agent is asleep or has used more processing time and must wait.
	 * The battery power for the device is also checked to make sure that the device has battery
	 * the main function is timed for battery drain (it is often so short that it's not measurable)
	 */
	public void step(SimState state) {
		//update battery and bandwidth
		updateConsumables();
		//update time
		updateTime();
		
		if (agentTime < (startTime + stepTime) && battery.hasPower()) {
			running.lock();
			try {
				if (co.getState() != Coroutine.State.RUNNING
						&& co.getState() != Coroutine.State.FINISHED) {
					battery.startTimer(); 
					processSendBuffer();
					do {
						double startTime = agentTime;
						co.run();
						double stopTime = agentTime;
						battery.mainloop(stopTime-startTime);
					} while ((agentTime < startTime + stepTime)
							&& !recvBuffer.isEmpty());
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
	 * Adds a connection to the other device
	 */
	private boolean addConnection(Device d) {
		boolean result;
		Edge e = new Edge(this, d, new Double(0));

		//System.out.println(activeConnections.size());
		connection.lock();
		try {
			// can we make the connection
			if ((activeConnections.size() < wirelessInterface.maxConnections())) {
				// Did the other device acceptConnection
				
				if (d.acceptConnection(this)) {
					UrbanSim urbansim = (UrbanSim) simState;
					synchronized (urbansim.connected) {
					activeConnections.add(e);
					urbansim.connected.addEdge(e);	
					}
					result = true;
				} else {
					result = false;
				}
			}else{
			result = false;
			}
		} finally {
			connection.unlock();
		}
		return result;
	}
/*
 * Returns true if there is a connection to the device otherwise false	
 */
	public boolean connectedTo(Device d) {
		if (findEdge(d) != null) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * returns true if there is a message being sent to the device otherwise false.
	 */
	public boolean sendingMessageTo(Device d) {
		synchronized (sendBuffer) {
			for (Message m : sendBuffer) {
				if (m.dst == d) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	/*
	 * Remove the device from the list of active connections on this device.
	 */
	public boolean removeConnection(Device b) {
		
		Edge toRemove = findEdge(b);
		if (toRemove != null) {
			connection.lock();
			try {
				activeConnections.remove(toRemove);
				//System.out.println("Removed edge");
				// need to update the ui
				UrbanSim urbansim = (UrbanSim) simState;
				synchronized (urbansim.connected) {
					urbansim.connected.removeEdge(toRemove);
				}
			} finally {
				connection.unlock();
			}
			//removed the connection
			return true;
		}
		return false;		
	}

	/*
	 * Resets all connections with this device. It also informs all devices that it was connected to 
	 * 
	 */
	private void resetConnections() {
		UrbanSim urbansim = (UrbanSim) simState;
		connection.lock();
		try {
			for(Edge e: activeConnections){
				//inform the other device.
				((Device)e.getOtherNode(this)).removeConnection(this);
			}
			activeConnections.clear();
		} finally {
			connection.unlock();
		}
		
		synchronized (urbansim.connected) {
			urbansim.connected.removeNode(this);
			updateUI();
		}

	}
	
	private void updateUI() {
		UrbanSim urbansim = (UrbanSim) simState;
		synchronized (urbansim.connected) {
			urbansim.connected.removeNode(this);
			synchronized (activeConnections) {
				for (Edge e : activeConnections) {
					urbansim.connected.addEdge(e);
				}
			}
		}
	}
	
	
	private Edge findEdge(Device b) {
		Edge found = null;
		UrbanSim urbansim = (UrbanSim) simState;
		connection.lock();
		try {
			for (Edge e : activeConnections) {
				if (e.getOtherNode(this) == b) {
					// System.out.println("Found edge");
					found = e;
					break;
				}
			}
		} finally {
			connection.unlock();
		}
		return found;
	}

	private boolean inRange(Device b) {
		UrbanSim urbansim = (UrbanSim) simState;
		Double2D aPos = this.currentPosition();

		if (aPos.distance(b.currentPosition()) <= wirelessInterface.maxRange()) {
			return true;
		} else {
			// That agent is out of range.
			removeConnection(b);
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
		if (msg.recvtime < (startTime + stepTime)) {
			if (running.tryLock()) {
				try {
					updateTime();
					if (agentTime < startTime + stepTime) {
						if (co.getState() != Coroutine.State.RUNNING
								&& co.getState() != Coroutine.State.FINISHED) {

							double startTime = agentTime;
							co.run();
							double stopTime = agentTime;
							battery.mainloop(stopTime-startTime);
						}
					}
				} finally {
					running.unlock();
				}
			}
		}

	}
	/*
	 * This function calculates if the message can be sent this simulation cycle or the next because of 
	 * limited bandwidth. It does not check if the devices are within range. 
	 * 
	 * If the message can be sent the receiving devices receive method is called and -1 returned
	 * otherwise the number of bits sent is returned
	 */
	private int sendTime(Device dst, Message msg, int bitsToSend) {
		synchronized (sendBuffer) {
			synchronized (ammountSent) {
				// calculate distance
				Double distance = currentPosition().distance(
						dst.currentPosition());
				// Time bits
				int sendTime = wirelessInterface.timeToSend(distance,
						bitsToSend);
				 if (agentTime + sendTime > startTime + stepTime) {
					// The devices could move closer or further apart causing
					// bitrate to change, send what we can. 
					
					int timeRemaning = (int) ((startTime + stepTime) - agentTime);
					int maxbitsSent = wirelessInterface.bitsSent(distance,
							timeRemaning);
					int actualBitsSent = getBandwidth(dst,maxbitsSent);
					// return the actual number of bits sent.
					//System.out.println("ActualBitsSent "+ actualBitsSent);
					return actualBitsSent;
				} else {
					// The message could be completely sent in this interval
					int actualBitsSent = getBandwidth(dst,bitsToSend);
					
						msg.recvtime = agentTime + sendTime;
						// give the message to the other device
					dst.receive(msg);
						// remove the msg from send buffers if exists.
						
					return actualBitsSent;
					
				}	
			}
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
			return remote;
		}else{
			dst.wirelessInterface.commitBandwidth(local);
			wirelessInterface.commitBandwidth(local);
			return local;
		}
		}
	}
	/*
	 * Checks that the receiving device is in range, an active connection exists, 
	 * and that there is not already a message waiting in the send buffer.
	 * 
	 */
	private void sendMessage(DeviceInterface a, Message msg) {
		Device dst = (Device) a;
		// System.out.println("Send To!");
		if (msg != null && inRange(dst) && connectedTo(dst)
				&& !sendingMessageTo(dst)) {

			// Time the message was sent
			msg.sendtime = agentTime;

			// System.out.println("Agent Time " +
			// Double.toString(msg.sendtime));

			int sentbits = sendTime(dst, msg, msg.size);
			if (sentbits == msg.size) {
				// Message was sent
				sendBuffer.remove(msg);
				ammountSent.remove(msg);
			} else {
				System.out.println("Could not send the message in this step");
				sendBuffer.add(msg);
				ammountSent.put(msg, sentbits);
			}
			System.out.println(sentbits + " of " + msg.size + " Sent");
			// System.out.println("Recv Time " + Double.toString(msg.recvtime));

			// Log the message as sent.
			synchronized (logSent) {
				logSent.add(msg);
			}

		} else {
			// Error sending message
			agentTime += 20;
		}
	}

	// Wrapper function for deviceInterface
	public void sendTo(DeviceInterface a, Message msg) throws SuspendExecution {
		// check that a connection exists
		if (connectedTo((Device) msg.dst)) {
			sendMessage(a, msg);
			checkGotTime();
		} else {
			return;
		}
	}

	// Wrapper function for deviceInterface
	public Message recv() throws SuspendExecution {
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
				if (recvBuffer.peek().recvtime < (startTime + stepTime)) {
					msg = recvBuffer.remove();
				} else {
					msg = null;
				}
			}
		}
		return msg;
	}

	

	// This is for the hack cast I'm aware, its bad but needed
	@SuppressWarnings("unchecked")
	public List<DeviceInterface> scan() throws SuspendExecution {
		// Time to perform a scan
		agentTime += wirelessInterface.scanTime();
		battery.wifiScan(wirelessInterface.scanTime());
		
		UrbanSim urbansim = (UrbanSim) simState;
		List<Device> inRange = urbansim.inRange(this, wirelessInterface);
		
		checkGotTime();
		// This is a hack cast, but should be OK
		return (List<DeviceInterface>) (List<?>) inRange;
	}

	public WirelessConnection getInterface() {
		return wirelessInterface;
	}

	/*
	 * Causes the agent to sleep at it current position of execution.Receiving a
	 * new message will wake up the agent.
	 */
	public void sleep() throws SuspendExecution {
		Coroutine.yield();
	}

	/*
	 * Increases agentTime by the corresponding amount. Causes the agent to not
	 * be runnable till after this time.
	 */
	public void sleep(int seconds) throws SuspendExecution {
		agentTime += seconds * stepTime;
		Coroutine.yield();
	}

	@Override
	public void coExecute() throws SuspendExecution {
		userAgent.main();
	}

	/*
	 * Wrapper function that connects to another device.
	 * 
	 */
	public boolean connect(DeviceInterface d) throws SuspendExecution {
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
		if(!sendingMessageTo((Device )d)){
			//Can only disconnect if not sending messages.
			removeConnection((Device) d);
			// This is a kind disconnect
			((Device) d).removeConnection(this);
			return true;
		}else{
			return false;
		}
		
		
	}

	
	@Override
	//Wrapper for activeCon() can't have locks in a method that throws suspendexecution
	public List<DeviceInterface> activeConnections() throws SuspendExecution {
		return activeCon();
	}

	/*
	 * Returns a list of all connections that the devices thinks are still
	 * active, does not check if the agents are still in range
	 */
	private List<DeviceInterface> activeCon() {
		List<DeviceInterface> connectedTo = new ArrayList<DeviceInterface>();
		connection.lock();
		try {
			for (Edge e : activeConnections) {
				connectedTo.add((DeviceInterface) e.getOtherNode(this));
			}
		} finally {
			connection.unlock();
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

	
	
	/*
	 * Called by a device than wants to connect to us.
	 * Could cause deadlocks when using syncronized to used trylock instead,
	 */
	public boolean acceptConnection(Device d) {
		boolean result;
		if (connection.tryLock()) {
			try {
				if(activeConnections.size() < wirelessInterface.maxConnections()){
					//Can accept connection
					Edge e = new Edge(this, d, new Double(0));
					activeConnections.add(e);			
					
					// UI updated by caller
					
					result = true;
				}else{
					result = false;
				}
			} finally {
				connection.unlock();
			}
		} else {
			result = false;
		}
		return result;
	}
	public boolean hasPower(){
		return battery.hasPower();
	}

	@Override
	public LongTermStorage getStorage() {
		return storage;
	}
	
	
	
	
}
