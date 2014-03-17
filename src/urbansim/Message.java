package urbansim;

public class Message {

	public Message(Device sender, Object obj, Device destination, int size) {
		this.src = sender;
		this.dst = destination;
		this.obj = obj;
		this.size = size;
	}

	
	//Data
	public Device src;
	public Device dst;
	public Object obj;
	public int size;
	
}
