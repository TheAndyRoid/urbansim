package urbansim;

import java.util.List;

import de.matthiasmann.continuations.SuspendExecution;



//These are all the system calls that devices must implement
public interface Device {
	public void sendTo(Device d, Message msg) throws SuspendExecution;
	public Message recv() throws SuspendExecution;
	public List<Device> scan() throws SuspendExecution;
	public void sleep() throws SuspendExecution;
	public void sleep(int seconds) throws SuspendExecution;
	public String getName();
}
