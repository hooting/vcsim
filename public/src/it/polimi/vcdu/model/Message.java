/**
 * 
 */
package it.polimi.vcdu.model;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class Message {
	/**
	 * @param outPort
	 * @param inPort
	 * @param delay
	 */
	/*public Message(String id,OutPort outPort, InPort inPort, float delay,float payload) {
		super();
		this.id=id;
		this.outPort = outPort;
		this.inPort = inPort;
		//Network delay
		this.delay = delay;
		//Processing payload
		this.payload=payload;
		
	}*/
	/*public Message(String id,OutPort outPort, InPort inPort, float delay,float payload,Object content) {
		super();
		this.id=id;
		this.outPort = outPort;
		this.inPort = inPort;
		//Network delay
		this.delay = delay;
		//Processing payload
		this.payload=payload;
		//content of the message, used to send the appropriate parameter to the algorithm
		this.content=content;		
	}*/
	public Message(String id,Port source, Port destination,Object content) {
		super();
		this.id=id;
		this.source=source;
		this.destination=destination;

		//content of the message, used to send the appropriate parameter to the algorithm
		this.content=content;		
	}
	private String id;
/*	private OutPort outPort;
	private InPort inPort;*/
	private Port source;
	private Port destination;
	private float delay;
//	private float payload;
	private Object content;
/*	*//**
	 * @return the outPort
	 *//*
	public OutPort getOutPort() {
		return outPort;
	}
	*//**
	 * @param outPort the outPort to set
	 *//*
	public void setOutPort(OutPort outPort) {
		this.outPort = outPort;
	}
	*//**
	 * @return the inPort
	 *//*
	public InPort getInPort() {
		return inPort;
	}
	*//**
	 * @param inPort the inPort to set
	 *//*
	public void setInPort(InPort inPort) {
		this.inPort = inPort;
	}*/
	/**
	 * @return the delay
	 */
	public float getDelay() {
		return delay;
	}
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(float delay) {
		this.delay = delay;
	}
	/**
	 * @return the payload
	 */
/*	public float getPayload() {
		return payload;
	}
	*//**
	 * @param payload the payload to set
	 *//*
	public void setPayload(float payload) {
		this.payload = payload;
	}*/
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
	 * @return the source
	 */
	public Port getSource() {
		return source;
	}
	/**
	 * @param source the source to set
	 */
	public void setSource(Port source) {
		this.source = source;
	}
	/**
	 * @return the destination
	 */
	public Port getDestination() {
		return destination;
	}
	/**
	 * @param destination the destination to set
	 */
	public void setDestination(Port destination) {
		this.destination = destination;
	}
	/**
	 * @return the content
	 */
	public Object getContent() {
		return content;
	}
	/**
	 * @param content the content to set
	 */
	public void setContent(Object content) {
		this.content = content;
	}
	
	
	

}
