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

	private int sleepTime = 0;

	// java-continuation
	private Coroutine co;

	private Queue<Message> sendBuffer = new LinkedList<Message>();
	private Queue<Message> recvBuffer = new LinkedList<Message>();
	

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

	// Called by MASON
	public void step(SimState state) {
		if (sleepTime == 0) {
			co.run();
		} else {
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
			//System.out.println("Device Got Message!");
			recvBuffer.add(msg);
		}

	}

	// Add message to send buffer
	public void sendTo(Device a, Message msg) {
		// System.out.println("Send To!");
		if (msg != null) {
			Agent dst = (Agent) a;
			dst.receive(msg);
		}
	}

	// Get message from receive buffer or return null
	public Message recv() {
		Message msg;
		synchronized (recvBuffer) {
			if (recvBuffer.isEmpty()) {
				//System.out.println("Empty Buffer");
				msg = null;
			} else {
				//System.out.println("Got a message");
				msg = recvBuffer.remove();
				
			}
		}
		return msg;
	}

	// This is for the hack cast I'm aware, its bad but needed
	@SuppressWarnings("unchecked")
	public List<Device> scan() {
		UrbanSim urbansim = (UrbanSim) simState;
		List<Agent> inRange = urbansim.inRange(this);
		// Have to sync on network or bad things happen
		synchronized (urbansim.connected) {
			// clear the old connections
			urbansim.connected.removeNode(this);
			// add ourself back to the network
			urbansim.connected.addNode(this);
			// recreate edges
			for (Agent a : inRange) {
				urbansim.connected.addEdge(this, a, new Double(0));
			}
		}
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
