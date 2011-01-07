/**
 * 
 */
package it.polimi.vcdu.sim;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */

import it.polimi.vcdu.alg.DefaultAlg;
import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Configuration;
import it.unipr.ce.dsg.deus.core.Engine;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.SimulationException;

import java.util.ArrayList;
import java.util.HashMap;






public class Simulator1 {

	private Configuration conf;
	private HashMap <Component,SimContainer> simContainerMap= new HashMap <Component,SimContainer>();
	
	
	// Instance variables;

	
	protected Engine engine;
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Node> components; 
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Event> events; 

	protected ArrayList<it.unipr.ce.dsg.deus.core.Process> processes; 
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Process> refProcesses; 
	protected static Simulator1 DefaultSimulator;
	protected static boolean StopSimulation = false;
	

	//protected float maxVirtualTime = 0;
	protected int seed = ControlParameters.getCurrentParameters().getSeed();

	
	public Simulator1(Configuration conf){
		this.conf=conf;
		initSimObjects();
		//for testing purpose
		int nTransactions = 2;
		initEvents(nTransactions);
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
			
			simCont.setAlgorithm(new DefaultAlg(simCont));
			//simCont.setAlgorithm(new VersionConsistency(simCont));
			
			
			simCont.setSimNet(new SimNet(c,simCont));
			this.simContainerMap.put(c, simCont);
		}
	}
	
	/**
	 * @return
	 */
	private void initEvents(int nTransactions) {
		this.events = new ArrayList<it.unipr.ce.dsg.deus.core.Event>();
		for (int i=0; i<nTransactions;i++){			
			//int host_id=RandUtils.selectComponent(this.conf.getComponents().size());
			//only for testing purpose
			int host_id=conf.getComponents().size();
			Component host= this.conf.getComponentFromId("C"+host_id);
			try {
				SimEvent ev=new SimEvent("injectRootTx",null,this.simContainerMap.get(host),null);
				ev.setSchedulerListener(new MySchedulerListener());
				Object parameters[]= new Object[1];
				parameters[0]=ev;				
				ev.setParameters(parameters);		
				ev.setOneShot(true);
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
			NoDelayProcess process;
			process = new NoDelayProcess("noDelayProcess",null,null,events);
			for(it.unipr.ce.dsg.deus.core.Event e:events){
				e.setParentProcess(process);
			}
			processes.add(process);
		} catch (InvalidParamsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
	}

	public void run(){
		Simulator1.setStopSimulation(false);
		totalAmountOfWorkCompleted = 0;
		try {
			engine.run();
		} catch (SimulationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
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

	public static Simulator1 getDefaultSimulator() {
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
	
