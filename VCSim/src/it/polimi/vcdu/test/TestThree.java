package it.polimi.vcdu.test;

import it.polimi.vcdu.alg.Measuring;
import it.polimi.vcdu.alg.Quiescence;
import it.polimi.vcdu.alg.VersionConsistency;
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
import it.polimi.vcdu.util.RandUtils;
import it.polimi.vcdu.util.TopologyGenerator;
import it.unipr.ce.dsg.deus.core.Engine;
import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;


import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class TestThree  {
	static int vid=1;
	static int eid=1;
	
	static float totalWorkingTimeWhenReady = 0;
	static float totalWorkingTimeWhenRequesting = 0;
	static float RequestTime = -1.0f;
	static float ReadyTime = -1.0f;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		prepLogging(Level.WARNING);
//		prepLogging(Level.INFO);
		
		Logger logger = Logger.getLogger("it.polimi.vcdu");
		logger.setLevel(Level.FINEST);
		java.util.logging.Handler [] handlers = logger.getHandlers();
		try {
			java.util.logging.FileHandler fh = new java.util.logging.FileHandler("simlog.txt");
			fh.setFormatter(new java.util.logging.SimpleFormatter());
			logger.setLevel(Level.FINEST);
			logger.addHandler(fh);
			fh.setLevel(Level.FINEST);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

/*		if (handlers.length == 0) logger.addHandler(new java.util.logging.ConsoleHandler());
		logger.getHandlers()[0].setLevel(Level.INFO);*/
//		Logger.getLogger("it.polimi.vcdu.alg").setLevel(Level.WARNING);

		ControlParameters.getCurrentParameters().setMaxVirtualTime(10000f);

		int nNodes = 128;
		int nEdges = 2;

		Graph<Number,Number> configGraph = initConfigGraph(nNodes, nEdges);
		
		RequestTime = 179.7979f;
		

///* Experiment Quiescence */
				
				reInit();
				Configuration conf0 = new Configuration(configGraph);
				Component targetedComponent0 = conf0.getComponentFromId("C1");
				expQuiescence(conf0,targetedComponent0,RequestTime);
//		
///* Experiment Version Consistency */
		
		reInit();
		Configuration conf = new Configuration(configGraph);
		Component targetedComponent = conf.getComponentFromId("C1");
		expVersConsistency(conf,targetedComponent,RequestTime);

/* Experiment measurement */
		reInit();

		Configuration conf2 = new Configuration(configGraph);
		Component targetedComponent2 = conf2.getComponentFromId("C1");

		exp_measuring(conf2,targetedComponent2, RequestTime, ReadyTime);
		
		
		System.exit(0);
	}


	private static void reInit(){
		vid = 1;
		eid = 1;
		totalWorkingTimeWhenReady = 0;
		totalWorkingTimeWhenRequesting = 0;
		ControlParameters.getCurrentParameters().reInit();
		RandUtils.reInit();
	}
	
	@SuppressWarnings("unused")
	private static void prepLogging(Level newlevel) {
		Logger logger = Logger.getLogger("it.polimi.vcdu");
		java.util.logging.Handler[] handlers = logger.getHandlers();
		if (handlers.length == 0) logger.addHandler(new java.util.logging.ConsoleHandler());
		logger.setLevel(newlevel);
		logger.getHandlers()[0].setLevel(newlevel);
	}

	private static void expVersConsistency(Configuration conf, Component targetedComponent, float triggeringTime){
		Simulator sim = new Simulator (conf,VersionConsistency.class);
		SimContainer simContainer= sim.getSimContainer(targetedComponent);
		
		try {
			Object[] content = new Object[1];
			content[0] = "startReconf";

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
					params, triggeringTime);

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
				RequestTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				for (Component com:conf.getComponents()){
					totalWorkingTimeWhenRequesting += com.getTotalWorkingTime();
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				ReadyTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				for (Component com:conf.getComponents()){
					totalWorkingTimeWhenReady += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Version Consistency \n\t RequestTime: "+ RequestTime
				+" total working time when request: "+ totalWorkingTimeWhenRequesting
				+"\n\t ReadyTime: "+ReadyTime + " total working time when ready: "+ totalWorkingTimeWhenReady);					
		
	}
	
	
	private static void exp_measuring(Configuration conf, Component targetedComponent, float requestTime, float readyTime) {
		Simulator sim = new Simulator(conf, Measuring.class);
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
					params, requestTime);
			
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
					params2, readyTime);

		} catch (InvalidParamsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		simContainer.getAlgorithm().setCollectReqSettingCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				RequestTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				for (Component com:conf.getComponents()){
					totalWorkingTimeWhenRequesting += com.getTotalWorkingTime();
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				ReadyTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				for (Component com:conf.getComponents()){
					totalWorkingTimeWhenReady += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Measurement: \n\t RequestTime: "+ RequestTime
				+" total working time when request: "+ totalWorkingTimeWhenRequesting
				+"\n\t ReadyTime: "+ReadyTime + " total working time when ready: "+ totalWorkingTimeWhenReady);
	};

	
	
	private static void expQuiescence(Configuration conf, Component targetedComponent, float triggeringTime) {
		Simulator sim = new Simulator(conf, Quiescence.class);
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
					params, triggeringTime);

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
				RequestTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				for (Component com:conf.getComponents()){
					totalWorkingTimeWhenRequesting += com.getTotalWorkingTime();
				}
			}
			
		});		
		simContainer.getAlgorithm().setCollectResultCallBack(new CallBack(simContainer){
			@Override
			public void callback(SimEvent currentEvent, Object[] parameters) {
				Simulator.getDefaultSimulator().setStopSimulation(true);
				ReadyTime = Engine.getDefault().getVirtualTime();
				Configuration conf = currentEvent.getSimObject().getHostComponent().getConf();
				for (Component com:conf.getComponents()){
					totalWorkingTimeWhenReady += com.getTotalWorkingTime();
				}
			}
			
		});		
		sim.run();
		
		Logger.getLogger("it.polimi.vcdu").info("*** Experiment with Quiescence: \n\t RequestTime: "+ RequestTime
				+" total working time when request: "+ totalWorkingTimeWhenRequesting
				+"\n\t ReadyTime: "+ReadyTime + " total working time when ready: "+ totalWorkingTimeWhenReady);
	}

	
	/**
	 * @param nNodes
	 * @param nEdges
	 * @return
	 */
	public static Graph<Number, Number> initConfigGraph(int nNodes, int nEdges) {

		// Creating the graph
		Factory<Graph<Number, Number>> fg = new Factory<Graph<Number, Number>>() {
			@Override
			public Graph<Number, Number> create() {
				return Graphs
						.synchronizedDirectedGraph(new DirectedSparseMultigraph<Number, Number>());
			}
		};
		Factory<Number> fv = new Factory<Number>() {
			@Override
			public Number create() {
				return vid++;
			};
		};
		Factory<Number> fe = new Factory<Number>() {
			@Override
			public Number create() {
				return eid++;
			};
		};

		TopologyGenerator<Number, Number> tg = new TopologyGenerator<Number, Number>(
				nNodes, nEdges, ControlParameters.getCurrentParameters()
						.getSeed(), fg, fv, fe);

		Graph<Number, Number> mg = tg.generate();

		GraphMLWriter<Number, Number> wr = new GraphMLWriter<Number, Number>();
		java.io.FileWriter fwr = null;
		try {
			fwr = new java.io.FileWriter("simplegraph.gml");
			wr.save(mg, fwr);
			fwr.close();
		} catch (IOException e) {
			System.err.println("Can not save graph to simplegraph.xml!");
			e.printStackTrace();
			System.exit(1);
		}
		tg.visualize(new ToStringLabeller<Number>());

		return mg;
	}

}
