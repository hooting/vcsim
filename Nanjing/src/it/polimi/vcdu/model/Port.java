/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public abstract class Port {
	
	protected String id;
	private Component host;
	private StaticEdge incidentEdge;
	
	
	public Port(String id)
	{
		this.id=id;
	}
	public Port(String id, Component host)
	{
		this.id=id;
		this.host=host;
	}
		
	public String toString(){
		//return id+"@"+host.getId();
		if (representation == null){
			representation = id+"@"+host.getId();
		}
		return representation;
	}
	
	private String representation =null;
	
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
	 * @param host the host to set
	 */
	public void setHost(Component host) {
		this.host = host;
	}
	
	public boolean equals(Object port){
		Port p= (Port) port;
		return this.id.equals(p.id)&& this.host.equals(p.host);
	}
	/**
	 * @return the incidentEdge
	 */
	public StaticEdge getIncidentEdge() {
		return incidentEdge;
	}
	/**
	 * @param incidentEdge the incidentEdge to set
	 */
	public void setIncidentEdge(StaticEdge incidentEdge) {
		this.incidentEdge = incidentEdge;
	}




}
