package it.polimi.vcdu.alg;

import it.polimi.vcdu.model.OutPort;
import it.polimi.vcdu.model.Transaction;
import it.polimi.vcdu.sim.CallBack;
import it.polimi.vcdu.sim.SimAppTx;
import it.polimi.vcdu.sim.SimContainer;
import it.polimi.vcdu.sim.SimEvent;
import it.unipr.ce.dsg.deus.core.Engine;

import java.util.logging.Logger;


public abstract class Algorithm {
	private SimContainer simContainer;
	
	public Algorithm(SimContainer simCon){
		this.simContainer=simCon;
	}
	
	protected abstract Logger getLOGGER();
	
	protected CallBack collectReqSettingCallBack;
	protected CallBack collectResultCallBack;
	public void setCollectReqSettingCallBack(CallBack callBack){
		this.collectReqSettingCallBack = callBack;
	};
	public void setCollectResultCallBack(CallBack callBack){
		this.collectResultCallBack = callBack;
	};
	
	

	public void onInitRootTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		Transaction tx= simApp.getTransaction();
		
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The root tx: "+tx.getAncestors());
		callBack.callback(currentEvent, null);
	}

	public void onToGoBeforeRootInitFirstSubTx(SimEvent currentEvent, SimAppTx simApp,  CallBack callBack) {
		Transaction tx= simApp.getTransaction();
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+tx.getAncestors());
		callBack.callback(currentEvent, null);
	}

	/**
	 * called by simContainer when a sub-transaction initiation request is received. The sub-transaction object 
	 * has already been created. 
	 * @param currentEvent
	 * @param simApp
	 * @param callBack
	 */
	public void onBeingInitSubTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+simApp.getTransaction().getAncestors());
		callBack.callback(currentEvent, null);
	}
	
	/**
	 * called by simContainer, when notified the end of a sub-transaction of a locally hosted transaction.  
	 * @param currentEvent
	 * @param simApp the SimAppTx object in charge of the local transaction (the parent of the ending sub-transaction).
	 * @param port the sub-transaction was initiated through this out-port.
	 * @param callBack
	 */
	public void onBeingEndingTx(SimEvent currentEvent, SimAppTx simApp,OutPort port, CallBack callBack) {
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime());
		callBack.callback(currentEvent, null);
	}

	/**
	 * called when a locally hosted root transaction ends. The root transaction has already been 
	 * removed from the localTransations of the host component.
	 * @param currentEvent
	 * @param simApp
	 * @param callBack
	 */
	public void onEndingRootTx(SimEvent currentEvent, SimAppTx simApp, CallBack callBack) {
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+simApp.getTransaction().getAncestors());
		callBack.callback(currentEvent, null);
	}



	/**
	 * @return the simContainer
	 */
	public SimContainer getSimContainer() {
		return simContainer;
	}

	public void onStartSubTx(SimEvent currentEvent, SimAppTx simApp,
			Transaction tx, CallBack callBack) {
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime());
		callBack.callback(currentEvent, null);
	}

	/**
	 * called when a locally hosted sub-transaction ends, concurrently with container sending 
	 * notification to the host component of its parent. The root transaction has already been 
	 * removed from the localTransations of the host component.
	 * @param currentEvent
	 * @param simApp
	 * @param callBack
	 */
	public void onEndingSubTx(SimEvent currentEvent, SimAppTx simApp,
			CallBack callBack) {
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+simApp.getTransaction().getAncestors());
		callBack.callback(currentEvent, null);
		
	}

	/*public void onEndingSubTx(SimEvent currentEvent, SimAppTx simApp,
			CallBack callBack) {
		getLOGGER().fine("Before callback "+ currentEvent.getId()+
				" At virtual time: "+Engine.getDefault().getVirtualTime() +
				"; The tx: "+simApp.getTransaction().getAncestors());
		callBack.callback(currentEvent, null);
		
	}
*/
}
