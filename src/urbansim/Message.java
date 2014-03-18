package urbansim;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Message extends ToXML {

	
	public Message(Device sender, Object obj, Device destination, int size) {
		this.src = sender;
		this.dst = destination;
		this.obj = obj;
		this.size = size;
	}

	// Data
	public Device src;
	public Device dst;
	public Object obj;
	public int size;

	public void toXML(Message msg, Element root, Document doc) {
		// create agent element
		Element emsg = doc.createElement("msg");
		root.appendChild(emsg);
		// set attributes
		emsg.setAttribute("size", Integer.toString(msg.size));

		// sender
		Element sender = doc.createElement("src");
		sender.setAttribute("type", ((Agent) msg.src).getType());
		sender.setAttribute("id", Long.toString(((Agent) msg.src).getID()));
		emsg.appendChild(sender);
		// Receiver
		Element receiver = doc.createElement("dst");
		receiver.setAttribute("type", ((Agent) msg.dst).getType());
		receiver.setAttribute("id", Long.toString(((Agent) msg.dst).getID()));
		emsg.appendChild(receiver);

	}

}
