package urbansim;

import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import urbansim.physical.DeviceInterface;

public class Message extends ToXML implements Comparable<Message> {

	
	public Message(DeviceInterface sender, Object obj, DeviceInterface destination, int size) {
		this.src = sender;
		this.dst = destination;
		this.obj = obj;
		this.size = size;
	}

	// Data
	public DeviceInterface src;
	public DeviceInterface dst;
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
		sender.setAttribute("agentType", ((Device) src).getAgentType());
		sender.setAttribute("deviceType", ((Device) src).getDeviceType());
		sender.setAttribute("id", Long.toString(((Device) src).getID()));
		emsg.appendChild(sender);
		// Receiver
		Element receiver = doc.createElement("dst");
		receiver.setAttribute("agentType", ((Device) dst).getAgentType());
		receiver.setAttribute("deviceType", ((Device) dst).getDeviceType());
		receiver.setAttribute("id", Long.toString(((Device) dst).getID()));
		emsg.appendChild(receiver);
		
		//call objects ToXML function if it exists
		java.lang.reflect.Method method;
		try {
			 method = obj.getClass().getMethod("toXML",Element.class, Document.class);
			 method.invoke(obj, emsg,doc );
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				//Object does no implements such a method, no problem continue
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
