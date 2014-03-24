package urbansim;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;




import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

public class Agent extends ToXML implements Steppable, PhysicalComponent, Device,
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

	private Lock running = new ReentrantLock();
	
	private double startTime;
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
		if (agentTime >= startTime + 1000) {
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


	private void time(){
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
					updateUI();
				}

			} finally {
				running.unlock();
			}
		}

	}
	
	

	// read from file
	public void readAgent(Element eAgent, SimState state) {
		agentID = UrbanSim.readAttributeLong("id", eAgent);
		agentType = UrbanSim.readAttributeString("type", eAgent);
		Element ePos = (Element) eAgent.getElementsByTagName("position")
				.item(0);
		setPosition(state, UrbanSim.readAttributeDouble2D(ePos));

	}

	

	private boolean addConnection(Agent b) {

		Edge e = new Edge(this, b, new Double(0));
		synchronized (activeConnections) {
			activeConnections.add(e);
		}
		return true;
	}

	public void removeConnection(Agent b) {
		Edge toRemove = findEdge(b);

		if (toRemove != null) {
			synchronized (activeConnections) {
				activeConnections.remove(toRemove);
			}
		}
	}

	private void resetConnections() {
		UrbanSim urbansim = (UrbanSim) simState;
		synchronized (activeConnections) {
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
					//System.out.println("Found edge");
					found = e;
					break;
				}
			}
		}
		return found;
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

	private boolean inRange(Agent b) {
		UrbanSim urbansim = (UrbanSim) simState;
		Double2D aPos = this.currentPosition();

		if (aPos.distance(b.currentPosition()) <= range) {
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

		// Check if this device can be started if not already running.
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
	
	private void sendMessage(Device a, Message msg) {
		Agent dst = (Agent) a;
		// System.out.println("Send To!");
		if (msg != null && inRange(dst)) {

			//Time the message was sent
			msg.sendtime = agentTime; 
			// Time to send a message
			agentTime+= 10;		
			
			//time the message should be received
			msg.recvtime = agentTime ;
			
			//Log the message as sent.
			synchronized(logSent){
				logSent.add(msg);
			}

			
			//give the message to the other device
			dst.receive(msg);
			

		} else {
			// Error sending message
			agentTime += 20;
		}
	}
	
	
	// Add message to send buffer
	public void sendTo(Device a, Message msg) throws SuspendExecution {
		sendMessage(a,msg);
		checkGotTime();
	}

	public Message getMessage(Queue<Message> buffer) {
		Message msg;
		synchronized (recvBuffer) {
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
		agentTime += 10;
		checkGotTime();
		return getMessage(recvBuffer);
	}

	// This is for the hack cast I'm aware, its bad but needed
	@SuppressWarnings("unchecked")
	public List<Device> scan() throws SuspendExecution {
		// Time to perform a scan
		agentTime += 200;

		UrbanSim urbansim = (UrbanSim) simState;
		List<Agent> inRange = urbansim.inRange(this, range);

		checkGotTime();
		// This is a hack cast, but should be OK
		return (List<Device>) (List<?>) inRange;
	}

	public void sleep() throws SuspendExecution {
		Coroutine.yield();
	}

	public void sleep(int seconds) throws SuspendExecution {
		agentTime += seconds *1000;				
		Coroutine.yield();
	}

	@Override
	public void coExecute() throws SuspendExecution {
		userAgent.main();
	}

	public boolean connect(Device d) throws SuspendExecution {
		agentTime += 5;
		checkGotTime();
		return addConnection((Agent) d);
	}

	
	
	@Override
	public void disconnect(Device d) {
		removeConnection((Agent) d);
		//This is a kind disconnect
		((Agent)d).removeConnection(this);
	}

	private List<Device> activeCon() {
		List<Device> connectedTo = new ArrayList<Device>();
		synchronized (activeConnections) {
			for (Edge e : activeConnections) {
				connectedTo.add((Device) e.getOtherNode(this));
			}
		}
		return connectedTo;
	}

	@Override
	public List<Device> activeConnections() throws SuspendExecution {

		return activeCon();
	}

	@Override
	public Element toXML(Element root, Document doc) {
		// Write to file
		
			// create agent element
			Element agentElement = doc.createElement("agent");
			root.appendChild(agentElement);

			// set attributes
			agentElement.setAttribute("type", getType());
			agentElement.setAttribute("id", Long.toString(getID()));

			Element position = doc.createElement("position");

			position.setAttribute("x",
					Double.toString(currentPosition().getX()));
			position.setAttribute("y",
					Double.toString(currentPosition().getY()));

			agentElement.appendChild(position);
			
			Element elogSent = doc.createElement("Sent");
			agentElement.appendChild(elogSent);
			for(Message m:logSent){
				m.isSender = true;
				m.toXML(elogSent,doc);				
			}
			logSent.clear();
			
			
			Element elogRecv = doc.createElement("Recv");
			agentElement.appendChild(elogRecv);
			for(Message m:logReceived){
				m.isSender=false;
				m.toXML(elogRecv,doc);
			}
			logReceived.clear();
			return agentElement;
		
	}

	

}
