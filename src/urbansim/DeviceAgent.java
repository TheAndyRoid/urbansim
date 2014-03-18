package urbansim;

import de.matthiasmann.continuations.SuspendExecution;

public interface DeviceAgent {
	// The only function that needs to be implemented by the simulation user.
	abstract void main() throws SuspendExecution;
}
