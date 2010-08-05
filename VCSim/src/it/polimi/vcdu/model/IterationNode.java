/**
 * 
 */
package it.polimi.vcdu.model;

import it.polimi.vcdu.model.OutPort;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class IterationNode {
	private float delay;
	private OutPort outPort;
	private boolean subTxInitAcked;
	public IterationNode (float delay, OutPort outPort){
		this.delay=delay;
		this.outPort=outPort;
		setSubTxInitAcked(false);
	}
	/**
	 * @return the delay
	 */
	public float getDelay() {
		return delay;
	}
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(float delay) {
		this.delay = delay;
	}
	/**
	 * @return the outPort
	 */
	public OutPort getOutPort() {
		return outPort;
	}
	/**
	 * @param outPort the outPort to set
	 */
	public void setOutPort(OutPort outPort) {
		this.outPort = outPort;
	}
	
	public String toString(){
		return "("+delay+","+outPort+")";
	}
	/**
	 * @param subTxInitAcked the subTxInitAcked to set
	 */
	public void setSubTxInitAcked(boolean subTxInitAcked) {
		this.subTxInitAcked = subTxInitAcked;
	}
	/**
	 * @return the subTxInitAcked
	 */
	public boolean isSubTxInitAcked() {
		return subTxInitAcked;
	}

}
