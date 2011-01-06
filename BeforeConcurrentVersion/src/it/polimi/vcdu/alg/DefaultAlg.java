package it.polimi.vcdu.alg;
import it.polimi.vcdu.sim.SimContainer;

import java.util.logging.Logger;


public class DefaultAlg extends Algorithm {
	public DefaultAlg(SimContainer simCon) {
		super(simCon);
		// TODO Auto-generated constructor stub
	}

	private final static Logger LOGGER = Logger.getLogger(DefaultAlg.class.getName());
	
	@Override
	protected Logger getLOGGER(){
		return LOGGER;
	}
	
	
	
}
