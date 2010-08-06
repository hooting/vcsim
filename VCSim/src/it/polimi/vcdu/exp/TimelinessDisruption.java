package it.polimi.vcdu.exp;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Running contemporary the Timeliness and Disruption Experiments
 * Timeliness experiment: presenting the average timeliness of the two approaches 
 * by showing the ready time of VC and Quiescence
 * Disruption Experiment: presenting the loss of working time between the two approaches
 * Control Parameters:
 * 		1) Graph size: 4 different system scale: 
 * 						(nNodes,nEdges): (8,3) ; (16,3) ; (32,3); (64,3)
 * 		2) Number of configurations: 50 and then average
 * 		3) Delay: here it is fixed = I would propose (localProcessingTime)/10
 * 		4) Target components: C1 as example of server node, C_Last as example of edge node
 * 		5) MeanArrival Fixed (localProcessingTime)/2
 * 		6) LocalProcessing Time 50
 *  
 */
public class TimelinessDisruption {


	/**
	 * The result is stored in nScale*2 files: each of this contains the results 
	 * of the n configuration with a given system scale and a given target component
	 * @param args
	 */
	public static void main(String[] args) {
		//the number of edges is the same for each graph
		int nEdges=3;
		int [] systemScale= {8,16,32,64,128};
		int masterSeed=123456;		
		int nRun=50;
		float localProcessingTime=50;
		float meanArrival=25;
		float delay=5;		
		ExperimentSet expSetServer;
		ExperimentSet expSetEdge;
		for(int i=0; i<systemScale.length;i++){
			expSetServer= new ExperimentSet(systemScale[i], nEdges, nRun, 
					delay,meanArrival, localProcessingTime, masterSeed, 
					"resultsExperiments/timelinessDisruption/scale"+systemScale[i]+"server", "C1");
			try {
				expSetServer.run();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.gc();
			
			String target="C"+systemScale[i];
			expSetEdge= new ExperimentSet(systemScale[i], nEdges, nRun, 
					delay,meanArrival, localProcessingTime, masterSeed, 
					"resultsExperiments/timelinessDisruption/scale"+systemScale[i]+"edge", target);
			try {
				expSetEdge.run();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.gc();
		}
		System.out.println("Experiment Completed");
	}

}
