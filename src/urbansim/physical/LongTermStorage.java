package urbansim.physical;



import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
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


public class LongTermStorage {
	long maxSize;
	long currentSize;
	TreeSet <Message> storage = new TreeSet<Message>();
	
	//Read in config settings.
	public LongTermStorage(File file){		
		
	}
	
	
	
	public boolean add(Message msg){
		//Check that we have the storage space
		if(hasSpace(msg.size)){
			currentSize+= msg.size;

			storage.add(msg);		
		
			return true;
		}else{
			return false;
		}
	}
	public void remove(Message msg){

		storage.remove(msg);
		currentSize-= msg.size;
	}
	public Message getOldest(){
		return storage.last();
	}
	public void removeOldest(){
		storage.remove(getOldest());
	}
	public Message getYoungest(){
		return storage.first();
	}
	public void removeYoungest(){
		storage.remove(getYoungest());
	}
	//true has space false not.
	public boolean hasSpace(int size){
	 if(maxSize>currentSize+size){
		return true; 
	 }else{
		 return false;
	 }
	}
	public long maxSize(){
		return maxSize;
	}
	
	
	
	
	
	
	
}
