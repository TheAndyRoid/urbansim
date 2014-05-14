

import org.w3c.dom.Element;

import java.lang.management.*;



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
	
	
	static public String stripExtension(String name){
		int pos = name.lastIndexOf(".");
		if (pos > 0) {
		    name = name.substring(0, pos);
		}
		return name;
	}
	

	
	
	
}
