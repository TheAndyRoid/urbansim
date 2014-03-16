package urbansim.physical;

import sim.engine.SimState;
import sim.util.Double2D;

public interface PhysicalComponent {
	public Double2D currentPosition();
	public void setPosition(SimState state, Double2D position);	

}
