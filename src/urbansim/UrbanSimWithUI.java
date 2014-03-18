package urbansim;

import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.grid.*;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.portrayal.simple.*;

import java.awt.*;

import javax.swing.*;

import sim.util.gui.*;
import sim.portrayal.*;

public class UrbanSimWithUI extends GUIState {

	public Display2D display;
	public JFrame displayFrame;
	ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();
	NetworkPortrayal2D networkPortrayal = new NetworkPortrayal2D();


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
		super.start();
		setupPortrayals();
	}

	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}

	public void setupPortrayals() {

		// Cast
		UrbanSim urbanSim = (UrbanSim) state;
		// Setup what and how to portray agents
		agentPortrayal.setField(urbanSim.agentPos);
		agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(8));
		
		
		networkPortrayal.setField( new SpatialNetwork2D( urbanSim.agentPos, urbanSim.connected ) );
		networkPortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D());


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
		display.attach(agentPortrayal, "Map");
		display.attach( networkPortrayal, "Connected" );
	}

	public void quit() {
		super.quit();
		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;
	}

}
