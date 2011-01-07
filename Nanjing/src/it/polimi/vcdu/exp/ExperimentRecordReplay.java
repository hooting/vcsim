package it.polimi.vcdu.exp;

import it.polimi.vcdu.alg.Measuring;
import it.polimi.vcdu.alg.Quiescence;
import it.polimi.vcdu.alg.VersionConsistencyOnDemand;
import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Configuration;
import it.polimi.vcdu.model.Message;
import it.polimi.vcdu.sim.CallBack;
import it.polimi.vcdu.sim.ControlParameters;
import it.polimi.vcdu.sim.MySchedulerListener;
import it.polimi.vcdu.sim.NoDelayProcess;
import it.polimi.vcdu.sim.SimContainer;
import it.polimi.vcdu.sim.SimEvent;
import it.polimi.vcdu.sim.Simulator;
import it.polimi.vcdu.sim.record.Recorder;
import it.polimi.vcdu.util.RandUtils;
import it.unipr.ce.dsg.deus.core.Engine;
import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;

import java.util.ArrayList;
import java.util.logging.Logger;

import edu.uci.ics.jung.graph.Graph;



public class ExperimentRecordReplay {
	private Result result;	
	Graph<Number,Number> configGraph;
	private String targetComponentName;
	private boolean waitingVC;
	
	
	public ExperimentRecordReplay(Graph<Number,Number> configGraph, String targetComponentName,float reqTime,boolean waitingVC){
		this.configGraph=configGraph;
		this.targetComponentName=targetComponentName;
		result=new Result();
		result.reqTime=reqTime;
		this.waitingVC=waitingVC;
		
	}
	
	public void run(){
		ControlParameters.getCurrentParameters().setMaxVirtualTime(20000f);//need to set in the ControlParameters
		Recorder recorder = expRecord();
		
		reInit();
		expQuiescence(recorder);
		reInit();
		//this.expVersConsistency();
		this.expOnDemandVersConsistency(recorder);
		reInit();
		this.expMeasuringQ(recorder);
		reInit();
		this.expMeasuringF(recorder);
	}
	
	public Result getResult(){
		assert result!=null;
		return result;
		
	}

	private void reInit(){
		ControlParameters.getCurrentParameters().reInit();
		RandUtils.reInit();
	}
	
	private Recorder expRecord() {
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Recording:");

		Configuration conf = new Configuration(configGraph);
		
		Simulator sim = new Simulator(conf, Measuring.class);
		
		sim.setRecording(true);
		Recorder recorder = sim.getRecorder();
		recorder.setMaxNumberOfTxs(100000); //need to set in the ControlParameters
		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Recording done");
		
		return sim.getRecorder();

	};
	
	private void expQuiescence(Recorder recorder) {
		Configuration conf = new Configuration(configGraph);
		Component targetedComponent = conf.getComponentFromId(this.targetComponentName);
		Simulator sim = new Simulator(conf, Quiescence.class, recorder);
		SimContainer simContainer = sim.getSimContainer(targetedComponent);
		
		try {
			Object[] content = new Object[1];
			content[0] = "onBeingPassivated";

			Message message = new Message("QuiescencPsuadoMsg", null, null,
					content);
			SimEvent reconfReqEvent = new SimEvent(null, null, null, null);
			reconfReqEvent.setSchedulerListener(new MySchedulerListener());
			ArrayList<Event> events = new ArrayList<Event>();

			events.add(reconfReqEvent);
			NoDelayProcess process = new NoDelayProcess("noDelay", null, null,
					events);

			Object[] params = new Object[1];
			params[0] = message;
			sim.insertProcess(process);
			sim.insertEvent(reconfReqEvent);
			reconfReqEvent.notifyWithDelay("dispatchToAlg", simContainer,
					params, result.reqTime);

			// the creation of SimEvent will affect global random, so we make a second event to 
			// help the measuring (who will new two events before sim run) to reproduce the exact
			// behavior. Note that we need two, another ome is within notifyWithDelay
			@SuppressWarnings("unused")
			SimEvent noUsereconfReqEvent1 = new SimEvent(null, null, null, null);
			@SuppressWarnings("unused")
			SimEvent noUsereconfReqEvent2 = new SimEvent(null, null, null, null);


		} catch (InvalidParamsException e) {
			e.printStackTrace();
			System.exit(1);
		}

		simContainer.getAlgorithm().setCollectReqSettingCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				float requestTime = Engine.getDefault().getVirtualTime();
				assert Math.abs(result.reqTime -requestTime) < 0.01;
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenRequestQ = 0f;
				for (Component com:conf.getComponents()){
					result.workWhenRequestQ += com.getTotalWorkingTime();
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				result.quiescenceTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenQuiescenceQ = 0f;
				for (Component com:conf.getComponents()){
					result.workWhenQuiescenceQ += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Quiescence: \n\t RequestTime: "+ result.reqTime
				+" total working time when request: "+ result.workWhenRequestQ
				+"\n\t ReadyTime: "+result.quiescenceTime + " total working time when ready: "+ result.workWhenQuiescenceQ);
	}
	
/*	public void expVersConsistency(){
		Configuration conf = new Configuration(configGraph);
		Component targetedComponent = conf.getComponentFromId(this.targetComponentName);
		Simulator sim = new Simulator (conf,VersionConsistency.class);
		SimContainer simContainer= sim.getSimContainer(targetedComponent);
		
		try {
			Object[] content = new Object[1];
			if(this.waitingVC){
				content[0] = "startReconfWaiting";
			}
			else{
				content[0] = "startReconf";
			}			

			Message message = new Message("VersConsPseudoMsg", null, null,
					content);
			SimEvent reconfReqEvent = new SimEvent(null, null, null, null);
			reconfReqEvent.setSchedulerListener(new MySchedulerListener());
			ArrayList<Event> events = new ArrayList<Event>();

			events.add(reconfReqEvent);
			NoDelayProcess process = new NoDelayProcess("noDelay", null, null,
					events);

			Object[] params = new Object[1];
			params[0] = message;
			sim.insertProcess(process);
			sim.insertEvent(reconfReqEvent);
			reconfReqEvent.notifyWithDelay("dispatchToAlg", simContainer,
					params, this.result.reqTime);

			// the creation of SimEvent will affect global random, so we make a second event to 
			// help the measuring (who will new two events before sim run) to reproduce the exact
			// behavior. Note that we need two, another ome is within notifyWithDelay
			@SuppressWarnings("unused")
			SimEvent noUsereconfReqEvent1 = new SimEvent(null, null, null, null);
			@SuppressWarnings("unused")
			SimEvent noUsereconfReqEvent2 = new SimEvent(null, null, null, null);


		} catch (InvalidParamsException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		
		simContainer.getAlgorithm().setCollectReqSettingCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				float requestTime = Engine.getDefault().getVirtualTime();
				assert Math.abs(result.reqTime -requestTime) < 0.01;
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenRequestF = 0f;
				for (Component com:conf.getComponents()){
					result.workWhenRequestF += com.getTotalWorkingTime();
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				result.vcFreenessTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenFreenessF = 0f;
				for (Component com:conf.getComponents()){
					result.workWhenFreenessF += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Version Consistency \n\t RequestTime: "+ result.reqTime
				+" total working time when request: "+ result.workWhenRequestF
				+"\n\t ReadyTime: "+result.vcFreenessTime + " total working time when ready: "+ result.workWhenFreenessF);					
		
	}*/
	
	public void expOnDemandVersConsistency(Recorder recorder){
		Configuration conf = new Configuration(configGraph);
		Component targetedComponent = conf.getComponentFromId(this.targetComponentName);
		//Simulator sim = new Simulator (conf,VCOnDemand.class);
		Simulator sim = new Simulator (conf,VersionConsistencyOnDemand.class,recorder);
		SimContainer simContainer= sim.getSimContainer(targetedComponent);
		
		try {
			Object[] content = new Object[1];
			if(this.waitingVC){
				content[0] = "onBeingRequestOnDemandWaiting";
			}
			else{
				content[0] = "onBeingRequestOnDemand";
			}			

			Message message = new Message("VersConsPseudoMsg", null, null,
					content);
			SimEvent reconfReqEvent = new SimEvent(null, null, null, null);
			reconfReqEvent.setSchedulerListener(new MySchedulerListener());
			ArrayList<Event> events = new ArrayList<Event>();

			events.add(reconfReqEvent);
			NoDelayProcess process = new NoDelayProcess("noDelay", null, null,
					events);

			Object[] params = new Object[1];
			params[0] = message;
			sim.insertProcess(process);
			sim.insertEvent(reconfReqEvent);
			reconfReqEvent.notifyWithDelay("dispatchToAlg", simContainer,
					params, this.result.reqTime);

			// the creation of SimEvent will affect global random, so we make a second event to 
			// help the measuring (who will new two events before sim run) to reproduce the exact
			// behavior. Note that we need two, another ome is within notifyWithDelay
			@SuppressWarnings("unused")
			SimEvent noUsereconfReqEvent1 = new SimEvent(null, null, null, null);
			@SuppressWarnings("unused")
			SimEvent noUsereconfReqEvent2 = new SimEvent(null, null, null, null);


		} catch (InvalidParamsException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		
		simContainer.getAlgorithm().setCollectReqSettingCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				float requestTime = Engine.getDefault().getVirtualTime();
				assert Math.abs(result.reqTime -requestTime) < 0.01;
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenRequestF = 0f;
				for (Component com:conf.getComponents()){
					result.workWhenRequestF += com.getTotalWorkingTime();
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				result.vcFreenessTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenFreenessF = 0f;
				for (Component com:conf.getComponents()){
					result.workWhenFreenessF += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Version Consistency \n\t RequestTime: "+ result.reqTime
				+" total working time when request: "+ result.workWhenRequestF
				+"\n\t ReadyTime: "+result.vcFreenessTime + " total working time when ready: "+ result.workWhenFreenessF);					
		
	}
	private void expMeasuringF(Recorder recorder) {
		Configuration conf = new Configuration(configGraph);
		Component targetedComponent = conf.getComponentFromId(this.targetComponentName);
		Simulator sim = new Simulator(conf, Measuring.class,recorder);
		SimContainer simContainer = sim.getSimContainer(targetedComponent);
		
		try {
			Object[] content = new Object[1];
			content[0] = "onFirstMeasurement";

			Message message = new Message("MeasurementPsuadoMsg", null, null,
					content);
			SimEvent reconfReqEvent = new SimEvent(null, null, null, null);
			reconfReqEvent.setSimObject(simContainer);
			reconfReqEvent.setSchedulerListener(new MySchedulerListener());
			ArrayList<Event> events = new ArrayList<Event>();

			events.add(reconfReqEvent);
			NoDelayProcess process = new NoDelayProcess("noDelay", null, null,
					events);

			Object[] params = new Object[1];
			params[0] = message;
			sim.insertProcess(process);
			sim.insertEvent(reconfReqEvent);
			reconfReqEvent.notifyWithDelay("dispatchToAlg", simContainer,
					params, result.reqTime);
			
			// Now do the second measurement
			Object[] content2 = new Object[1];
			content2[0] = "onSecondMeasurement";

			Message message2 = new Message("MeasurementPsuadoMsg2", null, null,
					content2);
			SimEvent reconfReqEvent2 = new SimEvent(null, null, null, null);
			reconfReqEvent2.setSimObject(simContainer);
			reconfReqEvent2.setSchedulerListener(new MySchedulerListener());
			ArrayList<Event> events2 = new ArrayList<Event>();

			events2.add(reconfReqEvent2);
			NoDelayProcess process2 = new NoDelayProcess("noDelay", null, null,
					events2);

			Object[] params2 = new Object[1];
			params2[0] = message2;
			sim.insertProcess(process2);
			sim.insertEvent(reconfReqEvent2);
			reconfReqEvent2.notifyWithDelay("dispatchToAlg", simContainer,
					params2, result.vcFreenessTime);
			

		} catch (InvalidParamsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		simContainer.getAlgorithm().setCollectReqSettingCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				float rt = Engine.getDefault().getVirtualTime();
				assert Math.abs(result.reqTime -rt) < 0.01;
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenRequestM = 0;
				for (Component com:conf.getComponents()){
					result.workWhenRequestM += com.getTotalWorkingTime();
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				float ft = Engine.getDefault().getVirtualTime();
				assert Math.abs(result.vcFreenessTime -ft) < 0.01;
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenFreenessM = 0;
				for (Component com:conf.getComponents()){
					result.workWhenFreenessM += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Measurement against Version Consistency: \n\t RequestTime: "+ result.reqTime
				+" total working time when request: "+ result.workWhenRequestM
				+"\n\t VCFreeTime: "+result.vcFreenessTime + " total working time when VCFree: "+ result.workWhenFreenessM);
	};

	
	private void expMeasuringQ(Recorder recorder) {
		Configuration conf = new Configuration(configGraph);
		Component targetedComponent = conf.getComponentFromId(this.targetComponentName);
		Simulator sim = new Simulator(conf, Measuring.class,recorder);
		SimContainer simContainer = sim.getSimContainer(targetedComponent);
		
		try {
			Object[] content = new Object[1];
			content[0] = "onFirstMeasurement";

			Message message = new Message("MeasurementPsuadoMsg", null, null,
					content);
			SimEvent reconfReqEvent = new SimEvent(null, null, null, null);
			reconfReqEvent.setSimObject(simContainer);
			reconfReqEvent.setSchedulerListener(new MySchedulerListener());
			ArrayList<Event> events = new ArrayList<Event>();

			events.add(reconfReqEvent);
			NoDelayProcess process = new NoDelayProcess("noDelay", null, null,
					events);

			Object[] params = new Object[1];
			params[0] = message;
			sim.insertProcess(process);
			sim.insertEvent(reconfReqEvent);
			reconfReqEvent.notifyWithDelay("dispatchToAlg", simContainer,
					params, result.reqTime);
			
			// Now do the second measurement
			Object[] content2 = new Object[1];
			content2[0] = "onSecondMeasurement";

			Message message2 = new Message("MeasurementPsuadoMsg2", null, null,
					content2);
			SimEvent reconfReqEvent2 = new SimEvent(null, null, null, null);
			reconfReqEvent2.setSimObject(simContainer);
			reconfReqEvent2.setSchedulerListener(new MySchedulerListener());
			ArrayList<Event> events2 = new ArrayList<Event>();

			events2.add(reconfReqEvent2);
			NoDelayProcess process2 = new NoDelayProcess("noDelay", null, null,
					events2);

			Object[] params2 = new Object[1];
			params2[0] = message2;
			sim.insertProcess(process2);
			sim.insertEvent(reconfReqEvent2);
			reconfReqEvent2.notifyWithDelay("dispatchToAlg", simContainer,
					params2,result.quiescenceTime );
			

		} catch (InvalidParamsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		simContainer.getAlgorithm().setCollectReqSettingCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				float rt = Engine.getDefault().getVirtualTime();
				assert Math.abs(result.reqTime -rt) < 0.01;
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenRequestM	 = 0;
				for (Component com:conf.getComponents()){
					result.workWhenRequestM += com.getTotalWorkingTime();					
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				float ft = Engine.getDefault().getVirtualTime();
				assert Math.abs(result.quiescenceTime -ft) < 0.01;
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				result.workWhenQuiescenceM = 0;
				for (Component com:conf.getComponents()){
					result.workWhenQuiescenceM += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Measurement against Quiescence: \n\t RequestTime: "+ result.reqTime
				+" total working time when request: "+ result.workWhenRequestM
				+"\n\t QuiescenceTime: "+result.quiescenceTime + " total working time when Quiescence achieved: "+ result.workWhenQuiescenceM);
	};

	
	
	
	public class Result{
		public float reqTime;
		public float quiescenceTime;
		public float vcFreenessTime;
		//measuring
		public float workWhenRequestM;
		//quiescence
		public float workWhenRequestQ;
		//freeness
		public float workWhenRequestF;
		
		public float workWhenQuiescenceM;
		
		public float workWhenQuiescenceQ;
		
		public float workWhenFreenessM;
		public float workWhenFreenessF;
		
		public float lossWorkByVC(){
			return (this.workWhenFreenessM-this.workWhenRequestM)-(this.workWhenFreenessF-this.workWhenRequestF);
		}
		
		public float lossWorkByQu(){
			return (this.workWhenQuiescenceM-this.workWhenRequestM)-(this.workWhenQuiescenceQ-this.workWhenRequestQ);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Result [quiescenceTime=" + quiescenceTime + ", reqTime="
					+ reqTime + ", vcFreenessTime=" + vcFreenessTime
					+ ", workWhenFreenessF=" + workWhenFreenessF
					+ ", workWhenFreenessM=" + workWhenFreenessM
					+ ", workWhenQuiescenceM=" + workWhenQuiescenceM
					+ ", workWhenQuiescenceQ=" + workWhenQuiescenceQ
					+ ", workWhenRequestF=" + workWhenRequestF
					+ ", workWhenRequestM=" + workWhenRequestM
					+ ", workWhenRequestQ=" + workWhenRequestQ + "]";
		}
		
	
	
	}
	
	
}

