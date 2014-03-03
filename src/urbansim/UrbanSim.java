package urbansim;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.SparseGrid2D;

public class UrbanSim extends SimState {

	private void getBounds() {

	}

	// Width and height are ignored
	private int GRID_WIDTH = 100, GRID_HEIGHT = 100;

	// data
	public Continuous2D agentPos = new Continuous2D(1.0, 10, 10);
	public int numAgents = 5;
	public ParallelSequence SAgents;

	public UrbanSim(long seed) {

		super(seed);
		agents = new Agent[numAgents];
	}

	Agent[] agents;

	// Setup the simulation here
	public void start() {
		super.start();
		
		agentPos.clear();
		//add  agents to the
		for(int i = 0; i <numAgents; i++){			
			agents[i] = new Agent(this,	new Double2D(
					random.nextDouble()*agentPos.getWidth(),
					 random.nextDouble() * agentPos.getHeight()
				));
			System.out.print(Integer.toString(i)+",");	
			
			
			
			
			
		}
		//create parallel
		SAgents = new ParallelSequence(agents,1);
		
		
		//Setup the call schedule
		schedule.scheduleRepeating(SAgents);
		
		
		
		System.out.println();
	}

	public static void main(String[] args) {
		doLoop(UrbanSim.class, args);
		System.exit(0);

	}

}
