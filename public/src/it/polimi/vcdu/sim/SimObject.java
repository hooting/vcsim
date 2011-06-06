/**
 * 
 */
package it.polimi.vcdu.sim;


import it.polimi.vcdu.model.Component;

import java.util.logging.Logger;


/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class SimObject {
	
	protected final static Logger LOGGER = Logger.getLogger(SimObject.class.getName());
	
	private Component hostComponent;

	public SimObject(Component component)
	{
		this.hostComponent=component;
		LOGGER.fine(this.getClass().getCanonicalName() +" created at component "+ hostComponent.getId());
	}

	/**
	 * @return the component
	 */
	public Component getHostComponent() {
		return hostComponent;
	}


}
