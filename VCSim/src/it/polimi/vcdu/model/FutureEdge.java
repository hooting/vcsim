/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class FutureEdge extends DynamicEdge{

	/**
	 * @param transaction
	 * @param from
	 * @param to
	 */
	public FutureEdge(OutPort from, InPort to,String rid) {
		super(from, to,rid);
		
	}
	
/*	public boolean equals (Object e){
		FutureEdge edge= (FutureEdge) e;
		return (this.getFrom().equals(edge.getFrom()) && this.getTo().equals(edge.getTo()) && this.getTransaction().equals(edge.getTransaction()));
	}
*/
}
