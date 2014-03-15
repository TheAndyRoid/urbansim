package urbansim;

import java.awt.geom.Point2D;
import java.io.IOException;

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

	public Agent() {

	}

	public Agent(SimState state, Double2D position) {
		UrbanSim urbansim = (UrbanSim) state;
		setPosition(state, position);
	}

	public Double2D currentPosition(SimState state) {
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

}
