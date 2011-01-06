package it.polimi.vcdu.alg;

import it.polimi.vcdu.model.InPort;
import it.polimi.vcdu.model.Message;
import it.polimi.vcdu.model.OutPort;
import it.polimi.vcdu.model.StaticEdge;
import it.polimi.vcdu.model.Transaction;
import it.polimi.vcdu.sim.CallBack;
import it.polimi.vcdu.sim.SimAppTx;
import it.polimi.vcdu.sim.SimContainer;
import it.polimi.vcdu.sim.SimEvent;
import it.polimi.vcdu.sim.Simulator;
import it.unipr.ce.dsg.deus.core.Engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;


public class Quiescence extends Algorithm {
	private static final Logger LOGGER = Logger.getLogger(Quiescence.class.getName());

	public Quiescence(SimContainer simCon) {
		super(simCon);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Logger getLOGGER() {
		return LOGGER;
	}

	/*
	 * Because quieting/passivating works at component-level, we need some
	 * some component-wide states. But we will not change the component and 
	 * container, because we want them to be algorithm-independent.
	 */
	private boolean STOP_INITIATING_ROOT_TX = false;
	/**
	 * A node is passivated if it does not initiate sub-transactions on any depended
	 * node any more. Without dynamic information, this can only be achieved by 
	 * (1) it is locally inactive (STOP_INITIATING_ROOT_TX&&isALL_LOCAL_ROOT_TX_ENDED()) and
	 * (2) all statically depending nodes are passivated. 
	 */
	private boolean PASSIVATED = false;
	
	// passivating requests to ack. in fact they are outgoing static edges from which 
	// these requests come.
	private HashSet<StaticEdge> REQS = new HashSet<StaticEdge>();
	
	// incoming edges from which to sending out passivating requests and wait for acks
	private HashSet<StaticEdge> DEPS = new HashSet<StaticEdge>(); 
	
	private HashSet<Transaction> blockTransactions = new HashSet<Transaction>();
	
	private boolean amItheOneToBeQuieted = false;
	private float quiescenceReqTime = -1.0f;
	
	/**
	 * called by simContainer to make this node quiescent, or when receiving passivate requests. 
	 * @param currentEvent
	 * @param ose
	 */
	public void onBeingPassivated(SimEvent currentEvent){
		quiescenceReqTime = Engine.getDefault().getVirtualTime();
		this.amItheOneToBeQuieted = true;
		LOGGER.info("*** Request received to quiet component "
				+ getSimContainer().getHostComponent().getId()
				+ " at VT: "+quiescenceReqTime +" ***");
		this.collectReqSettingCallBack.callback(currentEvent, null);
		this.onBeingPassivated(currentEvent,null);
	}
	public void onBeingPassivated(SimEvent currentEvent, StaticEdge ose){
		LOGGER.info("Component "+ this.getSimContainer().getHostComponent().getId()
				+ " being passivated at VT: "+Engine.getDefault().getVirtualTime()
				+", request is from: "+ose);
		if (PASSIVATED) {
			// directly ack back 
			if(ose!=null) ackPassivate(currentEvent,ose);
		}else{
			// rember this reqest, and ack back when passivated
			if(ose!=null) REQS.add(ose);
			if(!this.isSTOP_INITIATING_ROOT_TX()) {
				this.setSTOP_INITIATING_ROOT_TX(true);
				ArrayList<StaticEdge> incomingStaticEdges = getSimContainer().getHostComponent().getIncomingStaticEdges();
				for(StaticEdge ise: incomingStaticEdges){
					DEPS.add(ise);
					reqPassivate(currentEvent,ise);
				}
				this.checkPassiveAndAck(currentEvent);
			}
		}	
	}
	
	/**
	 * called when an ack of passivate requested by me is received -- the corresponding 
	 * depending node is passivated.
	 * @param currentEvent
	 * @param ose
	 */
	public void onBeingAckPassivate(SimEvent currentEvent, StaticEdge ose){
		// we cannot directly call DEPS.remove(ose) because of object identity problem, here we
		// need to decide according to the string representation of static edges.
		

		LOGGER.info("Component "+ this.getSimContainer().getHostComponent().getId()
				+ " receving ack at VT: "+Engine.getDefault().getVirtualTime()
				+", ack is from: "+ose);
		HashSet<StaticEdge> tmp = new HashSet<StaticEdge>(DEPS);
		for(StaticEdge e:tmp){
			 if(e.toString().equals(ose.toString())) DEPS.remove(e);
			 checkPassiveAndAck(currentEvent);
		}
	}
	
	/**
	 * called when a local root tx is being initiated
	 */
	@Override
	public void onInitRootTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		if (this.isSTOP_INITIATING_ROOT_TX()){
			//block the progress of the transaction, and put it in blockedTransactions
			Transaction transaction = simApp.getTransaction();
			this.blockTransactions.add(transaction);
			getLOGGER().fine("Component "+ this.getSimContainer().getHostComponent().getId()
				+ " blocks Transation: "+ transaction 
				+ " at VT: "+Engine.getDefault().getVirtualTime());
		}else{
			super.onInitRootTx(currentEvent, simApp, callBack);
		}
	}
	
	/**
	 * called by simContainer when a sub-transaction initiation request is received. The sub-transaction object 
	 * has already been created. 
	 * @param currentEvent
	 * @param simApp
	 * @param callBack
	 */
	public void onBeingInitSubTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		if (this.isSTOP_INITIATING_ROOT_TX()) {
			getLOGGER().fine("*** I am being passivated, but I continue to serve sub-transaction "
					+ simApp.getTransaction()
					+ " At virtual time: "+Engine.getDefault().getVirtualTime() 
					+ " *** ");
		};
		super.onBeingInitSubTx(currentEvent, simApp, callBack);
	}

	/**
	 * called when a locally hosted root transaction ends. The root transaction has already been 
	 * removed from the localTransations of the host component.
	 * @param currentEvent
	 * @param simApp
	 * @param callBack
	 */
	@Override
	public void onEndingRootTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		super.onEndingRootTx(currentEvent, simApp, callBack);
		LOGGER.info("*** Root transaction "+ simApp.getTransaction() 
				+ " ends at VT: "+Engine.getDefault().getVirtualTime()
				+" ***");
		checkPassiveAndAck(currentEvent);
	}
	
	/**
	 * called when a locally hosted sub-transaction ends, concurrently with container sending 
	 * notification to the host component of its parent. The root transaction has already been 
	 * removed from the localTransations of the host component.
	 * @param currentEvent
	 * @param simApp
	 * @param callBack
	 */
	@Override
	public void onEndingSubTx(SimEvent currentEvent, SimAppTx simApp,
			CallBack callBack) {
		LOGGER.fine("*** Sub-transaction "+ simApp.getTransaction() 
				+ " ends at VT: "+Engine.getDefault().getVirtualTime()
				+" ***");
		super.onEndingSubTx(currentEvent, simApp, callBack);
		
	}
	
	
	
	
	private void checkPassiveAndAck(SimEvent currentEvent) {
		if(DEPS.isEmpty() 
				&& this.isALL_LOCAL_ROOT_TX_ENDED() 
				&& this.isSTOP_INITIATING_ROOT_TX() ){

			LOGGER.info("*** Passivated status achieved for Component "+ this.getSimContainer().getHostComponent().getId()
					+ " at VT: "+Engine.getDefault().getVirtualTime()
					+"! ***");
			
			
			//now passivated status is achieved
			PASSIVATED = true;
			for(StaticEdge ose:REQS){
				ackPassivate(currentEvent,ose);
			}
			
			if(this.amItheOneToBeQuieted) {
				LOGGER.info("*** Quiescence achieved for component "
						+ this.getSimContainer().getHostComponent().getId()
						+ " at VT: "+Engine.getDefault().getVirtualTime()
						+"! ***");
				this.collectResultCallBack.callback(currentEvent, null);
			}
		}
		
	}

	/**
	 * now we send a request msg along the incoming ise to passivate the depending node.
	 * @param currentEvent
	 * @param ise the incoming edge
	 */
	private void reqPassivate(SimEvent currentEvent, StaticEdge ise) {
		InPort ip = ise.getTo();
		Object[] content= new Object[2];
		content[0]= "onBeingPassivated";
		content[1]= ise;
		Object[] params= new Object[1];			
		params[0]=new Message("dispatchToAlg",ip,ip.getPeerPort(),content);				
		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);	
		LOGGER.info("*** Request depending node to be passivated, through incoming edge: " + ise
				+ " at VT: "+Engine.getDefault().getVirtualTime()
				+" ***");
	}

	/**
	 * now we send a ack to the depended node to notice it that I am passivated.
	 * @param currentEvent
	 * @param ose the out-going edge to the depended node
	 */
	private void ackPassivate(SimEvent currentEvent, StaticEdge ose){
		OutPort op = ose.getFrom();
		Object[] content= new Object[2];
		content[0]= "onBeingAckPassivate";
		content[1]= ose;
		Object[] params= new Object[1];			
		params[0]=new Message("dispatchToAlg",op,op.getPeerPort(),content);				
		currentEvent.notifyNoDelay("onSend", this.getSimContainer().getSimNet(), params);
		LOGGER.info("*** Ack back that I am passivated, through out-going edge: " + ose
				+ " at VT: "+Engine.getDefault().getVirtualTime()
				+" ***");
	}
	

	private boolean isALL_LOCAL_ROOT_TX_ENDED() {
		HashSet<Transaction> localTransactions = this.getSimContainer().getHostComponent().getLocalTransactions();
		for(Transaction tx : localTransactions){
			//note that tx might have been blocked on initiating. If so, it does not count here. 
			if (tx.isRoot() && ! this.blockTransactions.contains(tx)) return false;
		}
		return true;
	}

	/**
	 * @return the sTOP_INITIATING_ROOT_TX
	 */
	public boolean isSTOP_INITIATING_ROOT_TX() {
		return STOP_INITIATING_ROOT_TX;
	}

	/**
	 * @param sTOP_INITIATING_ROOT_TX the sTOP_INITIATING_ROOT_TX to set
	 */
	public void setSTOP_INITIATING_ROOT_TX(boolean sTOP_INITIATING_ROOT_TX) {
		STOP_INITIATING_ROOT_TX = sTOP_INITIATING_ROOT_TX;
		LOGGER.info("Component "+ this.getSimContainer().getHostComponent().getId()
				+ " stops to initiating root tx at VT: "+Engine.getDefault().getVirtualTime());
	}
	
}
