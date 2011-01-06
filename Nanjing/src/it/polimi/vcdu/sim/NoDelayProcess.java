/**
 * 
 */
package it.polimi.vcdu.sim;

import java.util.ArrayList;
import java.util.Properties;

import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.Node;
import it.unipr.ce.dsg.deus.core.Process;
/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class NoDelayProcess extends Process {

	/**
	 * @param id
	 * @param params
	 * @param referencedNodes
	 * @param referencedEvents
	 * @throws InvalidParamsException
	 */
	public NoDelayProcess(String id)
			throws InvalidParamsException {
		super(id, null, null,null);
		// TODO Auto-generated constructor stub
	}
	public NoDelayProcess(String id, Properties params,
			ArrayList<Node> referencedNodes, ArrayList<Event> referencedEvents)
			throws InvalidParamsException {
		super(id, params, referencedNodes, referencedEvents);
		initialize();
	}

	/* (non-Javadoc)
	 * @see it.unipr.ce.dsg.deus.core.Process#getNextTriggeringTime(it.unipr.ce.dsg.deus.core.Event, float)
	 */
	@Override
	public float getNextTriggeringTime(Event event, float virtualTime) {
		
		return virtualTime;
	}

	/* (non-Javadoc)
	 * @see it.unipr.ce.dsg.deus.core.Process#initialize()
	 */
	@Override
	public void initialize() throws InvalidParamsException {
		// TODO Auto-generated method stub
		
	}

}
