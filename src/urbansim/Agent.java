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

public class Agent implements Steppable, PhysicalComponent{

	public PhysicalComponent phy;
	public PeerComponent p2p;
	public Vehicle v;

	public Agent(){
		
	}
	public Agent(SimState state, Double2D position) {
		UrbanSim urbansim = (UrbanSim) state;
		urbansim.agentPos.setObjectLocation(this, position);
	}

	public Double2D currentPosition(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;
		return urbansim.agentPos.getObjectLocation(this);
	}

	public void setPosition(SimState state, Double2D newPosition) {
		UrbanSim urbansim = (UrbanSim) state;
		urbansim.agentPos.setObjectLocation(this, newPosition);
	}

	// Called by MASON
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;
		//System.out.println("step");

	}

	

}
