package it.polimi.vcdu.sim.record;

import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.Transaction;
import it.polimi.vcdu.sim.Simulator;
import it.unipr.ce.dsg.deus.core.Engine;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;


public class Recorder {

	
	private HashMap<String, ArrayList<AbstractMap.SimpleEntry<String, Float>>> totalHistory; // <ComponentName, [<rootTxID, injectTime>]>
	private HashMap<String, Iterator<AbstractMap.SimpleEntry<String, Float>>> replayRootTxStep; // <ComponentName, nextRootTx>

	private HashMap<String, ArrayList<TransactionRecord>> txHistories; // <rootTxName, transaction-subtxs>
	private HashMap<String, Iterator<TransactionRecord>> replayCurrentStep; // <rootTxID, currentStep>
	
	private int maxNumberOfTxs = 100000;
	public void setMaxNumberOfTxs(int maxNumberOfTxs) {
		this.maxNumberOfTxs = maxNumberOfTxs;
	}

	private int numberOfRecordedTxs=0;

	public Recorder() {
		totalHistory = new HashMap<String, ArrayList<AbstractMap.SimpleEntry<String, Float>>>();
		txHistories = new HashMap<String, ArrayList<TransactionRecord>>();
		replayCurrentStep = new HashMap<String, Iterator<TransactionRecord>>();
		replayRootTxStep = new HashMap<String, Iterator<AbstractMap.SimpleEntry<String, Float>>>();
	};
	
	public void reInit(){
		replayCurrentStep = new HashMap<String, Iterator<TransactionRecord>>();
		replayRootTxStep = new HashMap<String, Iterator<AbstractMap.SimpleEntry<String, Float>>>();
	}
	
	public void  recordRootTxInjectTime(String componentName, String rootTxName, float injectTime){
		ArrayList<AbstractMap.SimpleEntry<String, Float>> injTimes;
		if(totalHistory.containsKey(componentName)){
			injTimes = totalHistory.get(componentName);
		}else{
			injTimes = new 	ArrayList<AbstractMap.SimpleEntry<String, Float>>();
			totalHistory.put(componentName, injTimes);
		}
		injTimes.add(new AbstractMap.SimpleEntry<String, Float>(rootTxName, injectTime));
		txHistories.put(rootTxName, new ArrayList<TransactionRecord>());
	}
	
	public void recordTransaction(String rootTxID, Transaction tx){
		// note that we just use it's local processing time and iteration!
		ArrayList<TransactionRecord> theList = txHistories.get(rootTxID);
		TransactionRecord transactionRecord = new TransactionRecord(tx);
		theList.add(transactionRecord);
		this.numberOfRecordedTxs++;
		if(this.numberOfRecordedTxs>=this.maxNumberOfTxs){
			Simulator.getDefaultSimulator().setStopSimulation(true);
			Logger.getLogger("it.polimi.vcdu").info("==== Max Number of Tranaction Recorded =====");
		}
	}
	
	public HashMap<String, ArrayList<AbstractMap.SimpleEntry<String, Float>>> getTotalHistory() {
		return totalHistory;
	}
	
	public String getNextRootTransactionID(String hostName){
		Iterator<AbstractMap.SimpleEntry<String, Float>> iterNextRootTx;
		if(this.replayRootTxStep.containsKey(hostName)){
			iterNextRootTx = replayRootTxStep.get(hostName);
		}else{
			iterNextRootTx = this.totalHistory.get(hostName).iterator();
			this.replayRootTxStep.put(hostName, iterNextRootTx);
		}
		AbstractMap.SimpleEntry<String, Float> entry = iterNextRootTx.next();
		assert entry.getValue().floatValue()==Engine.getDefault().getVirtualTime() 
		       ||entry.getValue().floatValue() > Engine.getDefault().getMaxVirtualTime() ; 
		return entry.getKey();
	}
	
	public Transaction getNextTransction(Component host, ArrayList<String> ancestors, String rootTxID){
		Iterator<TransactionRecord> iterTxRecord;
		if(ancestors.isEmpty()){
			assert ! replayCurrentStep.containsKey(rootTxID);
			iterTxRecord = this.txHistories.get(rootTxID).iterator();
			replayCurrentStep.put(rootTxID, iterTxRecord);
		}else{
			assert ancestors.get(0).equals(rootTxID);
			iterTxRecord = replayCurrentStep.get(rootTxID);
		}
		assert iterTxRecord.hasNext() : "Panic! history exausted!";
		return iterTxRecord.next().getTransaction(host, ancestors);
	}
}