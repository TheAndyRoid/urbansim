package urbansim.p2p;

import org.w3c.dom.Element;

import urbansim.StopException;
import urbansim.physical.DeviceInterface;
import de.matthiasmann.continuations.SuspendExecution;

public interface DeviceAgent {
	// The only function that needs to be implemented by the simulation user.
	void main() throws SuspendExecution,StopException;
	void constructor(DeviceInterface device,Element genericData, Element agentSpecificDate);
}
