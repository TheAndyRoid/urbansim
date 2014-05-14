import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class Convert {

	String osmFile ="";
	String sumoFile  ="";
	String outputFile ="";
	String agentType ="";

	
	double DEG_TO_RAD = 0.017453292519943295769236907684886f;
	
	double xOffset;
	double yOffset;
	
	
	private DocumentBuilderFactory docFactory;
	private DocumentBuilder docBuilder;
	private Document doc;
	private Element rootElement;
	private TransformerFactory transformerFactory;
	private Transformer transformer;
	private DOMSource source;
	private StreamResult result;
	
	//these values adjust the offset of the map as sumo appears to do something odd here.
	private int yOffsetcorrection = 0;
	private int xOffsetcorrection = 0;
	

	public static void main(String[] args) {

		Convert c = new Convert();
		if(args.length != 4){
			System.out.println("Must supply <OSM file> <SUMO network File> <agentType> <output File> ");
			System.exit(0);
		}
		if(args.length == 4){
			c.osmFile=args[0];
			c.sumoFile=args[1];
			c.agentType=args[2];
			c.outputFile=args[3];
		}
				
		
		// Read the offset from the sumo file
		
		c.getOffset();
		ArrayList<double[]> busstopPositions = c.readOSM();
		ArrayList<double[]> convertedPositions = new ArrayList<double[]>(); 
		//System.out.println(busstopPositions);
		for(double [] p:busstopPositions){
			convertedPositions.add(c.WGS84toUTMtosumo(p[0],p[1]));			
		}
		
		for(double [] p:convertedPositions){
			System.out.println(p[0]+" "+p[1]);
		}

		c.writeToFile(convertedPositions);

		
		
	}

	
	private double[] WGS84toUTMtosumo(double lat,double clong){
		CoordinateConversion cc = new CoordinateConversion();
		String sutm = cc.latLon2UTM(lat,clong);
		String utm[] = sutm.split(" ");
		double [] point = new double[2];
		point[0] = Double.parseDouble(utm[2]);
		point[1] = Double.parseDouble(utm[3]);
		point[0] += xOffset;
		point[1] += yOffset;
		
		point[0] = round(point[0],2);
		point[1] = round(point[1],2);
		
		return point;
	}
	
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	
	
	// <location netOffset="-551412.24,-6331861.85"
	// convBoundary="0.00,0.00,4525.43,3876.78"
	// origBoundary="501616.54,6322128.42,
	//               606065.56,6670362.18"
	// projParameter="!"/>
	public void getOffset() {
		try {
			File XmlFile = new File(sumoFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(XmlFile);

			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();
			

			Element location = Utils.getChildElement("location",root);
			String convBoundary = Utils.readAttributeString("netOffset",location);
			String points[] = convBoundary.split(",");
			xOffset = Double.parseDouble(points[0])+xOffsetcorrection;
			yOffset = Double.parseDouble(points[1])+yOffsetcorrection;
			System.out.println("Using sumo offset "+xOffset + " , "+ yOffset);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList readOSM(){
		ArrayList<double[]> results = new ArrayList<double[]>(); 
		try {
			File XmlFile = new File(osmFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(XmlFile);

			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();
			
			
			
			NodeList nList = root.getElementsByTagName("node");
			for (int i = 0; i < nList.getLength(); i++) {
				Node inode = nList.item(i);
				if (inode.getNodeType() == Node.ELEMENT_NODE) {
					Element node = (Element) inode;
					// Got a node
					NodeList tList = node.getElementsByTagName("tag");

					for (int j = 0; j < tList.getLength(); j++) {
						Node tNode = tList.item(j);
						if (tNode.getNodeType() == Node.ELEMENT_NODE) {
							Element tag = (Element) tNode;
							if (Utils.readAttributeString("v", tag).equals(
									"bus_stop")) {
								// got a bus stop
								double[] point = new double[2];
								point[0] = Utils.readAttributeDouble("lat",
										node);
								point[1] = Utils.readAttributeDouble("lon",
										node);
								System.out.println("Found busstop at "+point[0] +" " + point[1]);
								results.add(point);
							}
						}

					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return results;
	}
	
	
	public void writeToFile(ArrayList<double[]> points){
		createNewFile(outputFile);
		int id = 0;
		for(double[]p:points){
			Element agent = doc.createElement("agent");
			rootElement.appendChild(agent);
			agent.setAttribute("id",Integer.toString(id));
			agent.setAttribute("deviceType",agentType);
			
			Element position = doc.createElement("position");
			agent.appendChild(position);
			position.setAttribute("x", Double.toString(p[0]));
			position.setAttribute("y", Double.toString(p[1]));			
			id++;
		}
		
		writeFile();
		
	}
	
	private void createNewFile(String filename) {
		try {
			// Create document
			docFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docFactory.newDocumentBuilder();

			// Add root
			doc = docBuilder.newDocument();
			rootElement = doc.createElement("Simulation");
			doc.appendChild(rootElement);

			// write the content into xml file
			transformerFactory = TransformerFactory.newInstance();
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "2");
			source = new DOMSource(doc);
			result = new StreamResult((new File(filename)));

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}

	}
	
	private void writeFile(){
		try {

			// Performs the actual writing
			transformer.transform(source, result);

			System.out.println("File saved!");

		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
	}
	
	
}
