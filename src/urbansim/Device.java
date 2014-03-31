package urbansim;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
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
import sim.util.*;
import sim.field.continuous.*;
import sim.field.network.Edge;
import urbansim.p2p.DeviceAgent;
import urbansim.physical.DeviceInterface;
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
	private long agentID;
	private SimState simState;
	private DeviceAgent userAgent;
	private WirelessConnection wirelessInterface;

	private int range = 50;

	private Lock running = new ReentrantLock();
	private Lock connection = new ReentrantLock();

	private double startTime = 0;
	private double agentTime = 0;

	private int sleepTime = 0;

	// java-continuation
	private Coroutine co;

	private Queue<Message> sendBuffer = new LinkedList<Message>();
	private Queue<Message> recvBuffer = new LinkedList<Message>();

	private List<Message> logSent = new ArrayList<Message>();
	private List<Message> logReceived = new ArrayList<Message>();

	private List<Edge> activeConnections = new ArrayList<Edge>();

	private void checkGotTime() throws SuspendExecution {
		if (agentTime > startTime + 1000) {
			Coroutine.yield();
		}
	}

	// Simple constructor
	/*
	 * public Device(String type, long id, SimState state) {
	 * 
	 * simState = state; this.agentType = type; this.agentID = id; userAgent =
	 * new MyAgent(this); co = new Coroutine(this); UrbanSim urbansim =
	 * (UrbanSim) simState; urbansim.connected.addNode(this);
	 * 
	 * }
	 */

	// Load static device
	public Device(Element agentElement, Long id, SimState state,
			File deviceType, Map<String, File> agentTypes,
			Map<String, File> interfaceTypes,Map<String, File> agentData) {
		this(id, state, deviceType, agentTypes, interfaceTypes,agentData);
		// Load the position
		Element ePos = (Element) agentElement.getElementsByTagName("position")
				.item(0);
		setPosition(state, Utils.readAttributeDouble2D(ePos));
	}

	// Load from sumo
	public Device(Long id, SimState state, File deviceType,
			Map<String, File> agentTypes, Map<String, File> interfaceTypes,Map<String, File> agentData) {
		agentID = id;
		simState = state;

		// Read agent and interface in.
		readAgent(deviceType, agentTypes, interfaceTypes,agentData);

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

	public String getName() {
		String tmp = new String(deviceType + "@" + agentType + "@" + agentID);
		// System.out.println(tmp);
		return tmp;
	}

	public Device(SimState state, Double2D position) {
		UrbanSim urbansim = (UrbanSim) state;
		setPosition(state, position);
	}

	public Double2D currentPosition() {
		return positionActual;
	}

	public void setPosition(SimState state, Double2D newPosition) {
		positionActual = newPosition;
		UrbanSim urbansim = (UrbanSim) state;
		Double2D pos = new Double2D(newPosition.getX(), newPosition.getY());
		urbansim.agentPos.setObjectLocation(this, pos);

	}

	private void time() {
		// get time
		startTime = simState.schedule.getTime() * 1000;
		if (agentTime < startTime) {
			agentTime = startTime;
		}
	}

	// Called by MASON
	public void step(SimState state) {
		time();
		if (agentTime < startTime + 1000) {
			running.lock();
			try {
				if (co.getState() != Coroutine.State.RUNNING
						&& co.getState() != Coroutine.State.FINISHED) {
					do {
						co.run();
					} while ((agentTime < startTime + 1000)
							&& !recvBuffer.isEmpty());
					// update connections ui
					// updateUI();
				}

			} finally {
				running.unlock();
			}
		}

	}

	// read from file
	public void readAgent(File deviceType, Map<String, File> agentTypes,
			Map<String, File> interfaceTypes,Map<String, File> agentData) {
		// agentID = Utils.readAttributeLong("id", eAgent);
		File agent = null;
		File deviceInterface = null;

		// Read the device file
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(deviceType);

			doc.getDocumentElement().normalize();

			// Element e = doc.getDocumentElement();

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

		// Create Interface

		wirelessInterface = new WirelessConnection(deviceInterface);

		// System.out.println(wirelessInterface.connectionTime());

	}

	private void connected(){
		
	}
	
	
	
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
					activeConnections.add(e);

					// need to update the ui
					UrbanSim urbansim = (UrbanSim) simState;
					synchronized (urbansim.connected) {
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

	public boolean connectedTo(Device d) {
		if (findEdge(d) != null) {
			return true;
		} else {
			return false;
		}
	}

	public void removeConnection(Device b) {
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
		}
	}

	private void resetConnections() {
		UrbanSim urbansim = (UrbanSim) simState;
		connection.lock();
		try {
			activeConnections.clear();
		} finally {
			connection.unlock();
		}
		synchronized (urbansim.connected) {
			urbansim.connected.removeNode(this);
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

	//
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
		if (msg.recvtime < (startTime + 1000)) {
			if (running.tryLock()) {
				try {
					time();
					if (agentTime < startTime + 1000) {
						if (co.getState() != Coroutine.State.RUNNING
								&& co.getState() != Coroutine.State.FINISHED) {

							co.run();
						}
					}
				} finally {
					running.unlock();
				}
			}
		}

	}

	private void sendMessage(DeviceInterface a, Message msg) {
		Device dst = (Device) a;
		// System.out.println("Send To!");
		if (msg != null && inRange(dst) && connectedTo(dst)) {

			// Time the message was sent
			msg.sendtime = agentTime;

			// System.out.println("Agent Time " +
			// Double.toString(msg.sendtime));

			// calculate distance
			Double distance = currentPosition().distance(dst.currentPosition());
			// Time to send a message
			agentTime += wirelessInterface.timeToSend(distance, msg.size);
			/*
			 * System.out.println("Transmit Time " +
			 * Double.toString(wirelessInterface.timeToSend(distance,
			 * msg.size)));
			 */
			// time the message should be received
			msg.recvtime = agentTime;

			// System.out.println("Recv Time " + Double.toString(msg.recvtime));

			// Log the message as sent.
			synchronized (logSent) {
				logSent.add(msg);
			}

			// give the message to the other device
			dst.receive(msg);

		} else {
			// Error sending message
			agentTime += 20;
		}
	}

	// Add message to send buffer
	public void sendTo(DeviceInterface a, Message msg) throws SuspendExecution {
		// check that a connection exists
		if (connectedTo((Device) msg.dst)) {
			sendMessage(a, msg);
			checkGotTime();
		} else {
			return;
		}
	}

	public Message getMessage(Queue<Message> buffer) {
		Message msg;
		synchronized (recvBuffer) {
			if (recvBuffer.isEmpty()) {
				// System.out.println("Empty Buffer");
				msg = null;
			} else {
				// System.out.println("Got a message");
				if (recvBuffer.peek().recvtime < (startTime + 1000)) {
					msg = recvBuffer.remove();
				} else {
					msg = null;
				}
			}
		}
		return msg;
	}

	// Get message from receive buffer or return null
	public Message recv() throws SuspendExecution {
		// time to receive a message
		agentTime += 10;
		checkGotTime();
		return getMessage(recvBuffer);
	}

	// This is for the hack cast I'm aware, its bad but needed
	@SuppressWarnings("unchecked")
	public List<DeviceInterface> scan() throws SuspendExecution {
		// Time to perform a scan
		agentTime += wirelessInterface.scanTime();

		UrbanSim urbansim = (UrbanSim) simState;
		List<Device> inRange = urbansim.inRange(this, wirelessInterface);

		checkGotTime();
		// This is a hack cast, but should be OK
		return (List<DeviceInterface>) (List<?>) inRange;
	}

	public WirelessConnection getInterface() {
		return wirelessInterface;
	}

	public void sleep() throws SuspendExecution {
		Coroutine.yield();
	}

	public void sleep(int seconds) throws SuspendExecution {
		agentTime += seconds * 1000;
		Coroutine.yield();
	}

	@Override
	public void coExecute() throws SuspendExecution {
		userAgent.main();
	}

	public boolean connect(DeviceInterface d) throws SuspendExecution {
		agentTime += wirelessInterface.connectionTime();
		checkGotTime();
		if(addConnection((Device) d) ){
			return true;
		}else{
			return false;
		}
	}

	@Override
	public void disconnect(DeviceInterface d) {
		removeConnection((Device) d);
		// This is a kind disconnect
		((Device) d).removeConnection(this);
	}

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
	public List<DeviceInterface> activeConnections() throws SuspendExecution {

		return activeCon();
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

	public boolean acceptConnection(Device d) {
		boolean result;
		if (connection.tryLock()) {
			try {
				if(activeConnections.size() < wirelessInterface.maxConnections()){
					//Can accept connection
					Edge e = new Edge(this, d, new Double(0));
					activeConnections.add(e);			
					
					// need to update the ui
					UrbanSim urbansim = (UrbanSim) simState;
					synchronized (urbansim.connected) {
						urbansim.connected.addEdge(e);
					}
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
}
