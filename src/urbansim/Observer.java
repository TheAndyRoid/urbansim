package urbansim;


import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sim.engine.SimState;
import sim.engine.Steppable;

public class Observer implements Steppable {
	private DocumentBuilderFactory docFactory;
	private DocumentBuilder docBuilder;
	private Document doc;
	private Element rootElement;
	private TransformerFactory transformerFactory;
	private Transformer transformer;
	private DOMSource source;
	private StreamResult result;
	
	public Observer() {
		
		try {
			//Create document 
			docFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docFactory.newDocumentBuilder();
	 
			//Add root 
			doc = docBuilder.newDocument();
			rootElement = doc.createElement("Simulation");
			doc.appendChild(rootElement);
	 
			
	 
			// write the content into xml file
			transformerFactory = TransformerFactory.newInstance();
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			source = new DOMSource(doc);
			result= new StreamResult(new File("data.xml"));
	 
		
		 } catch (ParserConfigurationException pce) {
				pce.printStackTrace();
		 } catch (TransformerException tfe) {
					tfe.printStackTrace();
		  }	
		
		

	}

	@Override
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;
		
		state.schedule.getSteps();
		Element stepElement = doc.createElement("Step");
		rootElement.appendChild(stepElement);
		
		//set attribute of step
		Attr attr = doc.createAttribute("id");
		attr.setValue(String.valueOf(state.schedule.getTime()));
		stepElement.setAttributeNode(attr);
		
		
		for(Agent a:urbansim.allAgents){
			writeAgent(a,stepElement);			
		}
		
		//Write the file out
		try{	
			
		//Performs the actual writing
		transformer.transform(source, result);
 
		System.out.println("File saved!");
 
	 
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
		
		
	}
	
	
	private void writeAgent(Agent agent,Element stepRoot){	
			// staff elements	
					Element agentElement = doc.createElement("Agent");
					stepRoot.appendChild(agentElement);
			 
					// set attribute to staff element
					Attr attr = doc.createAttribute("id");
					attr.setValue("Agent@" + System.identityHashCode(agent));
					agentElement.setAttributeNode(attr);
			 
					// shorten way
					// staff.setAttribute("id", "1");
			 							
	}
	
	
	
	

}
