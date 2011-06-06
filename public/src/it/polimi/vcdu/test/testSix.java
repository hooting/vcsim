package it.polimi.vcdu.test;

import it.polimi.vcdu.exp.Overhead;

import java.io.IOException;

public class testSix {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
		Overhead expOvh= new Overhead(16, 2, 10, 100,25, 50, 123456,11, "testSix", "C1");
		try {
			expOvh.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Experiment Completed");
	}

}
