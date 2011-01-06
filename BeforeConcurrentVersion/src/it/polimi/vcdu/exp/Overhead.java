package it.polimi.vcdu.exp;
/*
 * Given a fixed size of the graph and a given number of configurations, 
 * showing the timeliness and the disruption of the two approach by varying the delay
 * The target component is fixed. I would propose C1
 */

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import it.polimi.vcdu.exp.Experiment.Result;
import it.polimi.vcdu.sim.ControlParameters;
import it.polimi.vcdu.util.TopologyGenerator;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class Overhead {
	private int nNodes;
	private int nEdges;
	private Graph<Number,Number> graph;
	//how many times to run one experiment
	private int runNumber;	
	private float maxDelay;
	private float meanArrival;
	private float localProcessingTime;	
	private int masterSeed;
	private float step;
	
	private String id;
	private String targetComponent;

	/**
	 * @param nNodes
	 * @param nEdges
	 * @param runNumber
	 * @param maxDelay
	 * @param meanArrival
	 * @param localProcessingTime
	 * @param masterSeed
	 * @param nSteps
	 * @param id
	 * @param targetComponent
	 */
	public Overhead(int nNodes, int nEdges, int runNumber, float maxDelay,
			float meanArrival, float localProcessingTime, int masterSeed,
			float step, String id, String targetComponent) {
		super();
		this.nNodes = nNodes;
		this.nEdges = nEdges;
		this.runNumber = runNumber;
		this.maxDelay = maxDelay;
		this.meanArrival = meanArrival;
		this.localProcessingTime = localProcessingTime;
		this.masterSeed = masterSeed;
		this.step = step;
		this.id = id;
		this.targetComponent = targetComponent;
		
	}


	int vid=1;
	int eid=1;
	

	public void run() throws IOException{
		FileWriter fw;
		fw= new FileWriter(id+"_exp.csv");		
		fw.write("# "+ this+"\n");
		ControlParameters params=ControlParameters.getCurrentParameters();
		params.setMeanArrival(meanArrival);
		params.setMeanServTime(localProcessingTime);
		//generating the array of seeds
		int [] seeds= new int[this.runNumber];
		Random rand= new Random(this.masterSeed);
		for (int i =0; i<runNumber;i++){
			seeds[i]= rand.nextInt();			
		}
		float reqTime= this.localProcessingTime *5;
		//generating the graph
		this.graph=this.initConfigGraph();
		fw.write("Seed,ReqTime, quiescenceTime,deltaQT, vcFreenessTime,deltaFT, workWhenFreenessF, workWhenFreenessM, workWhenQuiescenceM, workWhenQuiescenceQ, workWhenRequestF, workWhenRequestM"
				+ ", workWhenRequestQ, lossWorkByQu, lossWorkByVC\n" );
		
		Experiment exp;
		for(float delay=0;delay<this.maxDelay; delay+=step){
			fw.write("DELAY,"+delay+"\n" );
			for (int i=0;i<runNumber; i++){
				params.setNetworkDelay(delay);
				params.setSeed(seeds[i]);
				params.reInit();
				boolean waitingVC=false;
				exp= new Experiment(this.graph,this.targetComponent,reqTime,waitingVC);
				exp.run();
				Result res= exp.getResult();
	
				float deltaQT=res.quiescenceTime-res.reqTime;
				float deltaFT=res.vcFreenessTime-res.reqTime;
				fw.write(seeds[i]+","+res.reqTime+","+res.quiescenceTime+","+deltaQT+","+res.vcFreenessTime+","+deltaFT+","+res.workWhenFreenessF+","+res.workWhenFreenessM+","+res.workWhenQuiescenceM+","+res.workWhenQuiescenceQ+
						","+res.workWhenRequestF+","+res.workWhenRequestM+","+res.workWhenRequestQ+","+res.lossWorkByQu()+","+res.lossWorkByVC()+"\n" );
				
			}
			fw.write("Average\n" );
			fw.write("STDEV\n" );
			fw.write("STDERROR\n" );
		}
		fw.close();
	
	}
	
	
	/**
	 * @param nNodes
	 * @param nEdges
	 * @return
	 */
	public  Graph<Number, Number> initConfigGraph() {
		vid=1;
		eid=1;

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
//
//		GraphMLWriter<Number, Number> wr = new GraphMLWriter<Number, Number>();
//		java.io.FileWriter fwr = null;
//		try {
//			fwr = new java.io.FileWriter(this.id+"_graph.gml");
//			wr.save(mg, fwr);
//			fwr.close();
//		} catch (IOException e) {
//			System.err.println("Can not save graph to simplegraph.xml!");
//			e.printStackTrace();
//			System.exit(1);
//		}
		tg.visualize(new ToStringLabeller<Number>());

		return mg;
	}


}
