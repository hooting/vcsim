package it.polimi.vcdu.exp;

import it.polimi.vcdu.exp.Experiment.Result;
import it.polimi.vcdu.sim.ControlParameters;
import it.polimi.vcdu.sim.SimNet;
import it.polimi.vcdu.util.TopologyGenerator;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class ExperimentSet {

	private int nNodes;
	private int nEdges;
	private Graph<Number,Number> graph;
	//how many times to run one experiment
	private int runNumber;	
	private float messageDelay;
	private float meanArrival;
	private float localProcessingTime;	
	private int masterSeed;
	
	private String id;
	private String targetComponent;
	

	int vid=1;
	int eid=1;

	/**
	 * 
	 * @param nNodes
	 * @param nEdges
	 * @param runNumber
	 * @param messageDelay
	 * @param meanArrival
	 * @param localProcessingTime
	 * @param masterSeed
	 * @param id
	 * @param targetComponentId
	 */
	public ExperimentSet(int nNodes, int nEdges, int runNumber,
			float messageDelay, float meanArrival, float localProcessingTime,
			int masterSeed, String id,String targetComponentId) {
		super();
		this.nNodes = nNodes;
		this.nEdges = nEdges;
		this.runNumber = runNumber;
		this.messageDelay = messageDelay;
		this.meanArrival = meanArrival;
		this.localProcessingTime = localProcessingTime;
		this.masterSeed = masterSeed;
		this.id = id;
		this.targetComponent=targetComponentId;
		this.graph=initConfigGraph();
	}

	public void run() throws IOException{
		FileWriter fw;
		fw= new FileWriter(id+".csv");		
		fw.write("# "+ this+"\n");
		ControlParameters params=ControlParameters.getCurrentParameters();
		params.setMeanArrival(meanArrival);
		params.setMeanServTime(localProcessingTime);
		params.setNetworkDelay(messageDelay);
		//generating the array of seeds
		int [] seeds= new int[this.runNumber];
		Random rand= new Random(this.masterSeed);
		for (int i =0; i<runNumber;i++){
			seeds[i]= rand.nextInt();			
		}
		float reqTime= this.localProcessingTime *5;
	
		fw.write("Seed,ReqTime, quiescenceTime,deltaQT, vcFreenessTime,deltaFT, workWhenFreenessF, workWhenFreenessM, workWhenQuiescenceM, workWhenQuiescenceQ, workWhenRequestF, workWhenRequestM"
				+ ", workWhenRequestQ, lossWorkByQu, lossWorkByVC\n" );
		for (int i=0;i<runNumber; i++){
			params.setSeed(seeds[i]);
			params.reInit();
			//generating the graph
			this.graph=this.initConfigGraph();
			SimNet.reInit();
			boolean waitingVC=false;
			Experiment exp= new Experiment(this.graph,this.targetComponent,reqTime,waitingVC);
			exp.run();
			Result res= exp.getResult();

			float deltaQT=res.quiescenceTime-res.reqTime;
			float deltaFT=res.vcFreenessTime-res.reqTime;
			fw.write(seeds[i]+","+res.reqTime+","+res.quiescenceTime+","+deltaQT+","+res.vcFreenessTime+","+deltaFT+","+res.workWhenFreenessF+","+res.workWhenFreenessM+","+res.workWhenQuiescenceM+","+res.workWhenQuiescenceQ+
					","+res.workWhenRequestF+","+res.workWhenRequestM+","+res.workWhenRequestQ+","+res.lossWorkByQu()+","+res.lossWorkByVC()+"\n" );
			
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
//		tg.visualize(new ToStringLabeller<Number>());

		return mg;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ExperimentSet [id=" + id + ", localProcessingTime="
				+ localProcessingTime + ", masterSeed=" + masterSeed
				+ ", meanArrival=" + meanArrival + ", messageDelay="
				+ messageDelay + ", nEdges=" + nEdges + ", nNodes=" + nNodes
				+ ", runNumber=" + runNumber + ", targetComponent="
				+ targetComponent + "]";
	}

	

	
}
