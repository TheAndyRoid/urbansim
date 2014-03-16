package urbansim;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import it.polito.appeal.traci.StepAdvanceListener;
import it.polito.appeal.traci.Vehicle;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import urbansim.p2p.PeerComponent;
import urbansim.physical.PhysicalComponent;

public class Agent implements Steppable, PhysicalComponent {

	public PhysicalComponent phy;
	public PeerComponent p2p;
	public Vehicle v;
	private Double2D positionActual; // actual
	private String agentType;
	private long agentID;

	public Agent(String type, long id) {
		this.agentType = type;
		this.agentID = id;
	}

	public Agent(Element agentElement,SimState state) {
		readAgent(agentElement,state);
	}

	public String getType() {
		return agentType;
	}

	public long getID() {
		return agentID;
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

		urbansim.agentPos.setObjectLocation(this,
				new Double2D(newPosition.getX(), newPosition.getY()));
	}

	// Called by MASON
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;
		// System.out.println("step");

	}

	// Write
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

	// read
	public void readAgent(Element eAgent, SimState state) {
		agentID = UrbanSim.readAttributeLong("id",eAgent);
		agentType = UrbanSim.readAttributeString("type",eAgent);
		Element ePos = (Element) eAgent.getElementsByTagName("position")
				.item(0);
		setPosition(state,UrbanSim.readAttributeDouble2D(ePos));
		

	}

}
