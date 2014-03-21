package urbansim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
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

	private String saveDir;

	private int deltasPerFile = 1;

	
	private List<Message> sentMessages = new ArrayList<Message>();

	public Observer(int deltasPerFile, String saveDir, SimState state) {
		this.saveDir = saveDir;
		this.deltasPerFile = deltasPerFile;
		createDataFolder();
	}

	private void createDataFolder() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		boolean success = (new File(saveDir + "/" + dateFormat.format(date)))
				.mkdirs();
		if (!success) {
			System.out.println("Could not create save directory");
		} else {
			saveDir = new String(saveDir + "/" + dateFormat.format(date));
		}
	}

	@Override
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;

		if ((state.schedule.getTime() % deltasPerFile) == 0) {

			if (state.schedule.getTime() != 0) {// Write the old file out
				try {

					// Performs the actual writing
					transformer.transform(source, result);

					System.out.println("File saved!");

				} catch (TransformerException tfe) {
					tfe.printStackTrace();
				}
			}
			//Create a new file to use
			createNewFile(saveDir + "/"
					+ String.valueOf(state.schedule.getTime()) + ".xml");
		}

		Element stepElement = doc.createElement("Step");
		rootElement.appendChild(stepElement);

		// set attribute of step
		Attr attr = doc.createAttribute("id");
		attr.setValue(String.valueOf(state.schedule.getTime()));
		stepElement.setAttributeNode(attr);

		for (Agent a : urbansim.allAgents) {
			Agent.writeAgent(a, stepElement, doc);
		}

		for(Message msg:sentMessages){
			msg.toXML(msg, stepElement, doc);
		}
		//Clear the messages
		sentMessages.clear();
		
		if ((state.schedule.getTime() % (deltasPerFile + 1)) == 0) {

		}

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
	
	
	public void logMessage(Message msg){
		synchronized(sentMessages){
			sentMessages.add(msg);			
		}		
	}

		

}
