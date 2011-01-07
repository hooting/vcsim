package it.polimi.vcdu.alg;

import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.DynamicEdge;
import it.polimi.vcdu.model.FutureEdge;
import it.polimi.vcdu.model.Message;
import it.polimi.vcdu.model.OutPort;
import it.polimi.vcdu.model.PastEdge;
import it.polimi.vcdu.model.Transaction;
import it.polimi.vcdu.sim.CallBack;
import it.polimi.vcdu.sim.SimAppTx;
import it.polimi.vcdu.sim.SimContainer;
import it.polimi.vcdu.sim.SimEvent;
import it.unipr.ce.dsg.deus.core.Engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;


public class VersionConsistency extends Algorithm {
	private HashMap<String,Observable> WaitingForAckFutureCreateCondition= new HashMap<String, Observable>();
	public VersionConsistency(SimContainer simCon) {
		super(simCon);
		// TODO Auto-generated constructor stub
	}

	private final static Logger LOGGER = Logger.getLogger(VersionConsistency.class.getName());

//	private float VersConsistencyReqTime = -1.0f;
	private boolean startReconf=false;
	private HashSet<String> FPSet = new HashSet<String>();
	private boolean waitingInsteadOfBlocking=false;

	@Override
	protected Logger getLOGGER() {		
		return LOGGER;
	}
	
	
	public void startReconf(SimEvent currentEvent){
		getLOGGER().info("************** STARTING RECONFIGURATION OF COMPONENT "+this.getSimContainer().getHostComponent().getId()+" ***************\n " +
				"virtual time "+Engine.getDefault().getVirtualTime());
//		this.VersConsistencyReqTime=Engine.getDefault().getVirtualTime();
		this.startReconf=true;
		this.collectReqSettingCallBack.callback(currentEvent, null);
		getLOGGER().info(
//				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
//				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
				" \n *************** current FPSet : " + FPSet);
		
		checkFreeness(currentEvent);
	}
	public void startReconfWaiting(SimEvent currentEvent){
		getLOGGER().info("************** STARTING RECONFIGURATION OF COMPONENT "+this.getSimContainer().getHostComponent().getId()+" ***************\n " +
				"virtual time "+Engine.getDefault().getVirtualTime());
//		this.VersConsistencyReqTime=Engine.getDefault().getVirtualTime();
		this.startReconf=true;
		this.waitingInsteadOfBlocking=true;
		this.collectReqSettingCallBack.callback(currentEvent, null);
		getLOGGER().info(
//				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
//				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
				" \n *************** current FPSet : " + FPSet);
		
		checkFreeness(currentEvent);
	}
	
	@Override
	public void onInitRootTx(SimEvent currentEvent, SimAppTx simApp,CallBack callBack) {
	
		if (this.waitingInsteadOfBlocking|| !this.startReconf || FPSet.contains(simApp.getTransaction().getRootId())){
			Transaction tx= simApp.getTransaction();
			//Setting Up local edges
			Component host= simApp.getHostComponent();
			FutureEdge lfe= new FutureEdge( host.getLocalOutPort(),host.getLocalInPort(),tx.getId());
			PastEdge lpe= new PastEdge( host.getLocalOutPort(),host.getLocalInPort(),tx.getId());
			host.addToOES(lfe);
			host.addToOES(lpe);
			host.addToIES(lfe);
			host.addToIES(lpe);
			
			getLOGGER().fine("Before callback "+ currentEvent.getId()+
					" At virtual time: "+Engine.getDefault().getVirtualTime() +
					"; The root tx: "+tx.getAncestors());		
			callBack.callback(currentEvent, null);
		}
		else{
			getLOGGER().fine("*** VersionConsistency algorithm blocks transaction: "+ simApp.getTransaction() + " at VT: " 
					+ Engine.getDefault().getVirtualTime());
		}
	}
	@Override
	public void onToGoBeforeRootInitFirstSubTx(SimEvent currentEvent,  SimAppTx simApp,CallBack callBack) {
		Transaction tx= simApp.getTransaction();
		getLOGGER().fine("setting up non local future edges "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+tx.getAncestors());
		//Setting up non local future edges
		Component host =simApp.getHostComponent();
		Iterator <OutPort> it =host.f(tx).iterator();
		
		WaitingForRootAckFutureCreate waitingObj = new WaitingForRootAckFutureCreate(callBack);
		waitingObj.addObserver(new Observer(){

			@Override
			public void update(Observable o, Object arg) {
				SimEvent currentEvent = ((WaitingForRootAckFutureCreate)o).getCurrentEvent();
				CallBack callBack= ((WaitingForRootAckFutureCreate)o).getCallBack();
				callBack.callback(currentEvent, null);				
			}
			
		});
		ArrayList<FutureEdge> path= new ArrayList<FutureEdge>();
		path.add(host.getLocalFe(tx.getId()));
		this.WaitingForAckFutureCreateCondition.put(path.toString(), waitingObj);
		while(it.hasNext()){
			OutPort op=it.next();
			FutureEdge fe= new FutureEdge(op,op.getPeerPort(),tx.getId());
			host.addToOES(fe);
			waitingObj.toWait(fe);	
			ArrayList<FutureEdge> pathToSend=new ArrayList<FutureEdge>(path);
			pathToSend.add(fe);
			Object[] content= new Object[2];
			content[0]= "notifyFutureCreate";
			content[1]= pathToSend;
			Object[] params= new Object[1];			
			params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
			currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
		}	
		waitingObj.setCurrentEvent(currentEvent);
		waitingObj.checkAndNotify();
		
	
		getLOGGER().fine("we are waiting for acks of future edge creation from ports: "+ host.f(tx)+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+tx.getAncestors());
	}
	
	/**
	 * Some depending neighbor notifies me that it creates a future edge to me.
	 * @param currentEvent
	 * @param path
	 */
	public void notifyFutureCreate(SimEvent currentEvent, ArrayList<FutureEdge> path){
		getLOGGER().fine("Being notified a non local future edge to me is created. At virtual time: "
				+Engine.getDefault().getVirtualTime() + "; The path: "+path);
		
		
		Component host =this.getSimContainer().getHostComponent();		
		FutureEdge xe= path.get(path.size()-1);
		host.addToIES(xe);
		WaitingForSubAckFutureCreate waitingObj=new WaitingForSubAckFutureCreate(path);	
		waitingObj.addObserver(new Observer(){
			@Override
			public void update(Observable o, Object arg) {
				SimEvent currentEvent = ((WaitingForSubAckFutureCreate)o).getCurrentEvent();
				ArrayList <FutureEdge> path= ((WaitingForSubAckFutureCreate)o).getPath();
				FutureEdge xe= path.get(path.size()-1);
				OutPort op=xe.getFrom();
				Object[] content= new Object[2];
				content[0]= "ackFutureCreate";
				content[1]= path;
				Object[] params= new Object[1];			
				params[0]=new Message("dispatchToAlg",op.getPeerPort(),op,content);				
				currentEvent.notifyNoDelay("onSend", VersionConsistency.this.getSimContainer().getSimNet(), params);				
			}			
		});	
		this.WaitingForAckFutureCreateCondition.put(path.toString(), waitingObj);
		for (OutPort op: host.f()){
			FutureEdge fe= new FutureEdge(op,op.getPeerPort(),xe.getRid());
			host.addToOES(fe);
			waitingObj.toWait(fe);	
			ArrayList<FutureEdge> pathToSend=new ArrayList<FutureEdge>(path);
			pathToSend.add(fe);
			Object[] content= new Object[2];
			content[0]= "notifyFutureCreate";
			content[1]= pathToSend;
			Object[] params= new Object[1];			
			params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
			currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);	
		}
		waitingObj.setCurrentEvent(currentEvent);
		waitingObj.checkAndNotify();
		
		getLOGGER().fine("we are waiting for acks of future edge creation from ports: "+ host.f()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The path: "+path);
	}
	
	public void ackFutureCreate(SimEvent currentEvent, ArrayList<FutureEdge> path){

		getLOGGER().fine("Ack of non local future edge creation received of path: "+ path+
				" At virtual time: "+Engine.getDefault().getVirtualTime() );
		
		assert path.size()>1;
		FutureEdge fe=path.get(path.size()-1);
		path.remove(fe);
		Observable o=this.WaitingForAckFutureCreateCondition.get(path.toString());
		assert o!=null;
		if (path.size()==1){
			WaitingForRootAckFutureCreate waitingObj=(WaitingForRootAckFutureCreate)o;			 
			waitingObj.ackReceived(currentEvent, fe);
		}
		else{
			
			WaitingForSubAckFutureCreate waitingObj=(WaitingForSubAckFutureCreate)o;			 
			waitingObj.ackReceived(currentEvent, fe);
		}
		
	}

	/*
	 * Progressing step
	 */
	private void removeFutureEdges (SimEvent currentEvent, String rid){
		Component host= this.getSimContainer().getHostComponent();
		for (FutureEdge fe: host.getNonLocalFeFromOES(rid)){
			OutPort op=fe.getFrom();
			if(  host.getNonLocalFeFromIES(rid).isEmpty()  && host.getLocalTransactionsWithRootRidAndPortOp(rid, op).isEmpty() ){
				host.removeFromOES(fe);
				Object[] content= new Object[2];
				content[0]= "notifyFutureRemove";
				content[1]=fe;
				Object[] params= new Object[1];			
				params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
				currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
			}			
		}
	}
	
	public void onBeingInitSubTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {	
		//Reconfiguration Starts: no new sub transactions accepted
		if (this.waitingInsteadOfBlocking|| !this.startReconf || FPSet.contains(simApp.getTransaction().getRootId())){
				
			
			getLOGGER().fine("Before callback "+ currentEvent.getId()+
					" At virtual time: "+Engine.getDefault().getVirtualTime() +
					"; The tx: "+simApp.getTransaction().getAncestors());
			
			//Setting Up local edges
			Component host= simApp.getTransaction().getHost();
			FutureEdge lfe= new FutureEdge( host.getLocalOutPort(),host.getLocalInPort(),simApp.getTransaction().getRootId());
			PastEdge lpe= new PastEdge( host.getLocalOutPort(),host.getLocalInPort(),simApp.getTransaction().getRootId());
			host.addToOES(lfe);
			host.addToOES(lpe);
			host.addToIES(lfe);
			host.addToIES(lpe);
			
			Object[] content= new Object[2];
			content[0]= "ackSubTxInit";
			content[1]=simApp.getTransaction().getRootId();
			Object[] params= new Object[1];
			//retrieve the parent ouport from the simApp
			OutPort xop= (OutPort)simApp.getInitiatingMessage().getSource();
			params[0]=new Message("dispatchToAlg",xop.getPeerPort(),xop,content);				
			currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
			
		
			callBack.callback(currentEvent, null);
		}
		else{
			getLOGGER().fine("*** VersionConsistency algorithm blocks transaction: "+ simApp.getTransaction() + " at VT: " 
					+ Engine.getDefault().getVirtualTime());
		}
	}
	public void ackSubTxInit(SimEvent currentEvent, String rid){
		this.removeFutureEdges(currentEvent, rid);
	}
	
	public void notifyFutureRemove(SimEvent currentEvent, FutureEdge fe ){
		String rid= fe.getRid();
		Component host= this.getSimContainer().getHostComponent();
		host.removeFromIES(fe);
		this.removeFutureEdges(currentEvent, rid);
		if(this.startReconf ){
			checkFreeness(currentEvent);
		}
	}	
	
	/*public void onEndingSubTx(SimEvent currentEvent, SimAppTx simApp,
			CallBack callBack) {
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+simApp.getTransaction().getAncestors());
		
		//retrieve the parent ouport from the simApp
		OutPort xop= (OutPort)simApp.getInitiatingMessage().getSource();
		//this part can be optimized
		Object[] content= new Object[2];
		content[0]= "onBeingEndingTx";
		content[1]=simApp.getTransaction().getAncestors();
		
		Object[] params= new Object[1];

		params[0]=new Message("dispatchToAlg",xop.getPeerPort(),xop,content);				
		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
		callBack.callback(currentEvent, null);
		
	}
*/
	public void onBeingEndingTx(SimEvent currentEvent,SimAppTx simApp,OutPort op, CallBack callBack) {
		PastEdge pe = new PastEdge(op,op.getPeerPort(),simApp.getTransaction().getRootId());
		this.getSimContainer().getHostComponent().addToOES(pe);
		Object[] content= new Object[2];
		content[0]= "notifyPastCreate";
		content[1]=pe;
		
		Object[] params= new Object[1];

		params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);	

		
		callBack.callback(currentEvent, null);
	}
	
	/*public void notifySubTxEnd(SimEvent currentEvent,OutPort op,String rid){
		PastEdge pe = new PastEdge(op,op.getPeerPort(),rid);
		this.getSimContainer().getHostComponent().addToOES(pe);
		Object[] content= new Object[2];
		content[0]= "notifyPastCreate";
		content[1]=pe;
		
		Object[] params= new Object[1];

		params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
		
	}*/
	
	public void notifyPastCreate(SimEvent currentEvent, PastEdge pe){
		this.getSimContainer().getHostComponent().addToIES(pe);
		Component host= this.getSimContainer().getHostComponent();
		if (host.getLocalTransactionsWithRootRid(pe.getRid()).isEmpty()){
			FutureEdge lfe=host.getLocalFe(pe.getRid());
			PastEdge lpe =host.getLocalPe(pe.getRid());
			host.removeFromOES(lfe);
			host.removeFromOES(lpe);
			host.removeFromIES(lfe);
			host.removeFromIES(lpe);
		}

		this.removeFutureEdges(currentEvent, pe.getRid());
		if (this.startReconf){
			checkFreeness(currentEvent);
		}
	}
	
	/*
	 * Cleaning-up step
	 */
	
	private void removeAllEdges(SimEvent currentEvent,String rid){
		Component host= this.getSimContainer().getHostComponent();
		HashSet<DynamicEdge> tempOES=new HashSet<DynamicEdge>(host.getOES());
		for(DynamicEdge edge:tempOES){
			if (edge.getRid().equals(rid)){
			host.removeFromOES(edge);
				if ((!(edge.getTo().equals(host.getLocalInPort())))  &&(edge instanceof FutureEdge)){
					Object[] content= new Object[2];
					content[0]= "notifyFutureRemove";
					content[1]=edge;
					Object[] params= new Object[1];		
					OutPort op=edge.getFrom();
					params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
					currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
				}
				
				else if ((!(edge.getTo().equals(host.getLocalInPort())))  &&(edge instanceof PastEdge)){
					Object[] content= new Object[2];
					content[0]= "notifyPastRemove";
					content[1]=edge;
					Object[] params= new Object[1];	
					OutPort op=edge.getFrom();
					params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
					currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
				}
			}
		}		
	}
	public void onEndingRootTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+simApp.getTransaction().getAncestors());
		
		Component host= this.getSimContainer().getHostComponent();
		String rid=simApp.getTransaction().getId();
		FutureEdge lfe =host.getLocalFe(rid);
		PastEdge   lpe =host.getLocalPe(rid);
		host.removeFromOES(lfe);
		host.removeFromOES(lpe);		
		host.removeFromIES(lfe);
		host.removeFromIES(lpe);
		this.removeAllEdges(currentEvent, rid);
		

		callBack.callback(currentEvent, null);
		
		if (this.startReconf){
			checkFreeness(currentEvent);
		}
	}
	
	public void notifyPastRemove(SimEvent currentEvent, PastEdge pe ){
		String rid= pe.getRid();
		Component host= this.getSimContainer().getHostComponent();
		host.removeFromIES(pe);
		this.removeAllEdges(currentEvent, rid);
		if (this.startReconf){
			checkFreeness(currentEvent);
		}
	}	
	
	public void checkFreeness(SimEvent currentEvent){
		
		assert this.startReconf;
		//populating the FSet and the PSet
		HashSet <String> FSet= new HashSet<String>();
		HashSet <String> PSet= new HashSet<String>();
		Component hostComponent = this.getSimContainer().getHostComponent();
		HashSet <DynamicEdge> IES =hostComponent.getIES();
		
		for (DynamicEdge edge :IES){	
			if (edge instanceof FutureEdge){
				
				// we inserted to FSet its RID
				FSet.add(edge.getRid());
			}
			else if(edge instanceof PastEdge){
				
				// we inserted to PSet its RID
				PSet.add(edge.getRid());
			}					
		}

		//Create the intersection between FSet and PSet
		FPSet.clear();
		for (String ridInFSet :FSet){
			if (PSet.contains(ridInFSet)){
				FPSet.add(ridInFSet);
			}
		}
		//Check
		if (FPSet.isEmpty()){
			LOGGER.info("*** Freeness achieved for component "
					+ this.getSimContainer().getHostComponent().getId()
					+ " at VT: "+Engine.getDefault().getVirtualTime()
					+"! ***");
			this.collectResultCallBack.callback(currentEvent, null);
			

			//debugging purpose
			//System.exit(0);
		}

		getLOGGER().info(
//				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
//				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
				" \n *************** current FPSet : " + FPSet
				+ " \nvirtual time "+Engine.getDefault().getVirtualTime());
			
		
		
	}
	//

	//inner classes
	private class WaitingForRootAckFutureCreate extends Observable{		
		private HashSet <FutureEdge>fEdgesToAck;
		private SimEvent currentEvent;
		private CallBack callBack;
		/**
		 * @param callBack 
		 * @param id
		 * @param f
		 */
		public WaitingForRootAckFutureCreate(CallBack callBack) {
			this.callBack=callBack;
			fEdgesToAck=new HashSet <FutureEdge>();
			
		}
		public void toWait(FutureEdge fe){
			this.fEdgesToAck.add(fe);
		}
		public boolean conditionCheck(){
			
			return fEdgesToAck.isEmpty();
		}

		public void ackReceived(SimEvent currentEvent,FutureEdge fe){
			this.currentEvent=currentEvent;
			this.fEdgesToAck.remove(fe);
			this.setChanged();			
			if (conditionCheck()) this.notifyObservers();			
		}
		public void checkAndNotify(){
			this.setChanged();			
			if (conditionCheck()) this.notifyObservers();		
		}
		/**
		 * @param currentEvent the currentEvent to set
		 */
		public void setCurrentEvent(SimEvent currentEvent) {
			this.currentEvent = currentEvent;
		}
		/**
		 * @return the currentEvent
		 */
		public SimEvent getCurrentEvent() {
			return currentEvent;
		}
		/**
		 * @return the callBack
		 */
		public CallBack getCallBack() {
			return callBack;
		}
		
		
	}
	
	private class WaitingForSubAckFutureCreate extends Observable{
		private HashSet <FutureEdge>fEdgesToAck;
		private SimEvent currentEvent;
		private ArrayList<FutureEdge>path;
		
		public WaitingForSubAckFutureCreate(ArrayList<FutureEdge>path){
			fEdgesToAck= new HashSet <FutureEdge>();
			this.path=path;
		}
		public void toWait(FutureEdge fe){
			this.fEdgesToAck.add(fe);
		}
		private boolean conditionCheck(){
			
			return fEdgesToAck.isEmpty();
		}
		public void ackReceived(SimEvent currentEvent,FutureEdge fe){
			this.currentEvent=currentEvent;
			this.fEdgesToAck.remove(fe);
			this.setChanged();			
			if (conditionCheck()) this.notifyObservers();			
		}
		public void checkAndNotify(){
			this.setChanged();			
			if (conditionCheck()) this.notifyObservers();		
		}

		
		/**
		 * @param currentEvent the currentEvent to set
		 */
		public void setCurrentEvent(SimEvent currentEvent) {
			this.currentEvent = currentEvent;
		}
		/**
		 * @return the currentEvent
		 */
		public SimEvent getCurrentEvent() {
			return currentEvent;
		}
		/**
		 * @return the path
		 */
		public ArrayList<FutureEdge> getPath() {
			return path;
		}	
		
		
	}



}
