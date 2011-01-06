/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class InPort extends Port{

	private OutPort peerPort;
	/**
	 * @param id
	 */
	public InPort(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}
	/**
	 * @return the peerPort
	 */
	public OutPort getPeerPort() {
		return peerPort;
	}
	/**
	 * @param peerPort the peerPort to set
	 */
	public void setPeerPort(OutPort peerPort) {
		this.peerPort = peerPort;
	}

	


}
