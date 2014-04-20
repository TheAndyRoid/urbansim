package urbansim;


import it.polito.appeal.traci.Vehicle;
import it.polito.appeal.traci.VehicleLifecycleObserver;

import java.awt.geom.Point2D;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import urbansim.physical.WirelessConnection;

public class UrbanSim extends SimState implements VehicleLifecycleObserver,
		Steppable {

	
	// Width and height are ignored
	//private int GRID_WIDTH = 100, GRID_HEIGHT = 100;
	
	


	// data
	public Continuous2D agentPos = new Continuous2D(1, 600, 600);
	//for displaying connection between devices
	public Network connected = new Network(false);

	public int numAgents = 5;
	public ParallelSequence SAgents;
	public TraCI traci;
	public Observer observer;
	public UI ui= new UI();
	// Array of all agents
	Device[] agents;
	
	
	//Lock for connections
	public Lock connection = new ReentrantLock();
	
	
	public String roadNetwork;
	
	//Configuration Options
	private String caseDir;
	private String sumoFile;
	private String sumoServer;
	private String staticAgentFile;
	
	private String agentTypeDir;
	private String deviceTypeDir;
	private String interfaceTypeDir;
	private String agentDataDir;
	private String batteryTypeDir;
	private String storageTypeDir;
	
	private Map<String,File> agentTypes = new HashMap<String,File>();
	private Map<String,File> deviceTypes= new HashMap<String,File>();
	private Map<String,File> interfaceTypes = new HashMap<String,File>();
	private Map<String,File> agentData = new HashMap<String,File>();
	private Map<String,File> batteryTypes = new HashMap<String,File>();
	private Map<String,File> storageTypes = new HashMap<String,File>();
	
	private Map<String,Long> id = new HashMap<String,Long>();
	
	private String saveDirectory;
	private int simulationDurationSeconds;
	private int deltasPerFile;
	private long nextAgentID = 0;	
	

	// Map SUMO strings to agents
	public Map<String, Device> mobileAgents = new HashMap<String, Device>();
	public List<Device> stationaryAgents = new ArrayList<Device>();
	public List<Device> allAgents = new ArrayList<Device>();

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
		observer = new Observer(deltasPerFile,saveDirectory,this);

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
			agents = urbansim.allAgents.toArray(new Device[urbansim.allAgents
					.size()]);

			// create parallel for faster processing
			urbansim.SAgents = new ParallelSequence(agents, 12);

			// Step Agents
			urbansim.schedule.scheduleOnce(urbansim.SAgents);
		}
		
		//Update the ui
		state.schedule.scheduleOnce(urbansim.ui);
		
		// Log Data
		 state.schedule.scheduleOnce(urbansim.observer);

	}

	@Override
	// Vehicle was created by sumo
	public void vehicleDeparted(Vehicle vehicle) {
		System.out.println("new Vehicle:" + vehicle.getID());
		// Get position
		Point2D pos = new Point2D.Double();
		
		//TODO get vehicle type from sumo.

		String type = new String("car");
		
		File dev = deviceTypes.get(type);
		if(dev != null){
		// Create new agent
		Device tmp = new Device(nextAgentID,
								this,
								dev,
								agentTypes,
								interfaceTypes,
								agentData,
								batteryTypes,
								storageTypes);
		
		// Create new agent
				//Device tmp = new Device("mobile",nextAgentID,this);
				nextAgentID++;
				tmp.v = vehicle;
				
				// Add agent to mobile agent list
				mobileAgents.put(vehicle.getID(), tmp);

				// Add agent to all list
				allAgents.add(tmp);
		}
		
		
		

		

	}

	// Vehicle was destroyed by sumo
	public void vehicleArrived(Vehicle vehicle) {
		System.out.println("dead Vehicle");
		// Remove from arrays
		Device tmp = mobileAgents.remove(vehicle.getID());
		allAgents.remove(tmp);
		
		tmp.resetConnections();
		tmp.coRun = false;
		
		//remove connection
		for(Device d:allAgents){
			d.removeConnection(tmp);
		}
		
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
					
					String type = Utils.readAttributeString("deviceType", e);
					Long id = Utils.readAttributeLong("id", e);
					File dev = deviceTypes.get(type);
					if(dev != null){
					// Create new agent
					Device tmp = new Device(
											e,
											id,
											this,
											dev,
											agentTypes,
											interfaceTypes,
											agentData,
											batteryTypes,
											storageTypes);
					
					// Add agent to static agent list
					stationaryAgents.add(tmp);

					// Add agent to all list
					allAgents.add(tmp);
					
					}
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
			caseDir  = Utils.readElementString("caseDir",root);
			saveDirectory = caseDir +"/" +Utils.readElementString("saveDirectory",root);
			sumoFile  = Utils.readElementString("sumoFile",root);
			sumoServer  = Utils.readElementString("sumoServer",root);
			deltasPerFile  = Utils.readElementInt("deltasPerFile",root);
			System.out.println(deltasPerFile);
			
			staticAgentFile = caseDir +"/" +Utils.readElementString("staticAgentFile",root);
			roadNetwork = caseDir +"/" +Utils.readElementString("sumoRoadNetwork",root);
			
			
			
			
			simulationDurationSeconds= Utils.readElementInt("simulationDurationSeconds",root);
			//stepDelta= Utils.readElementInt("stepDelta",root);	
			
			//Load the directories
			agentTypeDir = caseDir +"/" +Utils.readElementString("agentDir",root);
			deviceTypeDir = caseDir +"/" +Utils.readElementString("deviceDir",root);
			interfaceTypeDir = caseDir +"/" +Utils.readElementString("interfaceDir",root);
			agentDataDir = caseDir +"/" +Utils.readElementString("agentDataDir",root);
			batteryTypeDir = caseDir +"/" +Utils.readElementString("batteryDir",root);
			storageTypeDir = caseDir +"/" +Utils.readElementString("storageDir",root);
			
			//Read agent types
			readTypes(agentTypes,agentTypeDir);	
			//Read interface types
			readTypes(interfaceTypes,interfaceTypeDir);	
			//Read device types
			readTypes(deviceTypes,deviceTypeDir);	
			//Read agent specific data
			readTypes(agentData,agentDataDir);
			//Read battery
			readTypes(batteryTypes,batteryTypeDir);
			//Read storage
			readTypes(storageTypes,storageTypeDir);
			
			
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		return true;

	}
	
	private void readTypes(Map<String,File> types, String path){
		File tmp = new File(path);			
		File[] files = readFiles(tmp);
		for(File f:files){
			System.out.println("loaded "+ Utils.stripExtension(f.getName()));
			types.put(Utils.stripExtension(f.getName()),f);
		}
	}	
	
	
	private File[] readFiles(File dir){
		File [] ret = dir.listFiles(
				new FilenameFilter(){
					public boolean accept(File dir, String name){
						return name.endsWith(".xml");
					}
				});
		return ret;
	}
	
	
	
	//Calculate agents in range
	public List<Device> inRange(Device agent, WirelessConnection winterface){
		double range = winterface.maxRange();
		List<Device> agentsInRange = new ArrayList<Device>();
		Double2D aPos = agent.currentPosition();
		for(Device a:allAgents){
			if(winterface.isCompatible(a.getInterface())){
				if(aPos.distance(a.currentPosition())<=range && a != agent && a.hasPower() && a.interfaceActive()){
					agentsInRange.add(a);
				}		
			}
		}
		return agentsInRange;
	}
	
	

}
