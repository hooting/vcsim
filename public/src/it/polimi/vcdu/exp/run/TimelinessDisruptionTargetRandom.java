package it.polimi.vcdu.exp.run;

import it.polimi.vcdu.exp.ExperimentSetTargetRandomServer;

import java.io.IOException;

/**
 * We use this program to evaluate:
 *    1. the impact of the scale of the system. In this case we fix the network delay and vary the number of components. 
 *    2. the impact of the network latency. In this case we fix the number of components and vary the value of network delay.
 * @author xxm
 *
 */
public class TimelinessDisruptionTargetRandom {
		
	public static void main(String[] args) {
		//the number of edges is the same for each graph
		int nEdges=2;
		
		int nComponents=16;  // by setting this value to 4,8,16 and 32, respectively, we can evaluate the performance for different scale of systems.
		
		int masterSeed=123456;		
		int nRun=100;
		float localProcessingTime=50;
		float meanArrival=25;
		
		
		//When evaluating the impact of system scale, we would like to set network delay to 5, and experiment with different nComponennts
		//float[] delays = new float[]{5};
		
		//When evaluating the impact of different level of network delays, we fix nComponents to 16, and experiment with different network delays:
		float[] delays = new float[]{0,1,10,25,50,100};
		
		for(int i = 0; i< delays.length;i++){
			float delay = delays[i];
			ExperimentSetTargetRandomServer expSet= new ExperimentSetTargetRandomServer(nComponents, nEdges, nRun, 
					delay,meanArrival, localProcessingTime, masterSeed, false,
					"resultsExperiments/timelinessDisruption/newexp_V"+nComponents+"E"+nEdges+"D"+delay+"N"+nRun+"TargetRandomServerNodes");
			try {
				expSet.run();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.gc();
		}
		System.out.println("Experiment Completed");
	}
}
