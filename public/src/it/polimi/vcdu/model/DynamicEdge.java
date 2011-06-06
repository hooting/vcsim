/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class DynamicEdge extends Edge{
/**
	 * @param from
	 * @param to
	 * @param rid
	 */
	public DynamicEdge(OutPort from, InPort to, String rid) {
		super(from, to);
		this.rid = rid;
	}
//	private Transaction transaction;
	private String rid;
/*	public DynamicEdge (Transaction transaction, OutPort from, InPort to){
		super(from,to);
		this.transaction=transaction;
	}
	*//**
	 * @return the transaction
	 *//*
	public Transaction getTransaction() {
		return transaction;
	}
	*//**
	 * @param transaction the transaction to set
	 *//*
	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}*/
	
	
	@Override 
	public boolean equals (Object e){
		DynamicEdge edge= (DynamicEdge) e;
		if (e==null) return false;
		
		return (this.getFrom().equals((edge.getFrom())) && this.getTo().equals((edge.getTo()))  && this.rid.equals((edge.getRid())));
	}
	/**
	 * @return the rid
	 */
	public String getRid() {
		return rid;
	}
	/**
	 * @param rid the rid to set
	 */
	public void setRid(String rid) {
		this.rid = rid;
	}
	
	public String toString(){
		if(representation == null){
			String ForP = (this instanceof FutureEdge? "F":"P");
			representation = this.getFrom()+"-"+ForP+"-"+rid+"-"+this.getTo();
		}
		return representation;
	}
	
	protected String representation = null;
}
