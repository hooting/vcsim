/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class OutPort extends Port{

	private InPort peerPort;
	/**
	 * @param id
	 */
	public OutPort(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}
	/**
	 * @return the peerPort
	 */
	public InPort getPeerPort() {
		return peerPort;
	}
	/**
	 * @param peerPort the peerPort to set
	 */
	public void setPeerPort(InPort peerPort) {
		this.peerPort = peerPort;
	}
	
	
}
