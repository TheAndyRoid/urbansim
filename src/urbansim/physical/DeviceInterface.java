package urbansim.physical;

import java.util.List;

import urbansim.Message;
import de.matthiasmann.continuations.SuspendExecution;



//These are all the system calls that devices must implement
public interface DeviceInterface {
	public void sendTo(DeviceInterface d, Message msg) throws SuspendExecution;
	public Message recv() throws SuspendExecution;
	public List<DeviceInterface> scan() throws SuspendExecution;
	public void sleep() throws SuspendExecution;
	public void sleep(int seconds) throws SuspendExecution;
	public String getName();
	public boolean connect(DeviceInterface d) throws SuspendExecution;
	public boolean disconnect(DeviceInterface d);
	public List<DeviceInterface> activeConnections() throws SuspendExecution;
	public LongTermStorage getStorage(); 
}
