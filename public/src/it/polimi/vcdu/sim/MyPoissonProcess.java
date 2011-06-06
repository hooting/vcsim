package it.polimi.vcdu.sim;


import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.Node;
import it.unipr.ce.dsg.deus.core.Process;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

/**
 * This class is my Poisson process. It accept one parameter
 * called "meanArrival" (float) that is used to generate the triggering time.
 * Each time the process receives a request for generating a new triggering
 * time, it will compute it by adding the current simulation virtual the value
 * of an Homogeneous Poisson Process with the rate parameter calculated as
 * 1/meanArrival time.
 * 
 * @author Matteo Agosti (agosti@ce.unipr.it)
 * @author Michele Amoretti (michele.amoretti@unipr.it)
 * 
 */
public class MyPoissonProcess extends Process {
	private static final String MEAN_ARRIVAL = "meanArrival";

	private float meanArrival = 0;

	public MyPoissonProcess(String id, Properties params,
			ArrayList<Node> referencedNodes, ArrayList<Event> referencedEvents)
			throws InvalidParamsException {
		super(id, params, referencedNodes, referencedEvents);
		initialize();
	}

	public float getMeanArrival() {
		return meanArrival;
	}

	public float getNextTriggeringTime(Event event, float virtualTime) {
//		return virtualTime + expRandom(event.getEventRandom(), (float) 1 / meanArrival);
		return virtualTime + expRandom(ControlParameters.getCurrentParameters().getRandomObj(), (float) 1 / meanArrival);
	}

	public void initialize() throws InvalidParamsException {
		if (params.getProperty(MEAN_ARRIVAL) == null)
			throw new InvalidParamsException(MEAN_ARRIVAL
					+ " param is expected.");

		try {
			meanArrival = Float.parseFloat(params.getProperty(MEAN_ARRIVAL));
		} catch (NumberFormatException ex) {
			throw new InvalidParamsException(MEAN_ARRIVAL
					+ " must be a valid float value.");
		}
	}

	// returns exponentially distributed random variable
	private float expRandom(Random random, float lambda) {
		float randomFloat = (float) (-Math.log(1-random.nextFloat()) / lambda);
		return randomFloat;
	}
}
