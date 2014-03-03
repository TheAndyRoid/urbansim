package urbansim;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import urbansim.p2p.PeerComponent;
import urbansim.physical.PhysicalComponent;

public class Agent implements Steppable,PhysicalComponent {

	public PhysicalComponent phy;
	public PeerComponent p2p;



	public Agent(SimState state, Double2D position) {
				UrbanSim urbansim = (UrbanSim) state;		
				urbansim.agentPos.setObjectLocation(this,position);		
		System.out.println("Agent Init");
	}

	public Double2D currentPosition(SimState state){
		UrbanSim urbansim = (UrbanSim) state;		
		return  urbansim.agentPos.getObjectLocation(this);		
	}
	public void setPosition(SimState state,Double2D newPosition){
		UrbanSim urbansim = (UrbanSim) state;
		urbansim.agentPos.setObjectLocation(this,newPosition);		
	}	
		
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;
		// System.out.println("step");
		
		//get old position
		Double2D oldPos = currentPosition(state);
		Double2D newPos = new Double2D(
					oldPos.x + 0.01,
					oldPos.y + 0.01);
		setPosition(state,newPos);
		
	}

}
