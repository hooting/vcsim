	/**
 * 
 */
package it.polimi.vcdu.sim;

import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.Process;
/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class DelayProcess extends Process {

	/**
	 * @param id
	 * @param params
	 * @param referencedNodes
	 * @param referencedEvents
	 * @throws InvalidParamsException
	 */
	private float delay;
	public DelayProcess(String id, float delay)
			throws InvalidParamsException {
		super(id, null, null,null);
		this.delay=delay;
		
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see it.unipr.ce.dsg.deus.core.Process#getNextTriggeringTime(it.unipr.ce.dsg.deus.core.Event, float)
	 */
	@Override
	public float getNextTriggeringTime(Event event, float virtualTime) {
		
		return virtualTime+delay;
	}

	/* (non-Javadoc)
	 * @see it.unipr.ce.dsg.deus.core.Process#initialize()
	 */
	@Override
	public void initialize() throws InvalidParamsException {
		// TODO Auto-generated method stub
		
	}

}
