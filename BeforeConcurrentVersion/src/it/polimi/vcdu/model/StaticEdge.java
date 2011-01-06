/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class StaticEdge extends Edge {

	/**
	 * @param from
	 * @param to
	 */
	public StaticEdge(OutPort from, InPort to) {
		super(from, to);
		// TODO Auto-generated constructor stub
	}
	
	public String toString(){
		return getFrom()+"-S-"+getTo();
	}

}
