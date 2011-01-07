package it.polimi.vcdu.exp.run;

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
import it.polimi.vcdu.util.RandUtils;
import it.polimi.vcdu.util.TopologyGenerator;
import it.unipr.ce.dsg.deus.core.Engine;
import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;


public class WaitingBlocking {
	static int vid = 1;
	static int eid = 1;
	
	static float totalWorkingTimeWhenReady = 0;
	static float totalWorkingTimeWhenRequesting = 0;
	static float RequestTime = -1.0f;
	static float ReadyTime = -1.0f;
	static boolean blocking=true;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ControlParameters.getCurrentParameters().setMaxVirtualTime(200000f);
		float blockingTimes[]=new float[100];
		float waitingTimes[]=new float[100];
		
		int nNodes = 16;
		int nEdges = 2;

//		int nNodes = 2;
//		int nEdges = 1;
		Graph<Number,Number> configGraph = initConfigGraph(nNodes, nEdges);

		ControlParameters.getCurrentParameters().setNetworkDelay(5);
		ControlParameters.getCurrentParameters().setMeanArrival(400);
		RequestTime = 2500f;
		String targetString="C2";
		int [] seeds= new int[100];
		Random rnd = ControlParameters.getCurrentParameters().getRandomObj();
		for (int i=0;i<100;i++){
			seeds[i]=rnd.nextInt();
		}
		for(int i=0;i<100;i++){
			ControlParameters.getCurrentParameters().setSeed(seeds[i]);
			blocking=true;
			reInit();
			expVCOD(configGraph, targetString);
			blockingTimes[i]=ReadyTime-RequestTime;
			
			ReadyTime=-10;
			blocking=false;
			reInit();
			expVCOD(configGraph, targetString);			
			waitingTimes[i]=ReadyTime-RequestTime;
			ReadyTime=-10;
		}
		
		float totalBlocking=0;
		float totalWait=0;
		System.out.println("\nblocking");
		for (int i=0;i<100;i++){
		
			System.out.print(blockingTimes[i]+"\t");
			totalBlocking+=blockingTimes[i];
		}
		System.out.println("\nwaiting");
		for (int i=0;i<100;i++){
	
			System.out.print(waitingTimes[i]+"\t");
			totalWait+=waitingTimes[i];
		}
		System.out.println("\nblocking average"+totalBlocking/100);
		System.out.println("\nwaiting average" +totalWait/100);
		
	}
	private static void expVCOD(Graph<Number, Number> configGraph,
			String targetString) {
		///* Experiment VCOnDemand default VC */
				
				reInit();
				
				
				Configuration conf = new Configuration(configGraph);
				
				
				VersionConsistencyOnDemand.DefaultDDMngMode = VersionConsistencyOnDemand.DDMngMode.DEFAULT;
				VersionConsistencyOnDemand.DefaultVCScope = null;
				
				Component targetedComponent = conf.getComponentFromId(targetString); //C2
				Simulator sim = new Simulator(conf, VersionConsistencyOnDemand.class);
				SimContainer simContainer = sim.getSimContainer(targetedComponent);
				
				try {
					Object[] content = new Object[1];
					if (blocking){
						content[0] = "onBeingRequestOnDemand";
					}
					else{
						content[0] ="onBeingRequestOnDemandWaiting";
					}
					
		//			content[0] = "startReconfUnderVC";
					
		
					Message message = new Message("VCODPsuadoMsg", null, null,
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
					reconfReqEvent.notifyWithDelay("dispatchToAlg", simContainer, params, RequestTime);
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
				
				Logger.getLogger("it.polimi.vcdu").info("*** Experiment with VCOnDemand: \n\t RequestTime: "+ RequestTime
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

	
	
	
	
	
	private static void reInit(){
		vid = 1;
		eid = 1;
		totalWorkingTimeWhenReady = 0;
		totalWorkingTimeWhenRequesting = 0;
		
		ControlParameters.getCurrentParameters().reInit();
		RandUtils.reInit();
	}
	


}
