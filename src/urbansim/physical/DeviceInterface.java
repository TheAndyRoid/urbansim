package urbansim.physical;

import java.util.List;

import urbansim.Message;
import urbansim.StopException;
import de.matthiasmann.continuations.SuspendExecution;



//These are all the system calls that devices must implement
public interface DeviceInterface {
	public long sendTo(DeviceInterface d, Message msg) throws SuspendExecution,StopException;
	public Message recv() throws SuspendExecution,StopException;
	public List<DeviceInterface> scan() throws SuspendExecution,StopException;
	public void sleep() throws SuspendExecution,StopException;
	public void sleep(long millaseconds) throws SuspendExecution,StopException;
	public String getName();
	public boolean connect(DeviceInterface d) throws SuspendExecution,StopException;
	public boolean disconnect(DeviceInterface d);
	public List<DeviceInterface> getActiveConnections() throws SuspendExecution,StopException;
	public LongTermStorage getStorage();
	public int getMaxConnections();
	public boolean isRunning() throws StopException; 
	public void interfaceOn() throws SuspendExecution,StopException;
	public void interfaceOff() throws SuspendExecution,StopException;
	public boolean isInterfaceActive() throws SuspendExecution,StopException;
	public double getBatteryRemaining() throws SuspendExecution,StopException;
	public double getTime() throws SuspendExecution,StopException;
}
