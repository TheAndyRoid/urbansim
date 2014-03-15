package urbansim;

import it.polito.appeal.traci.Vehicle;
import it.polito.appeal.traci.VehicleLifecycleObserver;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.SparseGrid2D;

public class UrbanSim extends SimState implements VehicleLifecycleObserver,
		Steppable {

	private void getBounds() {

	}

	// Width and height are ignored
	private int GRID_WIDTH = 100, GRID_HEIGHT = 100;

	// data
	public Continuous2D agentPos = new Continuous2D(1.0, 500, 500);
	public int numAgents = 5;
	public ParallelSequence SAgents;
	public TraCI traci;
	public Observer observer;
	// Array of all agents
	Agent[] agents;
	
	//Configuration Options
	private String caseDir;
	private String sumoFile;
	private String sumoServer;
	private String staticAgentFile;
	private String agentConfigFile;
	private String saveDirectory;
	private int simulationDurationSeconds;
	private int stepDelta;
	private int deltasPerFile;
	
	
	
	
	
	
	
	
	

	// Map SUMO strings to agents
	public Map<String, Agent> mobileAgents = new HashMap<String, Agent>();
	public List<Agent> stationaryAgents = new ArrayList<Agent>();
	public List<Agent> allAgents = new ArrayList<Agent>();

	public UrbanSim(long seed) {

		super(seed);
		// agents = new Agent[numAgents];
	}

	// Setup the simulation here
	public void start() {
		super.start();
		schedule.reset();

		agentPos.clear();
		mobileAgents.clear();
		stationaryAgents.clear();
		allAgents.clear();
		if (traci != null) {
			traci.close();
			System.out.println("Close");
		}

		// Read in simulation settings
		readSimulationSettings("/home/andyroid/uni/cs4526/Application/test/caseFile/case.xml");

		// Create the motion aware stepper
		traci = new TraCI();
		traci.addVehicleLifecycleObserver(this);

		// Create observer
		observer = new Observer(deltasPerFile,caseDir,this);

		step(this);

		schedule.scheduleRepeating(this);

		System.out.println();
	}

	public static void main(String[] args) {
		doLoop(UrbanSim.class, args);
		System.exit(0);

	}

	// create the schedule
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;

		// Update Positions
		state.schedule.scheduleOnce(urbansim.traci);

		// Must simulate atleast one Agent
		if (urbansim.allAgents.size() > 0) {
			// Create agent array for parallel
			agents = urbansim.allAgents.toArray(new Agent[urbansim.allAgents
					.size()]);

			// create parallel for faster processing
			urbansim.SAgents = new ParallelSequence(agents, 8);

			// Step Agents
			urbansim.schedule.scheduleOnce(urbansim.SAgents);
		}
		// Log Data
		 state.schedule.scheduleOnce(urbansim.observer);

	}

	@Override
	// Vehicle was created by sumo
	public void vehicleDeparted(Vehicle vehicle) {
		System.out.println("new Vehicle:" + vehicle.getID());
		// Get position
		Point2D pos = new Point2D.Double();

		// Create new agent
		Agent tmp = new Agent();
		tmp.v = vehicle;

		// Add agent to mobile agent list
		mobileAgents.put(vehicle.getID(), tmp);

		// Add agent to all list
		allAgents.add(tmp);

	}

	// Vehicle was destroyed by sumo
	public void vehicleArrived(Vehicle vehicle) {
		System.out.println("dead Vehicle");
		// Remove from arrays
		Agent tmp = mobileAgents.remove(vehicle.getID());
		allAgents.remove(tmp);

		// Remove from graphic
		agentPos.remove(tmp);

	}

	@Override
	public void vehicleTeleportStarting(Vehicle vehicle) {
		// TODO Auto-generated method stub
	}

	@Override
	public void vehicleTeleportEnding(Vehicle vehicle) {
		// TODO Auto-generated method stub

	}

	private int readInt(String name,Element root){		
		return Integer.parseInt(root.getElementsByTagName(name).item(0).getTextContent());		
	}
	private String readString(String name,Element root){		
		return root.getElementsByTagName(name).item(0).getTextContent();		
	}
	
	
	private boolean readSimulationSettings(String filePath) {

		try {

			File fXmlFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();

			//read in the elements
			caseDir  = readString("caseDir",root);
			sumoFile  = readString("sumoFile",root);
			sumoServer  = readString("sumoServer",root);
			deltasPerFile  = readInt("deltasPerFile",root);
			System.out.println(deltasPerFile);
			
			staticAgentFile = readString("staticAgentFile",root);
			agentConfigFile= readString("agentConfigFile",root);
			simulationDurationSeconds= readInt("simulationDurationSeconds",root);
			stepDelta= readInt("stepDelta",root);		
		
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;

	}

}
