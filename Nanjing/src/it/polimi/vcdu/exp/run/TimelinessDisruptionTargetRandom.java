package it.polimi.vcdu.exp.run;

import it.polimi.vcdu.exp.ExperimentSetTargetRandomServer;

import java.io.IOException;

public class TimelinessDisruptionTargetRandom {
	public static void main(String[] args) {
		//the number of edges is the same for each graph
		int nEdges=2;
		int nComponents=16;
		int masterSeed=123456;		
		int nRun=100;
		float localProcessingTime=50;
		float meanArrival=25;
		//float delay=5;			
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
