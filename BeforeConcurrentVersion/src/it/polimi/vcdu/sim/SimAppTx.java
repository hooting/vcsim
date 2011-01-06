package it.polimi.vcdu.sim;

import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Iteration;
import it.polimi.vcdu.model.Message;
import it.polimi.vcdu.model.Transaction;
import it.unipr.ce.dsg.deus.core.Engine;

import java.util.ArrayList;
import java.util.logging.Logger;


public class SimAppTx extends SimObject {
	
	private static final Logger LOGGER = Logger.getLogger(SimAppTx.class.getName());
	
	private Transaction transaction;
	private SimContainer simContainer;
	private Message initiatingMessage;
	public SimAppTx(Component host,SimContainer simContainer,ArrayList<String> ancestors){
		super(host);
		this.simContainer=simContainer;
		this.transaction=new Transaction(host,ancestors);
	}

	/*
	 * Following are behaviors of a transaction
	 */
	/**
	 * Now I need to init a root tx
	 */
	public void onInitRootTx(SimEvent currentEvent){	
		
		this.transaction.getIteration().txCreatingRecordTime();
		LOGGER.fine("Root Tx "+this.transaction+ " starts at VT: " + Engine.getDefault().getVirtualTime());
		this.simContainer.onInitRootTx(currentEvent, this);
		//the simuContainer will call back my method readyToInitRootTx
	}	

	/**
	 * Now I need to start a sub tx
	 * @param currentEvent
	 */
	public void onStartSubTx(SimEvent currentEvent){
		
		this.transaction.getIteration().txCreatingRecordTime();
		

		LOGGER.fine("Sub Tx "+this.transaction+ " starts at VT: " + Engine.getDefault().getVirtualTime());
		
		this.simContainer.onStartSubTx(currentEvent, this);
		//the simContainer will call back my method onNextStep 
		
	}
	
	/**
	 * This method is called when current step is done.
	 * @param currentEvent
	 */
	public void onNextStep(SimEvent currentEvent){
		this.transaction.getIteration().txToWorkingRecordTime();

		
		Iteration iteration = this.transaction.getIteration();
		assert !iteration.isEnd();
		
		iteration.nextStep();
//		assert !iteration.isEnd();
		float delay=iteration.getCurrentStep().getDelay();
		Object parameters[]= new Object[1];
		parameters[0]= this;
		
		String methodName;
		
		if(!iteration.isEnd()){
			methodName = "onInitSubTx";		
		}
		else{
			methodName = "onEndingTx";
		}
		currentEvent.notifyWithDelay(methodName, this.simContainer, parameters, delay);
	}
	
	/*
	 * Following are interface methods for SimContainer to call
	 */
	
	public void readyToInitRootTx(SimEvent currentEvent, Object[] params){
		
		this.transaction.getIteration().txToWorkingRecordTime();
		
		Iteration iteration = this.transaction.getIteration();
		iteration.nextStep();

		float delay=iteration.getCurrentStep().getDelay();
		Object parameters[]= new Object[1];
		parameters[0]= this;
		String methodName;
		
		if(!iteration.isEnd()){
			methodName = "onToGoBeforeRootInitFirstSubTx";
		}
		else{
			methodName = "onEndingTx";
		}

		currentEvent.notifyWithDelay(methodName, this.simContainer, parameters, delay);
	}
	
	/**
	 * The container has finished processing that need to be done before I initiating first sub tx 
	 * @param currentEvent
	 * @param params
	 */
	public void readyToGoBeforeRootInitFirstSubTx(SimEvent currentEvent, Object[] params){

		this.transaction.getIteration().txToWorkingRecordTime();
		
		// now we can init sub tx normally
		// without deay we init the first transaction
		this.simContainer.onInitSubTx(currentEvent,this);
		//the simuContainer will call back my method readyToInitSubTx
	}
	
	
	public void readyToInitSubTx(SimEvent currentEvent){

//		this.transaction.getIteration().txToWorkingRecordTime();
		
		// now we waiting for subTxEnd
		
	}
	
	//I'm notified by my SimContainter that my SubTransaction ends
	public void onSubTxEnd(SimEvent currentEvent, String subTxID, Object[] params){
		
		
		onNextStep(currentEvent);
	}
	
	public void readyToEndTx(SimEvent currentEvent, Object[] params){

		this.transaction.getIteration().txToWorkingRecordTime();
		this.transaction.getIteration().txEnddingRecondTime();
		this.transaction.getHost().accumulateTxRecordTime(this.transaction);
		
		//nothing to do
		LOGGER.fine("Tx: "+ this.transaction.getAncestors()+" ends at VT: "
				+ Engine.getDefault().getVirtualTime());
		
		//LOGGER.info(Simulator.DefaultSimulator.getConf().toString());
		
	}
	
	//this method is called by myself to ask for the container if I'm ready to end the transaction
//	public void endingTx(SimEvent currentEvent){
//		this.simContainer.onEndingTx(currentEvent,this, this.transaction);
//		
//	}

	/*
	 * Getters and Setters
	 */
	/**
	 * @return the transaction
	 */
	public Transaction getTransaction() {
		return transaction;
	}

	/**
	 * @return the initiatingMessage
	 */
	public Message getInitiatingMessage() {
		return initiatingMessage;
	}

	/**
	 * @param initiatingMessage the initiatingMessage to set
	 */
	public void setInitiatingMessage(Message initiatingMessage) {
		this.initiatingMessage = initiatingMessage;
	}
	

}
