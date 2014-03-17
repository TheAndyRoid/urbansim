package urbansim;

import java.util.List;



//These are all the system calls that devices must implement
public interface Device {
	public void sendTo(Device d, Message msg);
	public Message recv();
	public List<Device> scan();
}
