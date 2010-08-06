package it.polimi.vcdu.alg;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Logger;


import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Configuration;
import it.polimi.vcdu.model.DynamicEdge;
import it.polimi.vcdu.model.FutureEdge;
import it.polimi.vcdu.model.FutureEdgeOD;
import it.polimi.vcdu.model.InPort;
import it.polimi.vcdu.model.Message;
import it.polimi.vcdu.model.OutPort;
import it.polimi.vcdu.model.PastEdge;
import it.polimi.vcdu.model.PastEdgeOD;
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
		
	private float vCOndemandReqTime = -1.0f;
	
	private HashSet<Component> vCScope;
	private HashSet<OutPort> requestOnDemandToWait;
	
	private HashSet<OutPort> inScopeOutPorts = new HashSet<OutPort>();
	private HashSet<InPort> inScopeInPorts = new HashSet<InPort>();
	
	private ArrayList<DeferredMethod> blockedMethodsDueToOnDemandSettingUp = new ArrayList<DeferredMethod>();
	
	// OnDemandWaitingForAckEdgeCreateCondition repository also includes those waiting condidtion objects for 
	// normal setting up of f edges for new root txs.
	// So we need to remember those root tx ids that set up on demand, so that we can know when on demand setting up is ready
	private HashMap<String,Observable> waitingForEdgeCreateConditionObjs= new HashMap<String, Observable>();
	private HashSet<String> onDemandPaths = new HashSet<String>();
	private boolean isLocalSettingUpDone = false; //is setting up for local root edges done?
	private HashSet<Component> directDependingComponentsToWaitForLocalSettingUpDone; // Once empty this component can swith to VC
	private HashSet<Component> allDependingComponentsToWaitForLocalSettingUpDone; //for targeted component only.  Once empty, beging to achieving freeness
	
	
	public VCOnDemand(SimContainer simCon) {
		super(simCon);
		dDMngMode  = DDMngMode.DEFAULT;
	}
	
	// I am the targeted component
	public void onBeingRequestOnDemand(SimEvent currentEvent){
		vCOndemandReqTime = Engine.getDefault().getVirtualTime();
		LOGGER.info("*** Request received to achieve freeness. Now setting up dynamic dependences from"
				+ getSimContainer().getHostComponent().getId()
				+ " at VT: "+Engine.getDefault().getVirtualTime() +" ***");
		this.collectReqSettingCallBack.callback(currentEvent, null);
		Component hostComponent = this.getSimContainer().getHostComponent();
		HashSet<Component> scope = computeAffectedScope(hostComponent, hostComponent.getConf());
		allDependingComponentsToWaitForLocalSettingUpDone = new HashSet<Component>(scope); 
		this.onBeingRequestOnDemand(currentEvent,scope,null);
	}
	
	public void onBeingRequestOnDemand(SimEvent currentEvent, HashSet<Component> scope, StaticEdge ose){
		LOGGER.info("*** Request received to achieve freeness. Now setting up dynamic dependences from edge "
				+ ose
				+ " at VT: "+Engine.getDefault().getVirtualTime() +" ***");
		if(this.vCScope == null){ // this is the first request
			this.vCScope = scope;
			
			//compute inScopeOutPorts and inScopeInPorts;
			Component host = this.getSimContainer().getHostComponent();
			for(OutPort op: host.getOutPorts()){
				if(this.vCScope.contains(op.getPeerPort().getHost())){
					this.inScopeOutPorts.add(op);
				}
			}
			
			directDependingComponentsToWaitForLocalSettingUpDone = new HashSet<Component>();
			for(InPort ip: host.getInPorts()){
				Component peerhost = ip.getPeerPort().getHost();
				assert this.vCScope.contains(peerhost); // currently, all in-ports should be in
				this.inScopeInPorts.add(ip);
				directDependingComponentsToWaitForLocalSettingUpDone.add(peerhost);
			}
			//we need to wait for all request from outgoing components in scope, setup the waitng
			this.requestOnDemandToWait = new HashSet<OutPort>(inScopeOutPorts);
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
				+ "swith to ONDEMAND at VT: "+Engine.getDefault().getVirtualTime() +" ***");
		
		this.dDMngMode = DDMngMode.ONDEMAND;
		
		vCOndemandReqTime = Engine.getDefault().getVirtualTime();
		
		// ask all up stream components to vc ondemand
		for (InPort ip: this.inScopeInPorts){
			//create a message to call the onBeingRequestOnDemand of the component 
			this.reqOnDemand(currentEvent, ip);
		}
		
		// setting up edges for all ongoing transactions, now we only need to consider root txs, 
		for (Transaction tx : this.getSimContainer().getHostComponent().getLocalTransactions()){
			if(tx.isRoot()){
				CallBack callback = new CallBack(getSimContainer()){
					@Override
					public void callback(SimEvent currentEvent, Object[] parameters) {
						checkAndNotifyFinishSetupOD(currentEvent);
					}
					
				};
				setupDynamicEdgesOnDemand(currentEvent, tx,callback);
			}
		}
		checkAndNotifyFinishSetupOD(currentEvent);
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
			HashSet<OutPort> futures = new HashSet<OutPort>();
			for(int i=tx.getIteration().getCurrent_step();i<tx.getIteration().getList().size()-1; i++) {
				OutPort outPort = tx.getIteration().getList().get(i).getOutPort();
				if(this.inScopeOutPorts.contains(outPort)){
					futures.add(outPort);
				}
			};
			HashSet<OutPort> pasts = new HashSet<OutPort>();
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
			WaitingForRootAckFutureCreate waitingObjFuture = new WaitingForRootAckFutureCreate(callback);
			waitingObjFuture.addObserver(new Observer(){
				@Override
				public void update(Observable o, Object arg) {
					SimEvent currentEvent = ((WaitingForRootAckFutureCreate)o).getCurrentEvent();
					CallBack callBack= ((WaitingForRootAckFutureCreate)o).getCallBack();
					callBack.callback(currentEvent, null);				
				}
			});
			if(!futures.isEmpty()){
				ArrayList<FutureEdge> path= new ArrayList<FutureEdge>();
				FutureEdge localFeOrg = host.getLocalFe(tx.getId());
				FutureEdgeOD localFeOD = new FutureEdgeOD(localFeOrg);
				path.add(localFeOD);
				this.waitingForEdgeCreateConditionObjs.put(path.toString(), waitingObjFuture);
				this.onDemandPaths.add(path.toString());
				for(OutPort op: futures){
					FutureEdge fe= new FutureEdge(op,op.getPeerPort(),tx.getId());
					host.addToOES(fe);
					waitingObjFuture.toWait(fe);	
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
			
			
		
			LOGGER.fine("we are waiting for acks of future edge creation on demand from ports: "+ futures+
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
			if(! pasts.isEmpty()){
				ArrayList<PastEdge> ppath= new ArrayList<PastEdge>();
				PastEdge localPeOrg = host.getLocalPe(tx.getId());
				PastEdgeOD localPeOD = new PastEdgeOD(localPeOrg);
				ppath.add(localPeOD);
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
			}
			
			waitingObjFuture.setCurrentEvent(currentEvent);
			waitingObjPast.setCurrentEvent(currentEvent);
		
			LOGGER.fine("we are waiting for acks of past edge creation on demand from ports: "+ pasts+
					" At virtual time: "+Engine.getDefault().getVirtualTime() +
					"; The tx: "+tx.getAncestors());
		}
		
		Collection<Observable> allWaitingObjs = new HashSet<Observable>(waitingForEdgeCreateConditionObjs.values());
//		for(Observable ob: allWaitingObjs){
//			if(ob instanceof WaitingForRootAckFutureCreate){
//				((WaitingForRootAckFutureCreate)ob).checkAndNotify();
//			}else{
//				((WaitingForRootAckPastCreateOD)ob).checkAndNotify();
//			}
//		}
	}
	
	private void checkAndNotifyFinishSetupOD(SimEvent currentEvent){
		Set<String> currentPaths = this.waitingForEdgeCreateConditionObjs.keySet();
		for(String path: currentPaths){
			if (this.onDemandPaths.contains(path)){ // we are not ready yet.
				return;
			}
		}
		if (this.isLocalSettingUpDone){ // no need to do it again.
			return;
		}
		
		this.isLocalSettingUpDone = true; // this value  affects depended components
		this.checkAndSwitchToVC(currentEvent);
		
		LOGGER.info("*** Component: " + this.getSimContainer().getHostComponent().getId()
				+ " finished local setting up at VT: " + Engine.getDefault().getVirtualTime());
		// notify depended components through out going components
		for(OutPort op: this.inScopeOutPorts){
			Object[] content= new Object[2];
			content[0]= "notifyOnDemandSettingUpLocalDone";
			content[1]= this.getSimContainer().getHostComponent();
			Object[] params= new Object[1];			
			params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
			currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);	
			LOGGER.info("*** Component: " + this.getSimContainer().getHostComponent().getId()
					+ "notify depending component: " + op.getPeerPort().getHost().getId());
		}	
		
		if (allDependingComponentsToWaitForLocalSettingUpDone!=null){// I am the targeted component
			allDependingComponentsToWaitForLocalSettingUpDone.remove(getSimContainer().getHostComponent());
			checkAndSwithToReconfig(currentEvent);
		}

 	}
	
	// I am notified a depending component has finished its setting up.
	public void notifyOnDemandSettingUpLocalDone(SimEvent currentEvent, Component theComponent){
		//Let's pass it down. so that the targeted node can know all the setting up is done and then
		// begins to block for freeness
		for(OutPort op: this.inScopeOutPorts){
			Object[] content= new Object[2];
			content[0]= "notifyOnDemandSettingUpLocalDone";
			content[1]= theComponent;
			Object[] params= new Object[1];			
			params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
			currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);		
		}	
		
		this.directDependingComponentsToWaitForLocalSettingUpDone.remove(theComponent);
		checkAndSwitchToVC(currentEvent);
		
		
		if (allDependingComponentsToWaitForLocalSettingUpDone!=null){// I am the targeted component
			allDependingComponentsToWaitForLocalSettingUpDone.remove(theComponent);
			checkAndSwithToReconfig(currentEvent);
		}
	}
	
	private void checkAndSwitchToVC(SimEvent currentEvent){
		if(this.dDMngMode== DDMngMode.VC){ //we are already in VC
			return;
		}
		if(this.directDependingComponentsToWaitForLocalSettingUpDone.isEmpty() && this.isLocalSettingUpDone){
			// Good, now we can switch to VC
			this.dDMngMode = DDMngMode.VC;
			LOGGER.info("Component:" + this.getSimContainer().getHostComponent().getId() 
					+" switch to VC, and resume all blocked txs, at VT: " + Engine.getDefault().getVirtualTime());
			// resume the blocked initing/ending of subtxs
			for (DeferredMethod dm:this.blockedMethodsDueToOnDemandSettingUp){
				Object[] params = dm.getParams();
				assert params[0] == null;
				params[0] = currentEvent;
				dm.setParams(params); //not necessary but ...
				dm.run();
			}
		}
	}

	private void checkAndSwithToReconfig(SimEvent currentEvent){
		if(this.allDependingComponentsToWaitForLocalSettingUpDone.isEmpty())
			if(! this.startReconf){
				LOGGER.info("Component:" + this.getSimContainer().getHostComponent().getId() 
						+" start reconfigure after setting up on demand, at VT: " + Engine.getDefault().getVirtualTime());
				this.startReconfAfterSettingUpOnDemandReady(currentEvent);
			}
	}


	/**
	 * Utility functions used in this class.
	 */
	
	public static HashSet<Component> computeAffectedScope(Component com, Configuration conf){
		HashSet<Component> resultScope = new HashSet<Component>();
		HashSet<Component> allComponents = new HashSet<Component>(conf.getComponents());
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
	
	
	
	/**
	 * Following is copied from VersionConsistency
	 */
	
//	private HashMap<String,Observable> WaitingForAckFutureCreateCondition= new HashMap<String, Observable>();
//
//
//	private float VersConsistencyReqTime = -1.0f;
	private boolean startReconf=false;
	private HashSet<String> FPSet = new HashSet<String>();
	private boolean waitingInsteadOfBlocking=false;


	
	public void startReconfAfterSettingUpOnDemandReady(SimEvent currentEvent){
		LOGGER.info("************** STARTING RECONFIGURATION OF COMPONENT "+this.getSimContainer().getHostComponent().getId()+" ***************\n " +
				"virtual time "+Engine.getDefault().getVirtualTime());
		
		this.startReconf=true;
		//this.collectReqSettingCallBack.callback(currentEvent, null);
//		getLOGGER().info(
//				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
//				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
//				" \n *************** current FPSet : " + FPSet);
		
		checkFreeness(currentEvent);
	}
	public void startReconfWaitingAfterSettingUpOnDemandReady(SimEvent currentEvent){
		LOGGER.info("************** STARTING RECONFIGURATION OF COMPONENT "+this.getSimContainer().getHostComponent().getId()+" ***************\n " +
				"virtual time "+Engine.getDefault().getVirtualTime());
		this.startReconf=true;
		this.waitingInsteadOfBlocking=true;
		//this.collectReqSettingCallBack.callback(currentEvent, null);
//		getLOGGER().info(
//				"************** Current Local Txs: "+this.getSimContainer().getHostComponent().getLocalTransactions()
//				+" \n *************** current IES : " + this.getSimContainer().getHostComponent().getIES() +
//				" \n *************** current FPSet : " + FPSet);
		
		checkFreeness(currentEvent);
	}
	
	@Override
	public void onInitRootTx(SimEvent currentEvent, SimAppTx simApp,CallBack callBack) {
		if(this.dDMngMode==DDMngMode.DEFAULT){
			assert ! startReconf;
			assert ! waitingInsteadOfBlocking;
			super.onInitRootTx(currentEvent, simApp, callBack);
			return;
		}
		
		//For both DDMngMode.ONDEMAND and DDMngMode.VC, we do the same as VersionConsistency but 
		//with considerations of scope
		
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
		if(this.dDMngMode==DDMngMode.DEFAULT){
			assert ! startReconf;
			assert ! waitingInsteadOfBlocking;
			super.onToGoBeforeRootInitFirstSubTx(currentEvent, simApp, callBack);
			return;
		}
		
		//For both DDMngMode.ONDEMAND and DDMngMode.VC, we do the same as VersionConsistency but 
		//with considerations of scope
		
		Transaction tx= simApp.getTransaction();
		assert tx.isRoot();
		
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
		this.waitingForEdgeCreateConditionObjs.put(path.toString(), waitingObj);
		while(it.hasNext()){ // we need to consider the scope
			OutPort op=it.next();
			assert this.inScopeOutPorts!=null;
			if (inScopeOutPorts.contains(op)){
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
				getLOGGER().fine("we are waiting for acks of future edge creation from edge: "+ fe +
						" At virtual time: "+Engine.getDefault().getVirtualTime() +
						"; The tx: "+tx.getAncestors());
			}
		}	
		waitingObj.setCurrentEvent(currentEvent);
		waitingObj.checkAndNotify();

	}
	
	/**
	 * Some depending neighbor notifies me that it creates a future edge to me.
	 * @param currentEvent
	 * @param path
	 */
	public void notifyFutureCreate(SimEvent currentEvent, ArrayList<FutureEdge> path){
		assert this.vCScope.contains(this.getSimContainer().getHostComponent());
		assert this.dDMngMode != DDMngMode.DEFAULT;
		
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
			if(this.vCScope.contains(peerhost)){ // we need to consider the scope
				assert this.inScopeOutPorts.contains(op);
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

				getLOGGER().fine("we are waiting for ack of future edge creation from edge: "+ fe+
						" At virtual time: "+Engine.getDefault().getVirtualTime() +
						"; The path: "+path);
			}
		}
		waitingObj.setCurrentEvent(currentEvent);
		waitingObj.checkAndNotify();
		
	}
	
	public void ackFutureCreate(SimEvent currentEvent, ArrayList<FutureEdge> path){
		assert this.vCScope.contains(this.getSimContainer().getHostComponent());
		assert this.dDMngMode != DDMngMode.DEFAULT;

		LOGGER.info("Ack of non local future edge creation received of path: "+ path+
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
		LOGGER.info("Being notified a non local past edge to me is created on demand. At virtual time: "
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
				getLOGGER().fine("we are waiting for acks of past edge on demand creation from port: "+ op+
						" At virtual time: "+Engine.getDefault().getVirtualTime() +
						"; The path: "+path);
			}
		}
		waitingObj.setCurrentEvent(currentEvent);
		waitingObj.checkAndNotify();
	}
	
	public void ackPastCreateDD(SimEvent currentEvent, ArrayList<PastEdge> path){

		getLOGGER().info("Ack of non local past edge creation received of path: "+ path+
				" At virtual time: "+Engine.getDefault().getVirtualTime() );
		
		assert path.size()>1;
		PastEdge fe=path.get(path.size()-1);
		path.remove(fe);
		Observable o=this.waitingForEdgeCreateConditionObjs.get(path.toString());
		assert o!=null;
		if (path.size()==1){
			WaitingForRootAckPastCreateOD waitingObj=(WaitingForRootAckPastCreateOD)o;			 
			waitingObj.ackReceived(currentEvent, fe);
		}
		else{
			
			WaitingForSubAckPastCreateOD waitingObj=(WaitingForSubAckPastCreateOD)o;			 
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
	
	// this method is going to be blocked, and resumed when switching to VC
	public void onBeingInitSubTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {	
		
		if(this.dDMngMode==DDMngMode.DEFAULT){
			assert ! startReconf;
			assert ! waitingInsteadOfBlocking;
			super.onBeingInitSubTx(currentEvent, simApp, callBack);
			return;
		}else if (this.dDMngMode==DDMngMode.ONDEMAND){
			Object[] params = new Object[3];
			params[0] = null; // currentEvent to be set later when called;
			params[1] = simApp;
			params[2] = callBack;
			DeferredMethod dm = new DeferredMethod(this, "onBeingInitSubTx", params);
			this.blockedMethodsDueToOnDemandSettingUp.add(dm);
			return;
		}
		
		assert this.dDMngMode==DDMngMode.VC;
		
		//Reconfiguration Starts: no new sub transactions with new rid accepted
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
		assert this.dDMngMode!=DDMngMode.DEFAULT;
		this.removeFutureEdges(currentEvent, rid);
	}
	
	public void notifyFutureRemove(SimEvent currentEvent, FutureEdge fe ){
		assert this.dDMngMode!=DDMngMode.DEFAULT;
		String rid= fe.getRid();
		Component host= this.getSimContainer().getHostComponent();
		host.removeFromIES(fe);
		this.removeFutureEdges(currentEvent, rid);
		if(this.startReconf ){
			checkFreeness(currentEvent);
		}
	}	
	
	// we also need to block this method when ONDEMAND
	public void onEndingSubTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		if(this.dDMngMode==DDMngMode.DEFAULT){
			assert ! startReconf;
			assert ! waitingInsteadOfBlocking;
			super.onEndingSubTx(currentEvent, simApp, callBack);
			return;
		}else if (this.dDMngMode==DDMngMode.ONDEMAND){
			Object[] params = new Object[3];
			params[0] = null; // currentEvent to be set later when called;
			params[1] = simApp;
			params[2] = callBack;
			DeferredMethod dm = new DeferredMethod(this, "onEndingSubTx", params);
			this.blockedMethodsDueToOnDemandSettingUp.add(dm);
			return;
		}
		assert this.dDMngMode==DDMngMode.VC;
		super.onEndingSubTx(currentEvent, simApp, callBack);
	}

	public void onBeingEndingTx(SimEvent currentEvent,SimAppTx simApp,OutPort op, CallBack callBack) {
		if(this.dDMngMode==DDMngMode.DEFAULT){
			super.onBeingEndingTx(currentEvent, simApp, op, callBack);
			return;
		}else{
			if(this.inScopeOutPorts.contains(op)){
				PastEdge pe = new PastEdge(op,op.getPeerPort(),simApp.getTransaction().getRootId());
				this.getSimContainer().getHostComponent().addToOES(pe);
				Object[] content= new Object[2];
				content[0]= "notifyPastCreate";
				content[1]=pe;
				
				Object[] params= new Object[1];
		
				params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
				currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);	
		
				
				callBack.callback(currentEvent, null);
			}else{
				super.onBeingEndingTx(currentEvent, simApp, op, callBack);
			}
		}
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
		assert this.dDMngMode!=DDMngMode.DEFAULT;
		
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
		assert this.dDMngMode!=DDMngMode.DEFAULT;
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
	
	// we should also delay this method
	public void onEndingRootTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		if(this.dDMngMode==DDMngMode.DEFAULT){
			super.onEndingRootTx(currentEvent, simApp, callBack);
			return;
		}else if(this.dDMngMode==DDMngMode.ONDEMAND){
			Object[] params = new Object[3];
			params[0] = null; // currentEvent to be set later when called;
			params[1] = simApp;
			params[2] = callBack;
			DeferredMethod dm = new DeferredMethod(this, "onEndingRootTx", params);
			this.blockedMethodsDueToOnDemandSettingUp.add(dm);
			return;
		}
		assert dDMngMode==DDMngMode.VC;
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
			this.checkAndNotify();			
		}
		public void checkAndNotify(){
			this.setChanged();			
			if (conditionCheck()) {
				VCOnDemand.this.removeFromWaitingForEdgeCreateConditionObjs(this);
				this.notifyObservers();		
			}
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
			this.checkAndNotify();			
		}
		public void checkAndNotify(){
			this.setChanged();			
			if (conditionCheck()) {
				VCOnDemand.this.removeFromWaitingForEdgeCreateConditionObjs(this);
				this.notifyObservers();		
			}		
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
			if (conditionCheck()) {
				VCOnDemand.this.removeFromWaitingForEdgeCreateConditionObjs(this);
				this.notifyObservers();		
			}		
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
			if (conditionCheck()) {
				VCOnDemand.this.removeFromWaitingForEdgeCreateConditionObjs(this);
				this.notifyObservers();		
			}	
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
	
	private void removeFromWaitingForEdgeCreateConditionObjs(Observable observable){
		//assert this.waitingForEdgeCreateConditionObjs.containsValue(observer);
		String key = null;
		for(String s : waitingForEdgeCreateConditionObjs.keySet()){
			if (waitingForEdgeCreateConditionObjs.get(s) == observable) {
				key =s;
				break;
			}
		}
		assert key!=null;
		waitingForEdgeCreateConditionObjs.remove(key);
	}
	
	
	private class DeferredMethod {
		Object object;
		String methodName;
		Object[] params;
		
		public DeferredMethod(Object obj, String mtdname, Object[] params){
			this.object = obj;
			this.methodName = mtdname;
			this.params = params;
		}

		public void run(){
			LOGGER.info("*** Call deferred method " + methodName);
			//SimEvent.runMethod(methodName, object, params); //does not work, why?
			SimEvent currentEvent = (SimEvent) params[0];
			SimAppTx simApp = (SimAppTx)params[1];
			CallBack callBack = (CallBack)params[2];
			if(methodName.equals("onBeingInitSubTx")){
				onBeingInitSubTx(currentEvent,simApp,callBack);
			}else if(methodName.equals("onEndingSubTx")){
				onEndingSubTx(currentEvent,simApp,callBack);
			}else{
				assert (methodName.equals("onEndingRootTx"));
				onEndingRootTx(currentEvent,simApp,callBack);
			}	
		}
		public Object[] getParams() {
			return params;
		}
		public void setParams(Object[] params) {
			this.params = params;
		}
	}
}
