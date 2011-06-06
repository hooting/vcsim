/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public abstract class Edge {

	private OutPort from;
	private InPort to;
		
	public Edge( OutPort from, InPort to){
		
		this.from=from;
		this.to=to;
	}

	/**
	 * @return the from OutPort
	 */
	public OutPort getFrom() {
		return from;
	}

	/**
	 * @param from the from to set
	 */
	public void setFrom(OutPort from) {
		this.from = from;
	}

	/**
	 * @return the to InPort
	 */
	public InPort getTo() {
		return to;
	}

	/**
	 * @param to the to to set
	 */
	public void setTo(InPort to) {
		this.to = to;
	}
	public boolean equals (Object e){
		if (e==null) return false;
		
		Edge edge= (Edge) e;
		return (this.getFrom().equals(edge.getFrom()) && this.getTo().equals(edge.getTo()));
	}


}
