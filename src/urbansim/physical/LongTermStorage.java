package urbansim.physical;



import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import urbansim.Device;
import urbansim.Message;
import urbansim.ToXML;
import urbansim.Utils;


public class LongTermStorage extends ToXML{
	private long maxSize;
	private long currentSize;
	LinkedList <Object> storage = new LinkedList<Object>();
	Map <Object,Integer> sizes = new HashMap<Object,Integer>();
	
	//Read in config settings.
	public LongTermStorage(File file){	
		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();

			Element capacitymax = Utils.getChildElement("maxcapacity", root);
			maxSize = Utils.readAttributeInt("bits", capacitymax);
			currentSize = 0;		

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public boolean add(Object obj, int size){
		if(hasSpaceFor(size)){
			currentSize+= size;

			storage.add(obj);
			sizes.put(obj,size);
		
			return true;
		}else{
			return false;
		}
	}
	public boolean remove(Object obj){

		if(storage.remove(obj)){
		currentSize-= sizes.get(obj);
		sizes.remove(obj);
		return true;
		}else{
		return false;
		}
	}
	
	public Object[] getArray(){
		return storage.toArray(new Object[storage.size()]);
	}
	
	public Object getOldest(){
		return storage.getFirst();
	}
	public void removeOldest(){
		storage.remove(getOldest());
	}
	public Object getYoungest(){
		return storage.getLast();
	}
	public void removeYoungest(){
		storage.remove(getYoungest());
	}
	//true has space false not.
	public boolean hasSpaceFor(int size){
	 if(maxSize>currentSize+size){
		return true; 
	 }else{
		 return false;
	 }
	}
	public long maxSize(){
		long ret = maxSize;
		return ret;
	}


	@Override
	public Element toXML(Element root, Document doc) {
		// create agent element
		Element estorage = doc.createElement("storage");
		root.appendChild(estorage);
		estorage.setAttribute("maxSize", Long.toString(maxSize));
		estorage.setAttribute("currentSize", Long.toString(currentSize));
		estorage.setAttribute("freeSpace", Long.toString(maxSize - currentSize));
		return estorage;
	}
	
	
	
	
	
}
