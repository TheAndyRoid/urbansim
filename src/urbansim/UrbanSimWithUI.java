package urbansim;

import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.display.*;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.grid.*;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.portrayal.simple.*;

import java.awt.*;
import java.io.File;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sim.util.Bag;
import sim.util.Double2D;
import sim.util.gui.*;
import sim.portrayal.*;

public class UrbanSimWithUI extends GUIState {

	public Display2D display;
	public JFrame displayFrame;
	ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();
	NetworkPortrayal2D networkPortrayal = new NetworkPortrayal2D();
	
	Continuous2D roadPoints;
	Network roadEdges;
	NetworkPortrayal2D roadPortrayal = new NetworkPortrayal2D();

	public static void main(String[] args) {

		UrbanSimWithUI vid = new UrbanSimWithUI();
		Console c = new Console(vid);
		c.setVisible(true);
	}

	public UrbanSimWithUI() {
		super(new UrbanSim(System.currentTimeMillis()));
	}

	public UrbanSimWithUI(SimState state) {
		super(state);
	}

	public static String getName() {
		return "UrbanSim";
	}

	public void start() {
		UrbanSim urbanSim = (UrbanSim) state;
		super.start();
		roadPoints = new Continuous2D(1.0,urbanSim.width,urbanSim.height);
		roadEdges = new Network(false);
		loadRoads();
		setupPortrayals();
	}

	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}
	
	
	public void loadRoads(){
		UrbanSim urbanSim = (UrbanSim) state;
		
		try {
			File fXmlFile = new File(urbanSim.roadNetwork);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();		
			
			
			
			NodeList eList = root.getElementsByTagName("edge");
			
			
			// for all edges
			for (int i = 0; i < eList.getLength(); i++) {
				Node eNode = eList.item(i);
				if (eNode.getNodeType() == Node.ELEMENT_NODE) {
					Element edge = (Element) eNode;
					// for all lanes
					NodeList lList = edge.getElementsByTagName("lane");

					for (int j = 0; j < lList.getLength(); j++) {
						Node lNode = lList.item(j);
						if (lNode.getNodeType() == Node.ELEMENT_NODE) {
							Element lane = (Element) lNode;
							processLane(lane);
						}

					}
				}
			}
			
			System.out.println("Loaded " + urbanSim.roadNetwork);		

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	public void processLane(Element lane){
		String shape = Utils.readAttributeString("shape",lane);
		String edges[] = shape.split("\\s+");
		Double2D lastPos = null;
		for (int i= 0; i < edges.length; i++) {
			String pos[] = edges[i].split(",");
			double x = Double.parseDouble(pos[0]);
			double y = Double.parseDouble(pos[1]);
			Double2D pos2d = new Double2D(x, y*-1);
			
			//check if the point exists, otherwise add it.
			Bag point = roadPoints.getObjectsAtLocation(pos2d);
			Object A = null;
			if(point == null || point.size() == 0){
				A = pos2d;
				roadPoints.setObjectLocation(A,pos2d);
				System.out.println(pos2d.toString());
			}else{
				A  = point.get(0);
			}
			
			Object B = null;
			if (lastPos != null){
				Bag tmp = roadPoints.getObjectsAtLocation(lastPos);
				 B = tmp.get(0);
				 lastPos = pos2d;
			}else{
				lastPos = pos2d;
				continue;
			}
			
			Edge tmp = new Edge(A,B,null);
			roadEdges.addEdge(tmp);			
				
		}
	}
	

	

	public void setupPortrayals() {

		// Cast
		UrbanSim urbanSim = (UrbanSim) state;
		// Setup what and how to portray agents
		agentPortrayal.setField(urbanSim.agentPos);
//		agentPortrayal.setPortrayalForAll(new OvalPortrayal2D());
		agentPortrayal.setPortrayalForAll(new SimplePortrayal2D(){
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info){
				Device d = (Device)object;
				//Draw the device specific portrayal
				d.getPortrayal().draw(object, graphics, info);			
			}
			
			 public boolean hitObject(Object object, DrawInfo2D range){
				 Device d = (Device)object;
				 return d.getPortrayal().hitObject(object, range);	
			 }			
			});
		
		
		networkPortrayal.setField( new SpatialNetwork2D( urbanSim.agentPos, urbanSim.connected ) );
		networkPortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D());
		
		
		roadPortrayal.setField( new SpatialNetwork2D( roadPoints, roadEdges ) );
		roadPortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D());


		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.white);
		// redraw the display
		display.repaint();

	}

	public void init(Controller c) {
		super.init(c);
		display = new Display2D(600, 600, this);
		display.setClipping(false);
		displayFrame = display.createFrame();
		displayFrame.setTitle("Map Display");
		c.registerFrame(displayFrame); // so the frame appears in the "Display"
										// list
		displayFrame.setVisible(true);
		display.attach( roadPortrayal, "Roads" ,0,  display.insideDisplay.height, true);
		
		display.attach(agentPortrayal, "Devices",  0,  display.insideDisplay.height,true);
		display.attach( networkPortrayal, "Connections" ,0,  display.insideDisplay.height, true);
		
	}

	public void quit() {
		super.quit();
		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}

}
