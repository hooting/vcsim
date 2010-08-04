/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class PastEdge extends DynamicEdge{

	/**
	 * @param transaction
	 * @param from
	 * @param to
	 */
	public PastEdge(OutPort from, InPort to,String rid) {
		super(from, to,rid);
		
	}

}
