/**
 * 
 */
package it.polimi.vcdu.model;

import it.polimi.vcdu.sim.ControlParameters;

import java.util.ArrayList;


/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class Transaction {
	
	private String id;
	private Component host;
	private Iteration iteration;
	private ArrayList<String> ancestors;

	public Transaction(Component host, ArrayList<String> ancestors) {
		this.id=host.getId()+"_T"+host.getTx_counter();
		host.incrementTx_counter();
		this.ancestors = new ArrayList<String>(ancestors);
		this.ancestors.add(this.id);
		this.host=host;
		float localProcessingTime= ControlParameters.getCurrentParameters().getLocalProcessingTime();
		this.iteration=new Iteration(localProcessingTime,host);
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the host
	 */
	public Component getHost() {
		return host;
	}


	/**
	 * @return the iteration
	 */
	public Iteration getIteration() {
		return iteration;
	}

	/**
	 * @return the isRoot
	 */
	public boolean isRoot() {
		return this.ancestors.size()==1;
	}

	/**
	 * @return the rootId
	 */
	public String getRootId() {
		return ancestors.get(0);
	}

	/**
	 * @return the ancestors
	 */
	public ArrayList<String> getAncestors() {
		return ancestors;
	}

	public String toString(){
		return ancestors.toString();
	}
}
