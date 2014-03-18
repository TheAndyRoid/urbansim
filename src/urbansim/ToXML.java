package urbansim;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class ToXML {
	abstract void toXML(Message msg, Element root, Document doc);
}
