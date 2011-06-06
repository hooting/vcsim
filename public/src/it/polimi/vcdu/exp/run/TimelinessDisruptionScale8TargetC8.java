package it.polimi.vcdu.exp.run;

import it.polimi.vcdu.exp.ExperimentSet;

import java.io.IOException;

public class TimelinessDisruptionScale8TargetC8 {
	public static void main(String[] args) {
		//the number of edges is the same for each graph
		int nEdges=3;
		int nComponents=8;
		int masterSeed=123456;		
		int nRun=50;
		float localProcessingTime=50;
		float meanArrival=25;
		float delay=5;			
		String target="C8";
		ExperimentSet expSet= new ExperimentSet(nComponents, nEdges, nRun, 
				delay,meanArrival, localProcessingTime, masterSeed, 
				"resultsExperiments/timelinessDisruption/scale"+nComponents+"Target"+target, target);
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
