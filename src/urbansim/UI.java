package urbansim;

import sim.engine.SimState;
import sim.engine.Steppable;

public class UI implements Steppable  {

	@Override
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;
		
		for(Device d:urbansim.allAgents){
			d.updateUI();
		}
		for(Device d:urbansim.allAgents){
			//d.resetConnectionBandwidthUI();
		}
		
		
	}

}
