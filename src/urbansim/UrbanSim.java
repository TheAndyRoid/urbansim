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
	private int threads = 1;
	public ParallelSequence SAgents;
	public TraCI traci;
	public Observer observer;
	public UI ui= new UI();
	// Array of all agents
	Device[] agents;
	
	
	//Lock for connections
	public Lock connection = new ReentrantLock();
	
	
	public String roadNetwork;
	
	
	public String caseFile = null;
	
	//Configuration Options
	private String caseDir;
	private String sumoFile;
	
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
		// clear loaded tyes
		agentTypes.clear();
		interfaceTypes.clear();
		deviceTypes.clear();
		agentData.clear();
		batteryTypes.clear();
		storageTypes.clear();
		
		
		
		
		
		
		
		if (traci != null) {
			traci.close();
			System.out.println("Close");
		}

		// Read in simulation settings
		readSimulationSettings(caseFile);

		//Read in static agents
		loadStaticAgent(staticAgentFile);
		
		// Create the motion aware stepper
		traci = new TraCI(sumoFile);
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
			urbansim.SAgents = new ParallelSequence(agents, threads);

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
		String type = null;
		try {
			type = vehicle.getType();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		 
		
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
		}else{
			System.out.println("Sumotype: "+type+" does not have a corisponding device type");
			
		}
		
		
		

		

	}

	// Vehicle was destroyed by sumo
	public void vehicleArrived(Vehicle vehicle) {
		System.out.println("dead Vehicle");
		// Remove from arrays
		Device tmp = mobileAgents.remove(vehicle.getID());
		if(tmp != null){
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
		

	}

	@Override
	//This is caused by a bad road simulation. ie cars become gridlocked, so they are teleported 
	// It's a rare event, but looks bad and is not realistic.
	// it's safer to Destroy the vehicle
	public void vehicleTeleportStarting(Vehicle vehicle) {
		System.out.println("                      Teleported Vehicle");
		//destroy it
		vehicleArrived(vehicle);

	
		

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

			
			String absolutePath = fXmlFile.getAbsolutePath();
			String filePathdir = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
			
			//read in the elements
			
			saveDirectory = filePathdir +Utils.readElementString("saveDirectory",root);
			sumoFile  = filePathdir +Utils.readElementString("sumoFile",root);
			
			deltasPerFile  = Utils.readElementInt("deltasPerFile",root);
			threads  = Utils.readElementInt("threads",root);
			System.out.println(deltasPerFile);
			
			staticAgentFile = filePathdir +Utils.readElementString("staticAgentFile",root);
			roadNetwork = filePathdir +Utils.readElementString("sumoRoadNetwork",root);
			
			
			
			

			//stepDelta= Utils.readElementInt("stepDelta",root);	
			
			//Load the directories
			agentTypeDir = filePathdir +Utils.readElementString("agentDir",root);
			deviceTypeDir = filePathdir +Utils.readElementString("deviceDir",root);
			interfaceTypeDir = filePathdir +Utils.readElementString("interfaceDir",root);
			agentDataDir = filePathdir +Utils.readElementString("agentDataDir",root);
			batteryTypeDir = filePathdir +Utils.readElementString("batteryDir",root);
			storageTypeDir = filePathdir +Utils.readElementString("storageDir",root);
			
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
