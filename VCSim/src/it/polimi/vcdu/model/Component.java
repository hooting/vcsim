/**
 * 
 */
package it.polimi.vcdu.model;

import it.unipr.ce.dsg.deus.core.Engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class Component {
	
	private static final Logger LOGGER = Logger.getLogger(Component.class.getName());
	
	private String id;
	
	private final LocalInPort  localInPort;
	private final LocalOutPort localOutPort;

	private ArrayList<InPort> nonLocalInPorts=new ArrayList<InPort>();
	private ArrayList<OutPort> nonLocalOutPorts=new ArrayList<OutPort>();

	private Configuration conf;

	
	//Data Structures specific for the dynamic dependence algorithm
	private HashSet<Transaction> localTransactions= new HashSet<Transaction>();
	//dynamic edges from this node
	private HashSet<DynamicEdge> OES= new HashSet<DynamicEdge>();
	//dynamic edges to this node
	private HashSet<DynamicEdge> IES= new HashSet<DynamicEdge>();
	
	private int tx_counter=0;


	public Component(Configuration hostConf, String id ,ArrayList<InPort> nonLocalInPorts, ArrayList<OutPort> nonLocalOutPorts)
	{
		this.id=id;
		this.conf = hostConf;
		localInPort= new LocalInPort(id+"_LIP");
		localInPort.setHost(this);
		localOutPort= new LocalOutPort(id+"_LOP");
		localOutPort.setHost(this);
		this.nonLocalInPorts = nonLocalInPorts;
		this.nonLocalOutPorts = nonLocalOutPorts;
		
		for(Port p: this.nonLocalInPorts) p.setHost(this);
		for(Port p: this.nonLocalOutPorts) p.setHost(this);
		
	}

	public String toString(){
		String out =this.id+"\n";
		
		out+="Local InPort: "+localInPort.getId()+ "\n";
		out+="Local OutPort: "+localOutPort.getId() +"\n";

		if (this.nonLocalInPorts.isEmpty())
		{
			out+="\nNo In Ports \n";
		}
		else
		{
			Iterator <InPort> ip_it=this.nonLocalInPorts.iterator();
			out+="\nIn Ports:\n";
			while(ip_it.hasNext()){
				InPort ip=ip_it.next();
				out+=ip.getId()+"\t outPortPeer: "+ ip.getPeerPort().getId()+"\n";
			}
			
		}
		if (this.nonLocalOutPorts.isEmpty())
		{
			out+="\nNo Out Ports \n";
		}
		else
		{
			Iterator <OutPort> op_it=this.nonLocalOutPorts.iterator();
			out+="Out Ports:\n";
			while(op_it.hasNext()){
				OutPort op= op_it.next();
				out+=op.getId()+"\t inPortPeer: "+ op.getPeerPort().getId()+"\n";
			}
			
		}
		out+="\n###################################################\n";
		out+="OES:\n"+this.OES;
		out+="\n###################################################\n";
		out+="IES:\n"+this.IES;
		out+="\n###################################################\n";
		
		return out;
	}

	/**
	 * GETTERS AND SETTERS
	 */
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
	 * @return the localInPort
	 */
	public LocalInPort getLocalInPort() {
		return localInPort;
	}

	/**
	 * @return the localOutPort
	 */
	public LocalOutPort getLocalOutPort() {
		return localOutPort;
	}


	
	
	
	
	
	
	/**
	 * @return the nonLocalInPorts
	 */
	public ArrayList<InPort> getInPorts() {
		return nonLocalInPorts;
	}
	/**
	 * @return the nonLocalOutPorts
	 */
	public ArrayList<OutPort> getOutPorts() {
		return nonLocalOutPorts;
	}
/*	*//**
	 * @param ports
	 * @return
	 *//*
	private boolean hasNoLocalPorts(ArrayList<Port> ports) {
		Iterator <Port> i = ports.iterator();
		while (i.hasNext()){
			Port port=i.next();
			if ((port instanceof LocalInPort) || (port instanceof LocalOutPort)){
				return false;
			}
		}
		return true;
	}*/
	
	
	

	/**
	 * @return the conf
	 */

	public Configuration getConf() {
		return conf;
	}
	/**
	 * @param conf the conf to set
	 */

	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	/**
	 * @return the simAppTx
	 */

	/* for dynamic dependence algorithm*/
	
	//out-ports might be used in the future by transaction tx
	public HashSet<OutPort> f(Transaction tx){
		HashSet <OutPort> set= new HashSet<OutPort>();
		Iterator <IterationNode> it= tx.getIteration().getList().subList(tx.getIteration().getCurrent_step(), tx.getIteration().getList().size()-1).iterator();
		
		//it.next();
		while(it.hasNext()){
			IterationNode node= it.next();
			set.add(node.getOutPort());
		}
		return set;
	}
	
	public HashSet<OutPort> p(Transaction tx){
		HashSet <OutPort> set= new HashSet<OutPort>();
		Iterator <IterationNode> it= tx.getIteration().getList().subList(0,tx.getIteration().getCurrent_step()).iterator();
		while(it.hasNext()){
			IterationNode node= it.next();
			set.add(node.getOutPort());
		}
		return set;
	}
	public HashSet<OutPort> f(){
		return new HashSet<OutPort>(this.nonLocalOutPorts);
	}
	/**
	 * @return the on-going localTransactions
	 */
	public HashSet<Transaction> getLocalTransactions() {
		return localTransactions;
	}
	
	public void addLocalTransaction(Transaction tx){
		localTransactions.add(tx);
		
	}
	public void removeFromLocalTransactions(Transaction tx){
		localTransactions.remove(tx);
	}
	
	public void addToOES(DynamicEdge edge){
		LOGGER.fine("add dynamic edge: "+ edge +" to OES of Component "+this.getId());
		for(DynamicEdge de : OES){
			if (de.toString().equals(edge.toString())) return;
		}
		this.OES.add(edge);
	}
	public void removeFromOES(DynamicEdge edge){
		LOGGER.fine("remove dynamic edge: "+ edge +" from OES of Component "+this.getId());
		if(edge!=null){
			for(DynamicEdge de : OES){
				if (de.toString().equals(edge.toString()))
					{
						this.OES.remove(de);
						return;
					}
			}	
		}
	}
	public void addToIES(DynamicEdge edge){
		LOGGER.fine("add dynamic edge: "+ edge +" to IES of Component "+this.getId());
		for(DynamicEdge de : IES){
			if (de.toString().equals(edge.toString())) return;
		}
		this.IES.add(edge);
	}
	public void removeFromIES(DynamicEdge edge){
		LOGGER.fine("remove dynamic edge: "+ edge +" from IES of Component "+this.getId());
		if(edge!=null){
			for(DynamicEdge de : IES){
				if (de.toString().equals(edge.toString())){
					this.IES.remove(de);
					return;
				}
			}
		}
	}
	/**
	 * @return the oES
	 */

	public HashSet<DynamicEdge> getOES() {
		return OES;
	}
	/**
	 * @return the iES
	 */
	public HashSet<DynamicEdge> getIES() {
		return IES;
	}
	
	public FutureEdge getLocalFe(String rid){
	
		for(DynamicEdge fe: this.OES){
			if ((fe instanceof FutureEdge)&& (fe.getFrom().equals(localOutPort)) && fe.getRid().equals(rid)){
				return (FutureEdge)fe;
			}			
		}
		return null;
	}
	public PastEdge getLocalPe(String rid){
		
		for(DynamicEdge pe: this.OES){
			if ((pe instanceof PastEdge)&& (pe.getFrom().equals(localOutPort)) && pe.getRid().equals(rid)){
				return (PastEdge)pe;
			}			
		}
		return null;
	}
	public ArrayList<FutureEdge> getNonLocalFeFromOES(String rid)
	{
		ArrayList<FutureEdge> set= new ArrayList<FutureEdge>();
		for(Iterator<DynamicEdge> it=this.OES.iterator();it.hasNext();){
			DynamicEdge e= it.next();
			if ((e instanceof FutureEdge) && !(e.getFrom().equals(localOutPort))){
				if(e.getRid().equals(rid)){
					set.add((FutureEdge) e);
				}
				
			}
		}
		return set;
	}
	
	public ArrayList<FutureEdge> getNonLocalFeFromIES(String rid)
	{
		ArrayList<FutureEdge> set= new ArrayList<FutureEdge>();
		for(Iterator<DynamicEdge> it=this.IES.iterator();it.hasNext();){
			DynamicEdge e= it.next();
			if ((e instanceof FutureEdge) && !(e.getFrom().equals(localOutPort))){
				if(e.getRid().equals(rid)){
					set.add((FutureEdge) e);
				}
			}
		}
		return set;
	}
	public HashSet<Transaction> getLocalTransactionsWithRootRidAndPortOp(String rid, OutPort op){
		HashSet<Transaction> set= new HashSet<Transaction>();
		for (Iterator<Transaction>it=this.getLocalTransactions().iterator();it.hasNext();)
		{
			Transaction t1= it.next();
			if(t1.getRootId()==rid && this.f(t1).contains(op)){
				set.add(t1);	
			}
				
		}
		return set;
	}
	public HashSet<Transaction> getLocalTransactionsWithRootRid(String rid){
		HashSet<Transaction> set= new HashSet<Transaction>();
		for (Iterator<Transaction>it=this.getLocalTransactions().iterator();it.hasNext();)
		{
			Transaction t1= it.next();
			if(t1.getRootId()==rid ){
				set.add(t1);	
			}
				
		}
		return set;
	}
	
	/**
	 * @return the tx_counter
	 */
	public int getTx_counter() {
		return tx_counter;
	}

	public void incrementTx_counter() {
		tx_counter ++;
	}
	

	
	/**
	 * @return all incoming static edges
	 */
	public ArrayList<StaticEdge> getIncomingStaticEdges() {
		// currently we just assume for each in-port there is a static edge incidents to it.
		ArrayList<StaticEdge> vse = new ArrayList<StaticEdge>(this.nonLocalInPorts.size());
		for(InPort ip : this.nonLocalInPorts){
			vse.add(new StaticEdge(ip.getPeerPort(),ip));
		}
		return vse ;
	}


	/**
	 * Following are for working time / blocked time statistics
	 */
	
	private float accumulatedWorkingTime = 0; // only include those tx already ended.
	private float accumulatedBlockedTime = 0; // only include those tx already ended.
	
	private static class TxTimeRecord{
		ArrayList<String> txAncestors;
		float workingTime;
		float blockTime;
		public TxTimeRecord(ArrayList<String> ancestors, float workingTime, float blockedTime) {
			this.txAncestors = ancestors;
			this.workingTime = workingTime;
			this.blockTime = blockedTime;
		}
		static boolean txInHistory(Transaction tx, ArrayList<TxTimeRecord> history){
			for(TxTimeRecord tr: history){ 
				if (tr.txAncestors.equals(tx.getAncestors())) return true;
			}
			return false;
		}
	}
	private ArrayList<TxTimeRecord> txHistory = new ArrayList<TxTimeRecord>();
	
	public void accumulateTxRecordTime(Transaction tx){
		assert tx.getIteration().getTxEndTime() >=0 : "Tx must already end!";
		
		float workingTime = tx.getIteration().getWorkingTime();
		float blockedTime = tx.getIteration().getBlockedTime();
		
		assert ! TxTimeRecord.txInHistory(tx, this.txHistory) : "Tx already in history!";
		
		TxTimeRecord txTimeRecord = new TxTimeRecord(tx.getAncestors(),workingTime,blockedTime);
		txHistory.add(txTimeRecord);
		this.accumulatedWorkingTime += txTimeRecord.workingTime;
		this.accumulatedBlockedTime =+ txTimeRecord.blockTime;
		
		LOGGER.fine("*** Component: "+ this.getId()+" accumulatse Tx: " + tx + "'s workingTime: "+ workingTime 
				+"; TotalWorkingTime: "+ this.accumulatedWorkingTime
				+"\n VirtualTime: "+ Engine.getDefault().getVirtualTime());
	}
	
	/**
	 * Get total working time until now, including those of  all running transactions 
	 * @return
	 */
	public float getTotalWorkingTime(){
		float totalTime = this.getAccumulatedWorkingTime();
		for(Transaction tx:this.getLocalTransactions()){
			assert (!TxTimeRecord.txInHistory(tx, this.txHistory)) : "Tx should not be in history!";
			totalTime+=tx.getIteration().getCurrentWorkingTime();
		}
		return totalTime;
	}
	
	public float getTotalBlockingTime(){
		float totalTime = this.getAccumulatedBlockedTime();
		for(Transaction tx:this.getLocalTransactions()){
			assert TxTimeRecord.txInHistory(tx, this.txHistory) : "Tx should not be in history!";
			totalTime+=tx.getIteration().getCurrentBlockedTime();
		}
		return totalTime;
	}
	
	/**
	 * @return the accumulatedWorkingTime
	 */
	public float getAccumulatedWorkingTime() {
		return accumulatedWorkingTime;
	}

	/**
	 * @return the accumulatedBlockedTime
	 */
	public float getAccumulatedBlockedTime() {
		return accumulatedBlockedTime;
	}
	
//	public float 

	
}
