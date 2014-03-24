package urbansim;

import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Message extends ToXML implements Comparable<Message> {

	
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
	public double sendtime = 0;
	public double recvtime = 0;
	public boolean isSender;

	public Element toXML(Element root, Document doc) {
		// create agent element
		Element emsg = doc.createElement("msg");
		root.appendChild(emsg);
		// set attributes
		emsg.setAttribute("size", Integer.toString(size));
		if(isSender){
		emsg.setAttribute("sendtime", Double.toString(sendtime));
		}else{
		emsg.setAttribute("recvtime", Double.toString(recvtime));
		}

		// sender
		Element sender = doc.createElement("src");
		sender.setAttribute("type", ((Agent) src).getType());
		sender.setAttribute("id", Long.toString(((Agent) src).getID()));
		emsg.appendChild(sender);
		// Receiver
		Element receiver = doc.createElement("dst");
		receiver.setAttribute("type", ((Agent) dst).getType());
		receiver.setAttribute("id", Long.toString(((Agent) dst).getID()));
		emsg.appendChild(receiver);
		
		//call objects ToXML function
		java.lang.reflect.Method method;
		try {
			 method = obj.getClass().getMethod("toXML",Element.class, Document.class);
			 method.invoke(obj, emsg,doc );
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
		
		return emsg;

	}
	

	//Compare messages on the time
	public int compareTo(Message o) {
		return Double.valueOf(recvtime).compareTo(o.recvtime);
		
	}

}
