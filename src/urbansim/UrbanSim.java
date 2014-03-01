package urbansim;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public class UrbanSim extends SimState {

	// data
	public Continuous2D map = new Continuous2D(1.0, 100, 100);
	public int numAgents = 80000;
	public ParallelSequence SAgents;

	public UrbanSim(long seed) {

		super(seed);
		agents= new Agent[numAgents];
	}

	Agent[] agents;
	
	// Setup the simulation here
	public void start() {
		super.start();

		// Clear the area
		map.clear();
		
		
		
		//add  agents to the
		for(int i = 0; i <numAgents; i++){
			Agent agent = new Agent();
			System.out.print(Integer.toString(i)+",");
			map.setObjectLocation(agent,
					new Double2D(map.getWidth()*0.5 + random.nextDouble()-0.5,
							map.getHeight()*0.5 + random.nextDouble()-0.5)
			);
			agents[i] = agent;
			
		}
		//create parralel
		SAgents = new ParallelSequence(agents,8);
		
		
		//Setup the call schedule
		schedule.scheduleRepeating(SAgents);
		
		
		
		System.out.println();
	}

	public static void main(String[] args) {
		doLoop(UrbanSim.class, args);
		System.exit(0);

	}

}
