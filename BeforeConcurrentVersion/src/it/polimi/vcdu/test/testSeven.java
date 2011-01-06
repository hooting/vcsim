package it.polimi.vcdu.test;

import java.io.IOException;

import it.polimi.vcdu.exp.ExperimentSet;
import it.polimi.vcdu.exp.Overhead;
import it.polimi.vcdu.exp.WaitingBlocking;

public class testSeven {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/**
		 * @param nNodes
		 * @param nEdges
		 * @param runNumber
		 * @param delay
		 * @param maxArrival
		 * @param localProcessingTime
		 * @param masterSeed
		 * @param step
		 * @param id
		 * @param targetComponent
		 */
		WaitingBlocking expWaitBlock= new WaitingBlocking(8, 2, 2, 5,90, 50, 123456,5, "testSeven", "C1");
		try {
			expWaitBlock.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Experiment Completed");
	}

}
