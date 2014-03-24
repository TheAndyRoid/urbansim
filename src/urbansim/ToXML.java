package urbansim;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class ToXML {
	public abstract Element toXML(Element root, Document doc);
}
