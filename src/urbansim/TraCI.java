package urbansim;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import it.polito.appeal.traci.MultiQuery;
import it.polito.appeal.traci.StepAdvanceListener;
import it.polito.appeal.traci.SumoTraciConnection;
import it.polito.appeal.traci.Vehicle;
import it.polito.appeal.traci.VehicleLifecycleObserver;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double2D;

public class TraCI implements Steppable{
	
	
	private SumoTraciConnection conn ;	
	
	
	public TraCI(String file){	
		BasicConfigurator.configure();
		conn = new SumoTraciConnection(
				file,  // config file
				12345                                  // random seed
				);
		try {
			conn.runServer();
			
			System.out.println("Map bounds are: " + conn.queryBounds());
			
			/*for (int i = 0; i < 4; i++) {
				int time = conn.getCurrentSimStep();
				Collection<Vehicle> vehicles = conn.getVehicleRepository().getAll().values();
				
				System.out.println("At time step " + time + ", there are "
						+ vehicles.size() + " vehicles: " + vehicles);
				for(Vehicle v:vehicles){
					System.out.println("Vehicle ID:" + v.getID());
					System.out.println("Vehicle Position:" + v.getPosition());
					
				}
				
				conn.nextSimStep();
			}*/
			
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	


	
	public void addVehicleLifecycleObserver(SimState state){
		conn.addVehicleLifecycleObserver((VehicleLifecycleObserver) state);	
	}
	
	
	public void step(SimState state) {
		UrbanSim urbansim = (UrbanSim) state;
		try {
			conn.nextSimStep();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//update mobile positions here
		
		
		//convert map to list		
		List<Device> allMobile = new ArrayList<Device>(urbansim.mobileAgents.values());
		
		
		//Split mobile agents into lists of 200		
		int partitionSize = 200;
		
		List<List<Device>> partitions = new LinkedList<List<Device>>();
		for (int i = 0; i < allMobile.size(); i += partitionSize) {
			partitions.add(allMobile.subList(i,
					i + Math.min(partitionSize, allMobile.size() - i)));
		}

		for (List<Device> part : partitions) {

			MultiQuery mq = conn.makeMultiQuery();
			for (Device d : part) {
				Device tmp = d;
				mq.add(tmp.v.queryReadPosition());
			}
			try {
				mq.run();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Multiquery done!");

			for (Device d : part) {
				Device tmp = d;

				Point2D pos = new Point2D.Double();
				try {
					pos = tmp.v.getPosition();
				} catch (IOException e) {
					e.printStackTrace();
				}

				tmp.setPosition(urbansim, new Double2D(pos.getX(), pos.getY()));
			}

		}
		
		
		System.out.println("Updating Positions done!");
				
	}
	public void close(){
		try {
			conn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LogManager.shutdown();
	}



	
}
