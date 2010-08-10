/**
 * 
 */
package it.polimi.vcdu.sim;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */

import it.polimi.vcdu.alg.Algorithm;
import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Configuration;
import it.polimi.vcdu.model.Transaction;
import it.polimi.vcdu.sim.record.Recorder;
import it.polimi.vcdu.sim.record.ReplayProcess;
import it.unipr.ce.dsg.deus.core.Engine;
import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.SimulationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;






public class Simulator {

	private Configuration conf;
	private HashMap <Component,SimContainer> simContainerMap= new HashMap <Component,SimContainer>();
	
	
	// Instance variables;

	
	protected Engine engine;
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Node> components; 
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Event> events; 

	protected ArrayList<it.unipr.ce.dsg.deus.core.Process> processes; 
	
	protected ArrayList<it.unipr.ce.dsg.deus.core.Process> refProcesses; 
	protected static Simulator DefaultSimulator;
	protected boolean StopSimulation = false;
	protected Class<?extends Algorithm> algorithmClass;
	

	//protected float maxVirtualTime = 0;
	protected int seed = ControlParameters.getCurrentParameters().getSeed();


	

	public Simulator(Configuration conf,Class<?extends Algorithm> algorithmClass){
		this.conf=conf;
		this.algorithmClass = algorithmClass; 
		initSimObjects();
		initEvents();
		initProcesses();
		this.refProcesses=processes;
		totalAmountOfWorkCompleted = 0;
		StopSimulation = false;
		DefaultSimulator=this;

	}
	
	/**
	 * Replay with the recorder. conf must be corresponding to the one recorded.
	 * @param conf
	 * @param algorithmClass
	 * @param recorder
	 */
	public Simulator(Configuration conf,Class<?extends Algorithm> algorithmClass, Recorder recorder){
		this.recorder = recorder;
		this.isReplaying = true;
		this.recorder.reInit();
		
		this.conf=conf;
		this.algorithmClass = algorithmClass; 
		initSimObjects();
		initEvents();
		initProcessesReplay();
		this.refProcesses=processes;
		totalAmountOfWorkCompleted = 0;
		StopSimulation = false;
		DefaultSimulator=this;

	}
	
	public void initSimObjects()
	{
		ArrayList<Component> components=this.conf.getComponents();
		for(Component c: components){
			SimContainer simCont= new SimContainer(c);			
			
			try {
				@SuppressWarnings("rawtypes")
				Class [] paramsTypes= new Class[1];
				paramsTypes[0]=SimContainer.class;
				Constructor<? extends Algorithm> constructor = this.algorithmClass.getConstructor(paramsTypes);
				simCont.setAlgorithm(constructor.newInstance(simCont));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/*
*/
			
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
				MyPoissonProcess poissonProc = 
					new MyPoissonProcess("myPoissonProcess", params,null,refEvents);
				e.setParentProcess(poissonProc);
				processes.add(poissonProc);

			}
		} catch (InvalidParamsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
	}

	private void initProcessesReplay(){
		processes = new ArrayList<it.unipr.ce.dsg.deus.core.Process>();
		try {
			for(it.unipr.ce.dsg.deus.core.Event e:events){
				SimEvent simEvent = (SimEvent)e;
				Component host = simEvent.getSimObject().getHostComponent();
				ArrayList<Event> refEvents = new ArrayList<Event> (1);
				refEvents.add(e);
				ArrayList<AbstractMap.SimpleEntry<String, Float>> list = this.recorder.getTotalHistory().get(host.getId());
				ReplayProcess replayProcess = new ReplayProcess("ReplayProcess", list ,refEvents);
				e.setParentProcess(replayProcess);
				processes.add(replayProcess);

			}
		} catch (InvalidParamsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
	}
	
	public void run(){
		setStopSimulation(false);
		totalAmountOfWorkCompleted = 0;

		float maxVirtualTime = ControlParameters.getCurrentParameters().getMaxVirtualTime();
		engine = new Engine(maxVirtualTime,seed, null,null,events,processes,this.refProcesses);
		try {
			engine.run();
		} catch (SimulationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void insertProcess(it.unipr.ce.dsg.deus.core.Process process){
		this.processes.add(process);
	}
	
	public void insertEvent(Event event){
		this.events.add(event);
	}
	
	/*
	 * For recording and replaying
	 */
	private Recorder recorder;
	private boolean isRecording = false;
	private boolean isReplaying = false;
	
	
	
	
	
	
	
	
	
	
	
	/*
	 * getters and setters
	 */
	
	
	public boolean isRecording() {
		return isRecording;
	}

	public void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
		if(isRecording){
			recorder = new Recorder();
		}
	}

	public boolean isReplaying() {
		return isReplaying;
	}

	public void setReplaying(boolean isReplaying) {
		this.isReplaying = isReplaying;
	}

	public Recorder getRecorder() {
		return recorder;
	}

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

	public static Simulator getDefaultSimulator() {
		return DefaultSimulator;
	}
	
	
	public boolean isStopSimulation() {
		return StopSimulation;
	}

	public void setStopSimulation(boolean stopSimulation) {
		this.StopSimulation = stopSimulation;

		if(StopSimulation) {
	//		Engine.getDefault().stopnow();
			System.out.println("Stopping the simulation ...");
		}
	}

	/* record total amount of work */
	private double totalAmountOfWorkCompleted = 0;
	public void addAmountOfWork(double work){
		totalAmountOfWorkCompleted += work;
	}
	public double getTotalAmountOfWorkCompleted(){
		return totalAmountOfWorkCompleted;
	}

	public SimContainer getSimContainer(Component comp){
		return this.simContainerMap.get(comp);
	}

	/**
	 * @return the algorithm
	 */
	public Class<? extends Algorithm> getAlgorithmClass() {
		return algorithmClass;
	}

	/**
	 * @param algorithm the algorithm to set
	 */
	public void setAlgorithmClass(Class<? extends Algorithm> algorithm) {
		this.algorithmClass = algorithm;
	}

}
	
