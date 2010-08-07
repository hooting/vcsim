package it.polimi.vcdu.exp.run;

import it.polimi.vcdu.exp.ExperimentSet;
import it.polimi.vcdu.exp.ExperimentSetTargetRandomServer;

import java.io.IOException;

public class TimelinessDisruptionScale4TargetRandom {
	public static void main(String[] args) {
		//the number of edges is the same for each graph
		int nEdges=2;
		int nComponents=8;
		int masterSeed=123456;		
		int nRun=50;
		float localProcessingTime=50;
		float meanArrival=25;
		float delay=50;			
		String target="C1";
		ExperimentSetTargetRandomServer expSet= new ExperimentSetTargetRandomServer(nComponents, nEdges, nRun, 
				delay,meanArrival, localProcessingTime, masterSeed, 
				"resultsExperiments/timelinessDisruption/scale"+nComponents+"TargetRandomServerNodes");
		try {
			expSet.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.gc();
		
		System.out.println("Experiment Completed");
	}
}
