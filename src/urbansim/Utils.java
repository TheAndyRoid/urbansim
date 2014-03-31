package urbansim;

import org.w3c.dom.Element;

import sim.util.Double2D;

public class Utils {
	static public int readElementInt(String name,Element root){		
		return Integer.parseInt(root.getElementsByTagName(name).item(0).getTextContent());		
	}
	static public String readElementString(String name,Element root){		
		return root.getElementsByTagName(name).item(0).getTextContent();
	}
	static public Double readElementDouble(String name,Element root){		
		return Double.parseDouble(root.getElementsByTagName(name).item(0).getTextContent());		
	}
	
	static public Long readAttributeLong(String name,Element root){		
		return Long.parseLong(root.getAttribute(name));		
	}
	

	static public Element getChildElement(String name,Element root){		
		return (Element) root.getElementsByTagName(name).item(0);		
	}
	
	
	static public String readAttributeString(String name,Element root){		
		return root.getAttribute(name);		
	}
	
	static public int readAttributeInt(String name,Element root){		
		return Integer.parseInt(root.getAttribute(name));		
	}
	
	static public Double readAttributeDouble(String name,Element root){		
		return Double.parseDouble(root.getAttribute(name));		
	}
	static public Double2D readAttributeDouble2D(Element root){
		return new Double2D(
				readAttributeDouble("x",root),
				readAttributeDouble("y",root)				
		 );		
	}
	
	static public String stripExtension(String name){
		int pos = name.lastIndexOf(".");
		if (pos > 0) {
		    name = name.substring(0, pos);
		}
		return name;
	}
	
}
