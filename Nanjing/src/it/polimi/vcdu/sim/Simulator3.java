/**
 * 
 */
package it.polimi.vcdu.sim;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */

import it.polimi.vcdu.alg.DefaultAlg;
import it.polimi.vcdu.alg.VersionConsistency;
import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Configuration;
import it.unipr.ce.dsg.deus.core.Engine;
import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.SimulationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;






public class Simulator3 {

	private Configuration conf;
	private HashMap <Component,SimContainer> simContainerMap= new HashMap <Component,SimContainer>();
	
	
	// Instance variables;

	
	protected Engine engine;
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Node> components; 
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Event> events; 

	protected ArrayList<it.unipr.ce.dsg.deus.core.Process> processes; 
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Process> refProcesses; 
	protected static Simulator3 DefaultSimulator;
	protected static boolean StopSimulation = false;
	

	//protected float maxVirtualTime = 0;
	protected int seed = ControlParameters.getCurrentParameters().getSeed();

	
	public Simulator3(Configuration conf){
		this.conf=conf;
		initSimObjects();
		initEvents();
		initProcesses();
		this.refProcesses=processes;
		totalAmountOfWorkCompleted = 0;
		StopSimulation = false;
		float maxVirtualTime = ControlParameters.getCurrentParameters().getMaxVirtualTime();
		engine = new Engine(maxVirtualTime,seed, null,null,events,processes,this.refProcesses);
		DefaultSimulator=this;
	}
	
	public void initSimObjects()
	{
		ArrayList<Component> components=this.conf.getComponents();
		for(Component c: components){
			SimContainer simCont= new SimContainer(c);			
			
			//simCont.setAlgorithm(new DefaultAlg(simCont));
			simCont.setAlgorithm(new VersionConsistency(simCont));
			
			
			simCont.setSimNet(new SimNet(c,simCont));
			this.simContainerMap.put(c, simCont);
		}
	}
	
	/**
	 * @return
	 */
	private void initEvents() {
		ArrayList<Component> components = this.conf.getComponents();
		this.events = new ArrayList<it.unipr.ce.dsg.deus.core.Event>();
		for (Component host: components){	
			try {
				SimEvent ev=new SimEvent("injectRootTx",null,this.simContainerMap.get(host),null);
				ev.setSchedulerListener(new MySchedulerListener());
				Object parameters[]= new Object[1];
				parameters[0]=ev;				
				ev.setParameters(parameters);		
				//ev.setOneShot(true);
				ev.setOneShot(false);
				events.add(ev);
			} catch (InvalidParamsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}
	
	private void initProcesses(){
		processes = new ArrayList<it.unipr.ce.dsg.deus.core.Process>();
		try {
			Properties params = new Properties();
			String s_meanArrival = Float.toString(ControlParameters.getCurrentParameters().getMeanArrival());
			params.put("meanArrival", s_meanArrival);
			
			for(it.unipr.ce.dsg.deus.core.Event e:events){
				ArrayList<Event> refEvents = new ArrayList<Event> (1);
				refEvents.add(e);
				it.unipr.ce.dsg.deus.impl.process.PoissonProcess poissonProc = 
					new it.unipr.ce.dsg.deus.impl.process.PoissonProcess("myPoissonProcess", params,null,refEvents);
				e.setParentProcess(poissonProc);
				processes.add(poissonProc);

			}
		} catch (InvalidParamsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
	}

	public void run(){
		Simulator3.setStopSimulation(false);
		totalAmountOfWorkCompleted = 0;
		try {
			engine.run();
		} catch (SimulationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	public void stopSimulation(){
		Simulator3.setStopSimulation(true);
		System.exit(0);
	}
	
	
	/*
	 * getters and setters
	 */
	
	
	/**
	 * @return the conf
	 */
	public Configuration getConf() {
		return conf;
	}

	/**
	 * @param conf the conf to set
	 */
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	public static Simulator3 getDefaultSimulator() {
		return DefaultSimulator;
	}
	
	
	public static boolean isStopSimulation() {
		return StopSimulation;
	}

	public static void setStopSimulation(boolean stopSimulation) {
		StopSimulation = stopSimulation;
		if(StopSimulation) System.out.println("Stopping the simulation ...");
	}

	/* record total amount of work */
	private double totalAmountOfWorkCompleted = 0;
	public void addAmountOfWork(double work){
		totalAmountOfWorkCompleted += work;
	}
	public double getTotalAmountOfWorkCompleted(){
		return totalAmountOfWorkCompleted;
	}





}
	
