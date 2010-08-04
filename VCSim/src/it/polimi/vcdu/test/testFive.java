package it.polimi.vcdu.test;

import java.io.IOException;

import it.polimi.vcdu.exp.ExperimentSet;

public class testFive {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
		ExperimentSet expSet= new ExperimentSet(4, 2, 50, 50,25, 50, 123456, "testFive02", "C1");
		try {
			expSet.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
