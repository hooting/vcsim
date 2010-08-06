/**
 * 
 */
package it.polimi.vcdu.sim;


import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Message;
import it.unipr.ce.dsg.deus.core.Engine;

import java.util.HashMap;


/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class SimNet extends SimObject {
	/**
	 * @param component
	 */
//	Port port;
	private SimContainer simContainer;
	private static HashMap<Component,SimNet> simNetMap = new HashMap<Component,SimNet> ();
	public SimNet(Component component, SimContainer simContainer) {		
		super(component);
		this.simContainer=simContainer;
		this.simContainer.setSimNet(this);
		simNetMap.put(component, this);
	}
	


	public void onSend(SimEvent e, Message message)
	{
		LOGGER.fine("SimNet of compoment "+ this.getHostComponent().getId()+" sending "
				+ message.getId() +"("+message.getContent()+")"
				+ " from "+ message.getSource()  
				+" to "+ message.getDestination() 
				+" at VT " + Engine.getDefault().getVirtualTime() );
		Object parameters[]= new Object[1];
		parameters[0]=message;
		float delay= ControlParameters.getCurrentParameters().getNetworkDelay();
		message.setDelay(delay);
		e.notifyWithDelay("receive",simNetMap.get(message.getDestination().getHost()), parameters, delay);
				
	}
	
	public void receive(SimEvent e,Message message)
	{
		Object[] params = new Object[1];
		params[0] = message;
		e.notifyNoDelay(message.getId(), this.simContainer, params);
		
	}
	
	public static void reInit(){
		simNetMap.clear();
	}
}
