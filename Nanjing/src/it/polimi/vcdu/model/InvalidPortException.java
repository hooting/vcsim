/**
 * this exception is executed whenever the components does not have exactly one Local Inport and exactly one local outport
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class InvalidPortException extends Exception {

	
	
	
	public InvalidPortException() {
		super("Invalid Port Exception: the component must have one local inport and one local outport");
		
	}
	
	/**
	 * @param message
	 */
	public InvalidPortException(String message) {
		super(message);
	
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
