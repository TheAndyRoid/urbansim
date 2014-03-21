package urbansim;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.matthiasmann.continuations.*;
import it.polito.appeal.traci.StepAdvanceListener;
import it.polito.appeal.traci.Vehicle;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.network.Edge;
import urbansim.p2p.PeerComponent;
import urbansim.physical.PhysicalComponent;

public class Agent implements Steppable, PhysicalComponent, Device,
		CoroutineProto {

	public PhysicalComponent phy;
	public PeerComponent p2p;
	public Vehicle v;
	private Double2D positionActual; // actual
	private String agentType;
	private long agentID;
	private SimState simState;
	private DeviceAgent userAgent;

	private int range = 50;

	private long time = 0;

	private int sleepTime = 0;

	// java-continuation
	private Coroutine co;

	private Queue<Message> sendBuffer = new LinkedList<Message>();
	private Queue<Message> recvBuffer = new LinkedList<Message>();
	private List<Edge> activeConnections = new ArrayList<Edge>();

	private void checkGotTime() throws SuspendExecution {
		if (time >= 1000) {
			Coroutine.yield();
		}
	}

	public Agent(String type, long id, SimState state) {

		simState = state;
		this.agentType = type;
		this.agentID = id;
		userAgent = new MyAgent(this);
		co = new Coroutine(this);
		UrbanSim urbansim = (UrbanSim) simState;
		urbansim.connected.addNode(this);

	}

	public Agent(Element agentElement, SimState state) {

		simState = state;
		readAgent(agentElement, state);
		userAgent = new MyAgent(this);
		co = new Coroutine(this);
		UrbanSim urbansim = (UrbanSim) simState;
		urbansim.connected.addNode(this);
	}

	public String getType() {
		return agentType;
	}

	public long getID() {
		return agentID;
	}

	public String getName() {
		return new String(agentType + "@" + agentID);
	}

	public Agent(SimState state, Double2D position) {
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

	private void resetTime() {
		if (time <= 1000) {
			time = 0;
		} else {
			time -= 1000;
		}
	}

	// Called by MASON
	public void step(SimState state) {
		if (sleepTime == 0) {
			resetTime();
			co.run();
		} else {
			resetTime();
			sleepTime--;

		}
	}

	// Write to file
	static public void writeAgent(Agent agent, Element agentRoot, Document doc) {
		// create agent element
		Element agentElement = doc.createElement("agent");
		agentRoot.appendChild(agentElement);

		// set attributes
		agentElement.setAttribute("type", agent.getType());
		agentElement.setAttribute("id", Long.toString(agent.getID()));

		Element position = doc.createElement("position");

		position.setAttribute("x",
				Double.toString(agent.currentPosition().getX()));
		position.setAttribute("y",
				Double.toString(agent.currentPosition().getY()));

		agentElement.appendChild(position);

	}

	// read from file
	public void readAgent(Element eAgent, SimState state) {
		agentID = UrbanSim.readAttributeLong("id", eAgent);
		agentType = UrbanSim.readAttributeString("type", eAgent);
		Element ePos = (Element) eAgent.getElementsByTagName("position")
				.item(0);
		setPosition(state, UrbanSim.readAttributeDouble2D(ePos));

	}

	// Add message to send buffer
	private void receive(Message msg) {
		UrbanSim urbansim = (UrbanSim) simState;
		urbansim.observer.logMessage(msg);
		synchronized (recvBuffer) {
			// System.out.println("Device Got Message!");
			recvBuffer.add(msg);
		}

	}

	
	private void addConnection(Agent b) {
		UrbanSim urbansim = (UrbanSim) simState;
		Edge e = new Edge(this, b, new Double(0));
		synchronized (activeConnections) {
			activeConnections.add(e);
		}
		synchronized (urbansim.connected) {
			urbansim.connected.addEdge(e);
		}

	}

	private void removeConnection(Agent b) {
		UrbanSim urbansim = (UrbanSim) simState;
		Edge toRemove = findEdge(b);
		
		if (toRemove != null) {
			synchronized(activeConnections){
				activeConnections.remove(toRemove);
			}			
			synchronized (urbansim.connected) {
				urbansim.connected.removeEdge(toRemove);
			}
		}
	}
	
	private void resetConnections(){
		UrbanSim urbansim = (UrbanSim) simState;
		synchronized (activeConnections){
			activeConnections.clear();
		}
		synchronized (urbansim.connected) {
			urbansim.connected.removeNode(this);
		}
		
	}

	private Edge findEdge(Agent b) {
		Edge found = null;
		UrbanSim urbansim = (UrbanSim) simState;
		synchronized (activeConnections) {
			for (Edge e : activeConnections) {
				if (e.getOtherNode(this) == b) {
					found = e;					
					break;
				}
			}
		}
		return found;
	}

	private boolean inRange(Agent b) {
		UrbanSim urbansim = (UrbanSim) simState;
		Double2D aPos = this.currentPosition();

		if (aPos.distance(b.currentPosition()) <= range) {
			return true;
		} else {
			//That agent is out of range.
			removeConnection(b);
			return false;
		}
	}

	// Add message to send buffer
	public void sendTo(Device a, Message msg) throws SuspendExecution {
		
		Agent dst = (Agent) a;
		// System.out.println("Send To!");
		if (msg != null && inRange(dst)) {			
			dst.receive(msg);
			// Time to send a message
			time += 10;
		}else{
			//Error sending message
			time += 20;
		}
		checkGotTime();
	}

	public Message getMessage(Queue<Message> buffer) {
		Message msg;
		synchronized (buffer) {
			if (recvBuffer.isEmpty()) {
				// System.out.println("Empty Buffer");
				msg = null;
			} else {
				// System.out.println("Got a message");
				msg = recvBuffer.remove();

			}
		}
		return msg;
	}

	// Get message from receive buffer or return null
	public Message recv() throws SuspendExecution {
		// time to receive a message
		time += 15;
		checkGotTime();
		return getMessage(recvBuffer);
	}

	// This is for the hack cast I'm aware, its bad but needed
	@SuppressWarnings("unchecked")
	public List<Device> scan() throws SuspendExecution {
		// Time to perform a scan
		time += 250;

		UrbanSim urbansim = (UrbanSim) simState;
		List<Agent> inRange = urbansim.inRange(this);
		
		resetConnections();		
		for(Agent a:inRange){
			addConnection(a);
		}
		

		checkGotTime();
		// This is a hack cast, but should be OK
		return (List<Device>) (List<?>) inRange;
	}

	public void sleep() throws SuspendExecution {
		Coroutine.yield();
	}

	public void sleep(int seconds) throws SuspendExecution {
		sleepTime = seconds;
		Coroutine.yield();
	}

	@Override
	public void coExecute() throws SuspendExecution {
		userAgent.main();
	}

}
