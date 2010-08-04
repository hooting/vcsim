package it.polimi.vcdu.test;

import it.polimi.vcdu.exp.Experiment;
import it.polimi.vcdu.sim.ControlParameters;
import it.polimi.vcdu.util.TopologyGenerator;

import java.io.IOException;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class testFour {

	static int vid=1;
	static int eid=1;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ControlParameters.getCurrentParameters().setMaxVirtualTime(10000f);
		ControlParameters.getCurrentParameters().setMeanArrival(25);

		int nNodes = 32;
		int nEdges = 2;

		Graph<Number,Number> configGraph = initConfigGraph(nNodes, nEdges);
		
		float requestTime = 179.7979f;
		
		Experiment exp= new Experiment(configGraph,"C1",requestTime);
		exp.run();
		System.out.println(exp.getResult());
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
