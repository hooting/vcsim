package it.polimi.vcdu.exp;

import it.polimi.vcdu.exp.Experiment.Result;
import it.polimi.vcdu.sim.ControlParameters;
import it.polimi.vcdu.util.TopologyGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

/* Given a fixed size of the graph and a given number of configurations, 
* showing the timeliness and the disruption of the VC in the 
* two versions:blocking vs waiting  by varying the delay
* The target component is fixed. I would propose C1
*/
public class WaitingBlocking  {
	private int nNodes;
	private int nEdges;
	private Graph<Number,Number> graph;
	//how many times to run one experiment
	private int runNumber;	
	private float delay;
	private float maxArrival;
	private float localProcessingTime;	
	private int masterSeed;
	private float step;
	
	private String id;
	private String targetComponent;
	/**
	 * @param nNodes
	 * @param nEdges
	 * @param runNumber
	 * @param delay
	 * @param maxArrival
	 * @param localProcessingTime
	 * @param masterSeed
	 * @param nSteps
	 * @param id
	 * @param targetComponent
	 */
	public WaitingBlocking(int nNodes, int nEdges, int runNumber, float delay,
			float maxArrival, float localProcessingTime, int masterSeed,
			float step, String id, String targetComponent) {
		super();
		this.nNodes = nNodes;
		this.nEdges = nEdges;
		this.runNumber = runNumber;
		this.delay = delay;
		this.maxArrival = maxArrival;
		this.localProcessingTime = localProcessingTime;
		this.masterSeed = masterSeed;
		this.step=step;
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
		params.setNetworkDelay(delay);
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
		fw.write("Seed,ReqTime, vcFreenessTime,deltaFT\n" );
		
		Experiment exp;
		boolean waitingVC;
		for(float arrivalRate=this.maxArrival;arrivalRate>this.step; arrivalRate-=step){
			fw.write("### BLOCKING VERSION CONSISTENCY ###\n");
			fw.write("arrivalRate,"+arrivalRate+"\n" );
			waitingVC=false;
			params.setMeanArrival(arrivalRate);
			for (int i=0;i<runNumber; i++){		
				
				params.setSeed(seeds[i]);
				params.reInit();
				
				exp= new Experiment(this.graph,this.targetComponent,reqTime,waitingVC);
				exp.run();
				Result res= exp.getResult();
	
				float deltaFT=res.vcFreenessTime-res.reqTime;
				fw.write(seeds[i]+","+res.reqTime+","+res.vcFreenessTime+","+deltaFT+"\n" );
				
			}
			fw.write("Average\n" );
			fw.write("STDEV\n" );
			fw.write("STDERROR\n" );
			
			fw.write("### WAITING VERSION CONSISTENCY ###\n");
			fw.write("arrivalRate,"+arrivalRate+"\n" );
			waitingVC=false;
			params.setMeanArrival(arrivalRate);
			for (int i=0;i<runNumber; i++){		
				
				params.setSeed(seeds[i]);
				params.reInit();
				
				exp= new Experiment(this.graph,this.targetComponent,reqTime,waitingVC);
				exp.expVersConsistency();
				Result res= exp.getResult();
	
				
				float deltaFT=res.vcFreenessTime-res.reqTime;
				fw.write(seeds[i]+","+res.reqTime+","+res.vcFreenessTime+","+deltaFT+"\n" );
				
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
