/**
 * 
 */
package it.polimi.vcdu.sim;

import it.polimi.vcdu.sim.MySchedulerListener;
import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.Process;

import java.lang.reflect.Method;
import java.util.Random;


/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class SimEvent extends Event {

/*	private enum EventType
	{ onInitTx,initSubTx,onSend,deliver,notifyProcessing,processing,subTxEnd,sendSubTxEnd,deliverSubEnd,notifySubEnd,onSubTxEnd;}*/

	private SimObject simObject;

	private Object [] parameters;
	//protected int seed =12345;
	/**
	 * @param id
	 * @param params
	 * @param parentProcess
	 * @throws InvalidParamsException
	 */
	public SimEvent(String id,  Process parentProcess,SimObject target, Object [] parameters)
			throws InvalidParamsException {
		super(id, null, parentProcess);
		simObject= target;
		this.parameters=parameters;
		
		//Random simulationRandom = new Random(seed);
		//Random simulationRandom = ControlParameters.getCurrentParameters().getRandomObj();
		Random simulationRandom = ControlParameters.getCurrentParameters().getTempRandom();
		this.setEventSeed(simulationRandom.nextInt());
		this.setOneShot(true);
	}
	
	public void addReferencedEvent(SimEvent refEvent)
	{
		this.referencedEvents.add(refEvent);
	}
	public void initReferencedEvent(SimEvent refEvent)
	{
		this.referencedEvents.clear();
		this.referencedEvents.add(refEvent);
	}

	public void clearReferencedEvent(){
		this.referencedEvents.clear();
	}

	@Override
	public void run() {
		if (Simulator.getDefaultSimulator().isStopSimulation()) return;
		if (this.id!=null)
		runMethod(this.id,this.simObject, this.parameters);		
	}
	
	@SuppressWarnings("rawtypes")
	public static void runMethod(String name, Object target, Object[] parameters) {
	    
		Method method = null;
	    try {
	    	if (parameters==null)
	    	{
	    		method = target.getClass().getMethod( name);
	    	}
	    	else
	    	{
	    		Class[] parametersClass= new Class[parameters.length];
	    		for (int i=0; i< parameters.length;i++)
	    		{
	    			parametersClass[i]= parameters[i].getClass();
	    		}
	    		method = target.getClass().getMethod( name, parametersClass);
	    	}
	    	
	      
	     // target.getClass().getMethod(name, parameterTypes)
	    }
	    catch (NoSuchMethodException e) { e.printStackTrace();System.exit(1);}
	    if (method != null)
	    try {
	      method.invoke(target, parameters);
	      
	    }
	    catch (Exception ecc) { ecc.printStackTrace();System.exit(1);}
	}
  
	/**
	 * @return the simObject
	 */
	public SimObject getSimObject() {
		return simObject;
	}

	/**
	 * @param simObject the simObject to set
	 */
	public void setSimObject(SimObject simObject) {
		this.simObject = simObject;
	}

	/**
	 * @return the parameters
	 */
	public Object getParameters() {
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}
	
	public void notify(String notification,SimObject destination,Object[] parameters,Process process ){
		SimEvent refEvent;
		try {			
			refEvent = new SimEvent(notification,process,destination,null);
			Object params[];
			if(parameters!=null){
				params=new Object[1+parameters.length];
				params [0]=refEvent;
				for (int i=0;i<parameters.length;i++){
					params[i+1]=parameters[i];
				}
			}
			else{
				params=new Object[1];
				params[0]=refEvent;
			}
			refEvent.setParameters(params);
			refEvent.setOneShot(true);
			refEvent.setSchedulerListener(new MySchedulerListener());			
			//this.initReferencedEvent(refEvent);
			this.addReferencedEvent(refEvent);
		} catch (InvalidParamsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
	}
	
	public void notifyNoDelay(String notification,SimObject destination,Object[] parameters) {
		try {
			this.notify(notification, destination, parameters, new NoDelayProcess("noDelayProcess"));
		} catch (InvalidParamsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void notifyWithDelay(String notification, SimObject destination, Object[] parameters,float delay) {
		try {
			this.notify(notification,destination,parameters, new DelayProcess("delayProcess",delay));
		} catch (InvalidParamsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	  

}
