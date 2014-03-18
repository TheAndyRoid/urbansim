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
import sim.field.network.Network;

public class UrbanSim extends SimState implements VehicleLifecycleObserver,
		Steppable {

	private void getBounds() {

	}
	// Width and height are ignored
	private int GRID_WIDTH = 100, GRID_HEIGHT = 100;

	// data
	public Continuous2D agentPos = new Continuous2D(1.0, 500, 500);
	//for displaying connection between devices
	public Network connected = new Network(false);

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
	private long nextAgentID = 0;	
	

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

		//Read in static agents
		loadStaticAgent(staticAgentFile);
		
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
			urbansim.SAgents = new ParallelSequence(agents, 16);

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
		Agent tmp = new Agent("mobile",nextAgentID,this);
		nextAgentID++;
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
		connected.removeNode(tmp);
		

	}

	@Override
	public void vehicleTeleportStarting(Vehicle vehicle) {
		// TODO Auto-generated method stub
	}

	@Override
	public void vehicleTeleportEnding(Vehicle vehicle) {
		// TODO Auto-generated method stub

	}

	static public int readElementInt(String name,Element root){		
		return Integer.parseInt(root.getElementsByTagName(name).item(0).getTextContent());		
	}
	static public String readElementString(String name,Element root){		
		return root.getElementsByTagName(name).item(0).getTextContent();		
	}
	static public Double readElementDouble(String name,Element root){		
		return Double.parseDouble(root.getElementsByTagName(name).item(0).getTextContent());		
	}
	
	static public Long readAttributeLong(String name,Element root){		
		return Long.parseLong(root.getAttribute(name));		
	}
	
	static public String readAttributeString(String name,Element root){		
		return root.getAttribute(name);		
	}
	
	static public Double readAttributeDouble(String name,Element root){		
		return Double.parseDouble(root.getAttribute(name));		
	}
	static public Double2D readAttributeDouble2D(Element root){
		return new Double2D(
				readAttributeDouble("x",root),
				readAttributeDouble("y",root)				
		 );		
	}
	
	
	
	
	private void loadStaticAgent(String filePath){
		try {

			File fXmlFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("agent");	
			
			for(int i = 0; i<nList.getLength();i++){
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) nNode;
					
					// Create new agent
					Agent tmp = new Agent(e,this);
					
					// Add agent to static agent list
					stationaryAgents.add(tmp);

					// Add agent to all list
					allAgents.add(tmp);
				}				
			}
		
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		
		
		
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
			caseDir  = readElementString("caseDir",root);
			sumoFile  = readElementString("sumoFile",root);
			sumoServer  = readElementString("sumoServer",root);
			deltasPerFile  = readElementInt("deltasPerFile",root);
			System.out.println(deltasPerFile);
			
			staticAgentFile = caseDir +"/" +readElementString("staticAgentFile",root);
			agentConfigFile= readElementString("agentConfigFile",root);
			simulationDurationSeconds= readElementInt("simulationDurationSeconds",root);
			stepDelta= readElementInt("stepDelta",root);		
		
			

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;

	}
	//Calculate agents in range
	public List<Agent> inRange(Agent agent){
		List<Agent> agentsInRange = new ArrayList<Agent>();
		Double2D aPos = agent.currentPosition();
		double dist = 50;
		for(Agent a:allAgents){
			if(aPos.distance(a.currentPosition())<=dist && a != agent){
				agentsInRange.add(a);
			}
		}
		return agentsInRange;
	}
	
	

}
