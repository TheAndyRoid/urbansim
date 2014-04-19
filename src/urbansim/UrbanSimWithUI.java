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
import java.awt.geom.Rectangle2D;
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

	
	Double2D botLeft;
	Double2D topRight;
	
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
			
			
			
			
			Element location = Utils.getChildElement("location",root);
			String convBoundary = Utils.readAttributeString("convBoundary",location);
			String points[] = convBoundary.split(",");
			botLeft = new Double2D(Double.parseDouble(points[0]),
					Double.parseDouble(points[1]));
			topRight = new Double2D(Double.parseDouble(points[2]),
					Double.parseDouble(points[3]));
			
			roadPoints = new Continuous2D(1.0,urbanSim.width,urbanSim.height);
			roadEdges = new Network(false);
			

			

			
						
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
							processShapeLane(lane);
						}

					}
				}
			}
			
			NodeList jList = root.getElementsByTagName("junction");
			for (int i = 0; i < jList.getLength(); i++) {
				Node jNode = jList.item(i);
				if (jNode.getNodeType() == Node.ELEMENT_NODE) {
					Element junction = (Element) jNode;
					processShapeJunction(junction);
				}
			}
			

			
			setupDisplay(controller);
			
			
			System.out.println("Loaded " + urbanSim.roadNetwork);		

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	public void processShapeLane(Element e){
		String shape = Utils.readAttributeString("shape",e);
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
	
	
	
	public void processShapeJunction(Element e){
		String shape = Utils.readAttributeString("shape",e);
		String edges[] = shape.split("\\s+");
		Object firstObj = null;
		Object lastObj = null;
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
			
			
			if(firstObj == null){
				firstObj = A;
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
			lastObj = A;
				
		}
		
		//close the ploygon
		Edge tmp = new Edge(firstObj,lastObj,null);
		roadEdges.addEdge(tmp);	
		
		
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
		setupDisplay(c);		
	}
	
	private void setupDisplay(Controller c){
		UrbanSim urbanSim = (UrbanSim) state;
		
		if (displayFrame != null){
			c.unregisterFrame(displayFrame);
			displayFrame.dispose();
		}
		display = new Display2D(500, 500, this);
		display.setClipping(false);
		displayFrame = display.createFrame();
		displayFrame.setTitle("Map Display");
		c.registerFrame(displayFrame); // so the frame appears in the "Display"
										// list
		
		
		
		Rectangle2D.Double rect = new Rectangle2D.Double(
				0,  // translated x origin
				display.insideDisplay.height, // translated y origin
				display.insideDisplay.width/10, // scaled width
				display.insideDisplay.height/10 ); // scaled height
		
		
		
		display.attach(roadPortrayal, "Roads" ,rect,true);
		display.attach(agentPortrayal, "Devices",  rect,true);
		display.attach( networkPortrayal, "Connections" ,rect,true);
		
		displayFrame.setVisible(true);
		
		display.setClipping(false);
	}
	

	

	public void quit() {
		super.quit();
		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}

}
