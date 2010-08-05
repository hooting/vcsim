package it.polimi.vcdu.alg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;


import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Configuration;
import it.polimi.vcdu.model.DynamicEdge;
import it.polimi.vcdu.model.FutureEdge;
import it.polimi.vcdu.model.InPort;
import it.polimi.vcdu.model.Message;
import it.polimi.vcdu.model.OutPort;
import it.polimi.vcdu.model.PastEdge;
import it.polimi.vcdu.model.StaticEdge;
import it.polimi.vcdu.model.Transaction;
import it.polimi.vcdu.sim.CallBack;
import it.polimi.vcdu.sim.SimAppTx;
import it.polimi.vcdu.sim.SimContainer;
import it.polimi.vcdu.sim.SimEvent;
import it.unipr.ce.dsg.deus.core.Engine;

public class VCOnDemand extends Algorithm {

	private final static Logger LOGGER = Logger.getLogger(VCOnDemand.class.getName());
	@Override
	protected Logger getLOGGER() {		
		return LOGGER;
	}
	
	/**
	 * @author Xiaoxing Ma
	 *
	 */
	
	// when DEFAULT, using the DefaultAlg; do not manage dynamic dependencies. when onBeingRequestOnDemand received, switch to ONDEMAND
	// when VC, using the VersionConsistency algorithm, manage dynamic dependencies, but with consideration of the scope. 
	// when ONDEMAND blocking any initialization and ending of sub transactions (and remember them) until VC, works like VC;
	// setting up for all on-going transactions, when setting-up is done, notify all out-going nodes (within scope) ;
	// and when all incoming components have notified me their finish of local setting-up, switch to VC and resume 
	// all blocked events (initialization and ending of transactions).
	// When receiving a notification of finishing-local-setting up from an incoming node, forward to all out going node, so that
	// the target node can learn that all nodes in the scope is ready, and can pursuing freeness.
	
	private enum DDMngMode{DEFAULT, ONDEMAND, VC}; 
	private DDMngMode dDMngMode = DDMngMode.DEFAULT;
	private boolean localSettingUpDone = false;
	
	private VersionConsistency vCAlgorithm; //redirect to vCAlgorithm when appropriate 
	
	private float vCOndemandReqTime = -1.0f;
	
	private ArrayList<Component> vCScope;
	private ArrayList<OutPort> requestOnDemandToWait;
	
	private ArrayList<OutPort> inScopeOutPorts = new ArrayList<OutPort>();
	private ArrayList<InPort> inScopeInPorts = new ArrayList<InPort>();
	
	private ArrayList<SimEvent> blockedEventsDueToOnDemandSettingUp = new ArrayList<SimEvent>();
	
	// OnDemandWaitingForAckEdgeCreateCondition repository also includes those waiting condidtion objects for 
	// normal setting up of f edges for new root txs.
	// So we need to remember those root tx ids that set up on demand, so that we can know when on demand setting up is ready
	private HashMap<String,Observable> waitingForEdgeCreateConditionObjs= new HashMap<String, Observable>();
	private HashSet<String> onDemandPaths = new HashSet<String>();
	
	
	public VCOnDemand(SimContainer simCon) {
		super(simCon);
		vCAlgorithm = new VersionConsistency(simCon);
		dDMngMode  = DDMngMode.DEFAULT;
	}
	
	
	public void onBeingRequestOnDemand(SimEvent currentEvent){
		vCOndemandReqTime = Engine.getDefault().getVirtualTime();
		LOGGER.info("*** Request received to achieve freeness. Now setting up dynamic dependences from"
				+ getSimContainer().getHostComponent().getId()
				+ " at VT: "+vCOndemandReqTime +" ***");
		this.collectReqSettingCallBack.callback(currentEvent, null);
		Component hostComponent = this.getSimContainer().getHostComponent();
		ArrayList<Component> scope = computeAffectedScope(hostComponent, hostComponent.getConf());
		
		this.onBeingRequestOnDemand(currentEvent,scope,null);
	}
	
	public void onBeingRequestOnDemand(SimEvent currentEvent, ArrayList<Component> scope, StaticEdge ose){
		LOGGER.info("*** Request received to achieve freeness. Now setting up dynamic dependences from edge "
				+ ose
				+ " at VT: "+vCOndemandReqTime +" ***");
		if(this.vCScope == null){ // this is the first request
			this.vCScope = scope;
			
			//compute inScopeOutPorts and inScopeInPorts;
			Component host = this.getSimContainer().getHostComponent();
			for(OutPort op: host.getOutPorts()){
				if(this.vCScope.contains(op.getPeerPort().getHost())){
					this.inScopeOutPorts.add(op);
				}
			}
			for(InPort ip: host.getInPorts()){
				assert this.vCScope.contains(ip.getPeerPort().getHost()); // currently, all in-ports should be in
				this.inScopeInPorts.add(ip);
			}
			//we need to wait for all request from outgoing components in scope, setup the waitng
			this.requestOnDemandToWait = new ArrayList<OutPort>(inScopeOutPorts);
		};
		
		if (ose==null){ //from myself
			assert requestOnDemandToWait.isEmpty();
		}else{
			OutPort op = ose.getFrom();
			assert requestOnDemandToWait.contains(op);
			requestOnDemandToWait.remove(op);
		}
		
		//I am ready to switch to ONDEMAND status
		if(this.requestOnDemandToWait.isEmpty()) switchToONDEMAND(currentEvent);
	}

		
	private void switchToONDEMAND(SimEvent currentEvent){
		LOGGER.info("*** Component: "+ this.getSimContainer().getHostComponent().getId() 
				+ "swith to ONDEMAND at VT: "+vCOndemandReqTime +" ***");
		
		this.dDMngMode = DDMngMode.ONDEMAND;
		
		vCOndemandReqTime = Engine.getDefault().getVirtualTime();
		
		// ask all up stream components to vc ondemand
		for (InPort ip: this.inScopeInPorts){
			//create a message to call the onBeingRequestOnDemand of the component 
			this.reqOnDemand(currentEvent, ip);
		}
		
		// setting up edges for all ongoing transactions
		for (Transaction tx : this.getSimContainer().getHostComponent().getLocalTransactions()){
			CallBack callback = new CallBack(getSimContainer()){
				@Override
				public void callback(SimEvent currentEvent, Object[] parameters) {
					checkAndNotifyFinishSetupOD();
				}
				
			};
			setupDynamicEdgesOnDemand(currentEvent, tx,callback);
		}
		checkAndNotifyFinishSetupOD();
	}
	
	
	private void reqOnDemand(SimEvent currentEvent, InPort ip) {
		Object[] content= new Object[3];
		content[0]= "onBeingRequestOnDemand";
		content[1]= this.vCScope;
		content[2]= ip.getIncidentEdge();
		Object[] params= new Object[1];			
		params[0]=new Message("dispatchToAlg",ip,ip.getPeerPort(),content);				
		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);	
		LOGGER.info("*** Request depending node to set up on demand, along edge "+ip.getIncidentEdge()
				+" at VT: "+Engine.getDefault().getVirtualTime()
				+" ***");
	}

	private void setupDynamicEdgesOnDemand(SimEvent currentEvent, Transaction tx, CallBack callback){
		// is a trasnaction is running locally, then create pastedge from 1 to currentstep -1
		// future edges from currentstep to size -2, otherwise, since the msg can be on road,
		//  let's be conservative, to create past edges from 1 to currentstep
		// and create future edges from current step to size-2
		// then there is no need to worry about non-root transactions. 
		if (tx.isRoot()){
			
			Component host= tx.getHost();
			FutureEdge lfe= new FutureEdge( host.getLocalOutPort(),host.getLocalInPort(),tx.getId());
			PastEdge lpe= new PastEdge( host.getLocalOutPort(),host.getLocalInPort(),tx.getId());
			host.addToOES(lfe);
			host.addToOES(lpe);
			host.addToIES(lfe);
			host.addToIES(lpe);
			
			//Setting up non local future edges. first decide the corresponding outports.
			ArrayList<OutPort> futures = new ArrayList<OutPort>();
			for(int i=tx.getIteration().getCurrent_step();i<tx.getIteration().getList().size()-1; i++) {
				OutPort outPort = tx.getIteration().getList().get(i).getOutPort();
				if(this.inScopeOutPorts.contains(outPort)){
					futures.add(outPort);
				}
			};
			ArrayList<OutPort> pasts = new ArrayList<OutPort>();
			for(int i=1;i<tx.getIteration().getCurrent_step();i++) {
				OutPort outPort = tx.getIteration().getList().get(i).getOutPort();
				if(this.inScopeOutPorts.contains(outPort)){
					pasts.add(outPort);
				}
			};
			if(! tx.getIteration().isWorking()) {
				OutPort outPort = tx.getIteration().getCurrentStep().getOutPort();
				if(this.inScopeOutPorts.contains(outPort)){
					pasts.add(outPort);
				}
			}

			//create future edges and wait for acks
			WaitingForRootAckFutureCreate waitingObj = new WaitingForRootAckFutureCreate(callback);
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
			this.waitingForEdgeCreateConditionObjs.put(path.toString(), waitingObj);
			this.onDemandPaths.add(path.toString());
			for(OutPort op: futures){
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
			
		
			getLOGGER().fine("we are waiting for acks of future edge creation on demand from ports: "+ futures+
					" At virtual time: "+Engine.getDefault().getVirtualTime() +
					"; The tx: "+tx.getAncestors());
			
			//create past edges and wait for acks
			WaitingForRootAckPastCreateOD waitingObjPast = new WaitingForRootAckPastCreateOD(callback);
			waitingObjPast.addObserver(new Observer(){
				@Override
				public void update(Observable o, Object arg) {
					SimEvent currentEvent = ((WaitingForRootAckPastCreateOD)o).getCurrentEvent();
					CallBack callBack= ((WaitingForRootAckPastCreateOD)o).getCallBack();
					callBack.callback(currentEvent, null);				
				}
			});
			ArrayList<PastEdge> ppath= new ArrayList<PastEdge>();
			ppath.add(host.getLocalPe(tx.getId()));
			this.waitingForEdgeCreateConditionObjs.put(ppath.toString(), waitingObjPast);
			this.onDemandPaths.add(ppath.toString());
			for(OutPort op: pasts){
				PastEdge pe= new PastEdge(op,op.getPeerPort(),tx.getId());
				host.addToOES(pe);
				waitingObjPast.toWait(pe);	
				ArrayList<PastEdge> ppathToSend=new ArrayList<PastEdge>(ppath);
				ppathToSend.add(pe);
				Object[] content= new Object[2];
				content[0]= "notifyPastCreateOD";
				content[1]= ppathToSend;
				Object[] params= new Object[1];			
				params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
				currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
			}	
			waitingObjPast.setCurrentEvent(currentEvent);
			waitingObjPast.checkAndNotify();
			
		
			getLOGGER().fine("we are waiting for acks of past edge creation on demand from ports: "+ pasts+
					" At virtual time: "+Engine.getDefault().getVirtualTime() +
					"; The tx: "+tx.getAncestors());
		}
	}
	
	private void checkAndNotifyFinishSetupOD(){
		assert false : "Fix me!";
	}
	
	
	
	
	



	/**
	 * Utility functions used in this class.
	 */
	
	public static ArrayList<Component> computeAffectedScope(Component com, Configuration conf){
		ArrayList<Component> resultScope = new ArrayList<Component>();
		ArrayList<Component> allComponents = new ArrayList<Component>(conf.getComponents());
		boolean hasMore = false;
		resultScope.add(com);
		ArrayList<Component> lastAdded = new ArrayList<Component>();
		lastAdded.add(com);
		do{
			hasMore = false;
			ArrayList<Component> tmpToAdd = new ArrayList<Component>();
			for(Component cc: lastAdded){
				for(InPort ip : cc.getInPorts()){
					Component predCom = ip.getPeerPort().getHost();
					if (!resultScope.contains(predCom)){
						tmpToAdd.add(predCom);
						hasMore = true;
					}
				}
			}
			resultScope.addAll(tmpToAdd);
			lastAdded = tmpToAdd;
		}while(hasMore);
		
		return resultScope;
	}
	
//	private static boolean isDependingOnRecursive(Component depending, Component depended){
//		boolean result = false;
//		if (depending == depended) return true;
//		for(OutPort op:depending.getOutPorts()){
//			if (isDependingOnop.getPeerPort().getHost()==depended) return true;
//		}
//		
//		return result;
//	}
	public static boolean isDependingOnDirect(Component depending, Component depended){
		boolean result = false;
		if (depending == depended) return true;
 		for(OutPort op:depending.getOutPorts()){
			if (op.getPeerPort().getHost()==depended) return true;
		}
		return result;
	}
//	
//	
//	
//	/**
//	 * Following is copied from VersionConsistency
//	 */
//	
//	private HashMap<String,Observable> WaitingForAckFutureCreateCondition= new HashMap<String, Observable>();
//
//
//	private float VersConsistencyReqTime = -1.0f;
//	private boolean startReconf=false;
//	private HashSet<String> FPSet = new HashSet<String>();
//	private boolean waitingInsteadOfBlocking=false;
//
//
//	
//	public void startReconf(SimEvent currentEvent){
//		getLOGGER().info("************** STARTING RECONFIGURATION OF COMPONENT "+this.getSimContainer().getHostComponent().getId()+" ***************\n " +
//				"virtual time "+Engine.getDefault().getVirtualTime());
//		this.VersConsistencyReqTime=Engine.getDefault().getVirtualTime();
//		this.startReconf=true;
//		this.collectReqSettingCallBack.callback(currentEvent, null);
//		getLOGGER().info(
////				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
////				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
//				" \n *************** current FPSet : " + FPSet);
//		
//		checkFreeness(currentEvent);
//	}
//	public void startReconfWaiting(SimEvent currentEvent){
//		getLOGGER().info("************** STARTING RECONFIGURATION OF COMPONENT "+this.getSimContainer().getHostComponent().getId()+" ***************\n " +
//				"virtual time "+Engine.getDefault().getVirtualTime());
//		this.VersConsistencyReqTime=Engine.getDefault().getVirtualTime();
//		this.startReconf=true;
//		this.waitingInsteadOfBlocking=true;
//		this.collectReqSettingCallBack.callback(currentEvent, null);
//		getLOGGER().info(
////				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
////				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
//				" \n *************** current FPSet : " + FPSet);
//		
//		checkFreeness(currentEvent);
//	}
//	
//	@Override
//	public void onInitRootTx(SimEvent currentEvent, SimAppTx simApp,CallBack callBack) {
//	
//		if (this.waitingInsteadOfBlocking|| !this.startReconf || FPSet.contains(simApp.getTransaction().getRootId())){
//			Transaction tx= simApp.getTransaction();
//			//Setting Up local edges
//			Component host= simApp.getHostComponent();
//			FutureEdge lfe= new FutureEdge( host.getLocalOutPort(),host.getLocalInPort(),tx.getId());
//			PastEdge lpe= new PastEdge( host.getLocalOutPort(),host.getLocalInPort(),tx.getId());
//			host.addToOES(lfe);
//			host.addToOES(lpe);
//			host.addToIES(lfe);
//			host.addToIES(lpe);
//			
//			getLOGGER().fine("Before callback "+ currentEvent.getId()+
//					" At virtual time: "+Engine.getDefault().getVirtualTime() +
//					"; The root tx: "+tx.getAncestors());		
//			callBack.callback(currentEvent, null);
//		}
//		else{
//			getLOGGER().fine("*** VersionConsistency algorithm blocks transaction: "+ simApp.getTransaction() + " at VT: " 
//					+ Engine.getDefault().getVirtualTime());
//		}
//	}
//	@Override
//	public void onToGoBeforeRootInitFirstSubTx(SimEvent currentEvent,  SimAppTx simApp,CallBack callBack) {
//		Transaction tx= simApp.getTransaction();
//		getLOGGER().fine("setting up non local future edges "+ currentEvent.getId()+
//				" At virtual time: "+Engine.getDefault().getVirtualTime() +
//				"; The tx: "+tx.getAncestors());
//		//Setting up non local future edges
//		Component host =simApp.getHostComponent();
//		Iterator <OutPort> it =host.f(tx).iterator();
//		
//		WaitingForRootAckFutureCreate waitingObj = new WaitingForRootAckFutureCreate(callBack);
//		waitingObj.addObserver(new Observer(){
//
//			@Override
//			public void update(Observable o, Object arg) {
//				SimEvent currentEvent = ((WaitingForRootAckFutureCreate)o).getCurrentEvent();
//				CallBack callBack= ((WaitingForRootAckFutureCreate)o).getCallBack();
//				callBack.callback(currentEvent, null);				
//			}
//			
//		});
//		ArrayList<FutureEdge> path= new ArrayList<FutureEdge>();
//		path.add(host.getLocalFe(tx.getId()));
//		this.WaitingForAckFutureCreateCondition.put(path.toString(), waitingObj);
//		while(it.hasNext()){
//			OutPort op=it.next();
//			FutureEdge fe= new FutureEdge(op,op.getPeerPort(),tx.getId());
//			host.addToOES(fe);
//			waitingObj.toWait(fe);	
//			ArrayList<FutureEdge> pathToSend=new ArrayList<FutureEdge>(path);
//			pathToSend.add(fe);
//			Object[] content= new Object[2];
//			content[0]= "notifyFutureCreate";
//			content[1]= pathToSend;
//			Object[] params= new Object[1];			
//			params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
//			currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
//		}	
//		waitingObj.setCurrentEvent(currentEvent);
//		waitingObj.checkAndNotify();
//		
//	
//		getLOGGER().fine("we are waiting for acks of future edge creation from ports: "+ host.f(tx)+
//				" At virtual time: "+Engine.getDefault().getVirtualTime() +
//				"; The tx: "+tx.getAncestors());
//	}
//	
	/**
	 * Some depending neighbor notifies me that it creates a future edge to me.
	 * @param currentEvent
	 * @param path
	 */
	public void notifyFutureCreate(SimEvent currentEvent, ArrayList<FutureEdge> path){
		getLOGGER().fine("Being notified a non local future edge to me is created. At virtual time: "
				+Engine.getDefault().getVirtualTime() + "; The path: "+path);
		assert !this.vCScope.isEmpty();
		
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
				currentEvent.notifyNoDelay("onSend", VCOnDemand.this.getSimContainer().getSimNet(), params);				
			}			
		});	
		this.waitingForEdgeCreateConditionObjs.put(path.toString(), waitingObj);
		for (OutPort op: host.f()){
			Component peerhost = op.getPeerPort().getHost();
			if(this.vCScope.contains(peerhost)){
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
		Observable o=this.waitingForEdgeCreateConditionObjs.get(path.toString());
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
	
	
	/**
	 * Some depending neighbor notifies me that it creates a past edge on demand to me.
	 * @param currentEvent
	 * @param path
	 */
	public void notifyPastCreateOD(SimEvent currentEvent, ArrayList<PastEdge> path){
		getLOGGER().fine("Being notified a non local past edge to me is created on demand. At virtual time: "
				+Engine.getDefault().getVirtualTime() + "; The path: "+path);
		assert !this.vCScope.isEmpty();
		
		Component host =this.getSimContainer().getHostComponent();		
		PastEdge xe= path.get(path.size()-1);
		host.addToIES(xe);
		WaitingForSubAckPastCreateOD waitingObj=new WaitingForSubAckPastCreateOD(path);	
		waitingObj.addObserver(new Observer(){
			@Override
			public void update(Observable o, Object arg) {
				SimEvent currentEvent = ((WaitingForSubAckPastCreateOD)o).getCurrentEvent();
				ArrayList <PastEdge> path= ((WaitingForSubAckPastCreateOD)o).getPath();
				PastEdge xe= path.get(path.size()-1);
				OutPort op=xe.getFrom();
				Object[] content= new Object[2];
				content[0]= "ackPastCreateDD";
				content[1]= path;
				Object[] params= new Object[1];			
				params[0]=new Message("dispatchToAlg",op.getPeerPort(),op,content);				
				currentEvent.notifyNoDelay("onSend", VCOnDemand.this.getSimContainer().getSimNet(), params);				
			}			
		});	
		this.waitingForEdgeCreateConditionObjs.put(path.toString(), waitingObj);
		for (OutPort op: host.f()){
			Component peerhost = op.getPeerPort().getHost();
			if(this.vCScope.contains(peerhost)){
				PastEdge pe= new PastEdge(op,op.getPeerPort(),xe.getRid());
				host.addToOES(pe);
				waitingObj.toWait(pe);	
				ArrayList<PastEdge> pathToSend=new ArrayList<PastEdge>(path);
				pathToSend.add(pe);
				Object[] content= new Object[2];
				content[0]= "notifyPastCreateOD";
				content[1]= pathToSend;
				Object[] params= new Object[1];			
				params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
				currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
			}
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
		Observable o=this.waitingForEdgeCreateConditionObjs.get(path.toString());
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
		
	
	
//
//	/*
//	 * Progressing step
//	 */
//	private void removeFutureEdges (SimEvent currentEvent, String rid){
//		Component host= this.getSimContainer().getHostComponent();
//		for (FutureEdge fe: host.getNonLocalFeFromOES(rid)){
//			OutPort op=fe.getFrom();
//			if(  host.getNonLocalFeFromIES(rid).isEmpty()  && host.getLocalTransactionsWithRootRidAndPortOp(rid, op).isEmpty() ){
//				host.removeFromOES(fe);
//				Object[] content= new Object[2];
//				content[0]= "notifyFutureRemove";
//				content[1]=fe;
//				Object[] params= new Object[1];			
//				params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
//				currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
//			}			
//		}
//	}
//	
//	public void onBeingInitSubTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {	
//		//Reconfiguration Starts: no new sub transactions accepted
//		if (this.waitingInsteadOfBlocking|| !this.startReconf || FPSet.contains(simApp.getTransaction().getRootId())){
//				
//			
//			getLOGGER().fine("Before callback "+ currentEvent.getId()+
//					" At virtual time: "+Engine.getDefault().getVirtualTime() +
//					"; The tx: "+simApp.getTransaction().getAncestors());
//			
//			//Setting Up local edges
//			Component host= simApp.getTransaction().getHost();
//			FutureEdge lfe= new FutureEdge( host.getLocalOutPort(),host.getLocalInPort(),simApp.getTransaction().getRootId());
//			PastEdge lpe= new PastEdge( host.getLocalOutPort(),host.getLocalInPort(),simApp.getTransaction().getRootId());
//			host.addToOES(lfe);
//			host.addToOES(lpe);
//			host.addToIES(lfe);
//			host.addToIES(lpe);
//			
//			Object[] content= new Object[2];
//			content[0]= "ackSubTxInit";
//			content[1]=simApp.getTransaction().getRootId();
//			Object[] params= new Object[1];
//			//retrieve the parent ouport from the simApp
//			OutPort xop= (OutPort)simApp.getInitiatingMessage().getSource();
//			params[0]=new Message("dispatchToAlg",xop.getPeerPort(),xop,content);				
//			currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
//			
//		
//			callBack.callback(currentEvent, null);
//		}
//		else{
//			getLOGGER().fine("*** VersionConsistency algorithm blocks transaction: "+ simApp.getTransaction() + " at VT: " 
//					+ Engine.getDefault().getVirtualTime());
//		}
//	}
//	public void ackSubTxInit(SimEvent currentEvent, String rid){
//		this.removeFutureEdges(currentEvent, rid);
//	}
//	
//	public void notifyFutureRemove(SimEvent currentEvent, FutureEdge fe ){
//		String rid= fe.getRid();
//		Component host= this.getSimContainer().getHostComponent();
//		host.removeFromIES(fe);
//		this.removeFutureEdges(currentEvent, rid);
//		if(this.startReconf ){
//			checkFreeness(currentEvent);
//		}
//	}	
//	
//	/*public void onEndingSubTx(SimEvent currentEvent, SimAppTx simApp,
//			CallBack callBack) {
//		getLOGGER().fine("Before callback "+ currentEvent.getId()+
//				" At virtual time: "+Engine.getDefault().getVirtualTime() +
//				"; The tx: "+simApp.getTransaction().getAncestors());
//		
//		//retrieve the parent ouport from the simApp
//		OutPort xop= (OutPort)simApp.getInitiatingMessage().getSource();
//		//this part can be optimized
//		Object[] content= new Object[2];
//		content[0]= "onBeingEndingTx";
//		content[1]=simApp.getTransaction().getAncestors();
//		
//		Object[] params= new Object[1];
//
//		params[0]=new Message("dispatchToAlg",xop.getPeerPort(),xop,content);				
//		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
//		callBack.callback(currentEvent, null);
//		
//	}
//*/
//	public void onBeingEndingTx(SimEvent currentEvent,SimAppTx simApp,OutPort op, CallBack callBack) {
//		PastEdge pe = new PastEdge(op,op.getPeerPort(),simApp.getTransaction().getRootId());
//		this.getSimContainer().getHostComponent().addToOES(pe);
//		Object[] content= new Object[2];
//		content[0]= "notifyPastCreate";
//		content[1]=pe;
//		
//		Object[] params= new Object[1];
//
//		params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
//		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);	
//
//		
//		callBack.callback(currentEvent, null);
//	}
//	
//	/*public void notifySubTxEnd(SimEvent currentEvent,OutPort op,String rid){
//		PastEdge pe = new PastEdge(op,op.getPeerPort(),rid);
//		this.getSimContainer().getHostComponent().addToOES(pe);
//		Object[] content= new Object[2];
//		content[0]= "notifyPastCreate";
//		content[1]=pe;
//		
//		Object[] params= new Object[1];
//
//		params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
//		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
//		
//	}*/
//	
//	public void notifyPastCreate(SimEvent currentEvent, PastEdge pe){
//		this.getSimContainer().getHostComponent().addToIES(pe);
//		Component host= this.getSimContainer().getHostComponent();
//		if (host.getLocalTransactionsWithRootRid(pe.getRid()).isEmpty()){
//			FutureEdge lfe=host.getLocalFe(pe.getRid());
//			PastEdge lpe =host.getLocalPe(pe.getRid());
//			host.removeFromOES(lfe);
//			host.removeFromOES(lpe);
//			host.removeFromIES(lfe);
//			host.removeFromIES(lpe);
//		}
//
//		this.removeFutureEdges(currentEvent, pe.getRid());
//		if (this.startReconf){
//			checkFreeness(currentEvent);
//		}
//	}
//	
//	/*
//	 * Cleaning-up step
//	 */
//	
//	private void removeAllEdges(SimEvent currentEvent,String rid){
//		Component host= this.getSimContainer().getHostComponent();
//		HashSet<DynamicEdge> tempOES=new HashSet<DynamicEdge>(host.getOES());
//		for(DynamicEdge edge:tempOES){
//			if (edge.getRid().equals(rid)){
//			host.removeFromOES(edge);
//				if ((!(edge.getTo().equals(host.getLocalInPort())))  &&(edge instanceof FutureEdge)){
//					Object[] content= new Object[2];
//					content[0]= "notifyFutureRemove";
//					content[1]=edge;
//					Object[] params= new Object[1];		
//					OutPort op=edge.getFrom();
//					params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
//					currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
//				}
//				
//				else if ((!(edge.getTo().equals(host.getLocalInPort())))  &&(edge instanceof PastEdge)){
//					Object[] content= new Object[2];
//					content[0]= "notifyPastRemove";
//					content[1]=edge;
//					Object[] params= new Object[1];	
//					OutPort op=edge.getFrom();
//					params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
//					currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
//				}
//			}
//		}		
//	}
//	public void onEndingRootTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
//		getLOGGER().fine("Before callback "+ currentEvent.getId()+
//				" At virtual time: "+Engine.getDefault().getVirtualTime() +
//				"; The tx: "+simApp.getTransaction().getAncestors());
//		
//		Component host= this.getSimContainer().getHostComponent();
//		String rid=simApp.getTransaction().getId();
//		FutureEdge lfe =host.getLocalFe(rid);
//		PastEdge   lpe =host.getLocalPe(rid);
//		host.removeFromOES(lfe);
//		host.removeFromOES(lpe);		
//		host.removeFromIES(lfe);
//		host.removeFromIES(lpe);
//		this.removeAllEdges(currentEvent, rid);
//		
//
//		callBack.callback(currentEvent, null);
//		
//		if (this.startReconf){
//			checkFreeness(currentEvent);
//		}
//	}
//	
//	public void notifyPastRemove(SimEvent currentEvent, PastEdge pe ){
//		String rid= pe.getRid();
//		Component host= this.getSimContainer().getHostComponent();
//		host.removeFromIES(pe);
//		this.removeAllEdges(currentEvent, rid);
//		if (this.startReconf){
//			checkFreeness(currentEvent);
//		}
//	}	
//	
//	public void checkFreeness(SimEvent currentEvent){
//		
//		assert this.startReconf;
//		//populating the FSet and the PSet
//		HashSet <String> FSet= new HashSet<String>();
//		HashSet <String> PSet= new HashSet<String>();
//		Component hostComponent = this.getSimContainer().getHostComponent();
//		HashSet <DynamicEdge> IES =hostComponent.getIES();
//		
//		for (DynamicEdge edge :IES){	
//			if (edge instanceof FutureEdge){
//				
//				// we inserted to FSet its RID
//				FSet.add(edge.getRid());
//			}
//			else if(edge instanceof PastEdge){
//				
//				// we inserted to PSet its RID
//				PSet.add(edge.getRid());
//			}					
//		}
//
//		//Create the intersection between FSet and PSet
//		FPSet.clear();
//		for (String ridInFSet :FSet){
//			if (PSet.contains(ridInFSet)){
//				FPSet.add(ridInFSet);
//			}
//		}
//		//Check
//		if (FPSet.isEmpty()){
//			LOGGER.info("*** Freeness achieved for component "
//					+ this.getSimContainer().getHostComponent().getId()
//					+ " at VT: "+Engine.getDefault().getVirtualTime()
//					+"! ***");
//			this.collectResultCallBack.callback(currentEvent, null);
//			
//
//			//debugging purpose
//			//System.exit(0);
//		}
//
//		getLOGGER().info(
////				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
////				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
//				" \n *************** current FPSet : " + FPSet
//				+ " \nvirtual time "+Engine.getDefault().getVirtualTime());
//			
//		
//		
//	}
//	//
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
	
	
	private class WaitingForRootAckPastCreateOD extends Observable{		
		private HashSet <PastEdge> pEdgesToAck;
		private SimEvent currentEvent;
		private CallBack callBack;
		/**
		 * @param callBack 
		 * @param id
		 * @param f
		 */
		public WaitingForRootAckPastCreateOD(CallBack callBack) {
			this.callBack=callBack;
			pEdgesToAck=new HashSet <PastEdge>();
			
		}
		public void toWait(PastEdge pe){
			this.pEdgesToAck.add(pe);
		}
		public boolean conditionCheck(){
			return pEdgesToAck.isEmpty();
		}

		public void ackReceived(SimEvent currentEvent,PastEdge pe){
			this.currentEvent=currentEvent;
			this.pEdgesToAck.remove(pe);
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

	private class WaitingForSubAckPastCreateOD extends Observable{
		private HashSet <PastEdge>pEdgesToAck;
		private SimEvent currentEvent;
		private ArrayList<PastEdge>path;
		
		public WaitingForSubAckPastCreateOD(ArrayList<PastEdge>path){
			pEdgesToAck= new HashSet <PastEdge>();
			this.path=path;
		}
		public void toWait(PastEdge pe){
			this.pEdgesToAck.add(pe);
		}
		private boolean conditionCheck(){
			return pEdgesToAck.isEmpty();
		}
		public void ackReceived(SimEvent currentEvent,PastEdge pe){
			this.currentEvent=currentEvent;
			this.pEdgesToAck.remove(pe);
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
		public ArrayList<PastEdge> getPath() {
			return path;
		}	
			
	}
	
}
