package urbansim;

import it.polito.appeal.traci.Vehicle;
import it.polito.appeal.traci.VehicleLifecycleObserver;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.SparseGrid2D;

public class UrbanSim extends SimState implements VehicleLifecycleObserver {

	private void getBounds() {

	}

	// Width and height are ignored
	private int GRID_WIDTH = 100, GRID_HEIGHT = 100;

	// data
	public Continuous2D agentPos = new Continuous2D(1.0, 500, 500);
	public int numAgents = 5;
	public ParallelSequence SAgents;
	public TraCI traci;
	
	//Map SUMO strings to agents
	public Map<String,Agent> mobileAgents = new HashMap<String,Agent>();
	public List<Agent> stationaryAgents = new ArrayList<Agent>();
	
	public UrbanSim(long seed) {	

		super(seed);
		agents = new Agent[numAgents];
	}
	
	//Array of all agents
	Agent[] agents;

	// Setup the simulation here
	public void start() {
		super.start();
		schedule.clear();
		
		agentPos.clear();	
		mobileAgents.clear();
		stationaryAgents.clear();
		if(traci != null){
			traci.close();
			System.out.println("Close");
		}
		
		
		
		//Create the motion aware stepper
		traci = new TraCI();
		traci.addVehicleLifecycleObserver(this);
		
		schedule.scheduleRepeating(Schedule.EPOCH,traci);
		
		
		System.out.println();
	}

	public static void main(String[] args) {
		doLoop(UrbanSim.class, args);
		System.exit(0);

	}
	
	//re-create the schedule
	private void reshedule(){
		schedule.clear();
		
		
		Agent[] magent = mobileAgents.values().toArray(new Agent[mobileAgents.size()]); 
		Agent[] sagent = stationaryAgents.toArray(new Agent[stationaryAgents.size()]);
		//Create agent array for parallel
		agents = new Agent[magent.length + sagent.length];
		
		System.arraycopy(magent,0,agents,0,magent.length);
		System.arraycopy(sagent,0,agents,magent.length,sagent.length);
				
		
		//create parallel
		SAgents = new ParallelSequence(agents,8);
		
		
		//Setup the call schedule for all agents
		schedule.scheduleRepeating(SAgents);
		
		
		schedule.scheduleRepeating(traci);
		
	}
	
	
	
	@Override
	//Vehicle was created by sumo
	public void vehicleDeparted(Vehicle vehicle) {
		System.out.println("new Vehicle:" + vehicle.getID());
		//Get position
		Point2D pos = new Point2D.Double();		
		//Create new agent
		Agent tmp = new Agent();		
		tmp.v = vehicle;
		//Add agent to mobile agent list
		mobileAgents.put(vehicle.getID(),tmp);		
		reshedule();
		
	}

	//Vehicle was destroyed by sumo
	public void vehicleArrived(Vehicle vehicle) {
		System.out.println("dead Vehicle");
		Agent tmp = mobileAgents.remove(vehicle.getID());
		//Remove from graphic
		agentPos.remove(tmp);
		reshedule();
	}

	@Override
	public void vehicleTeleportStarting(Vehicle vehicle) {
		// TODO Auto-generated method stub		
	}
	@Override
	public void vehicleTeleportEnding(Vehicle vehicle) {
		// TODO Auto-generated method stub
		
	}
	

}
