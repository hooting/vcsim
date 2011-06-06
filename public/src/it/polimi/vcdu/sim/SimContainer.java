package it.polimi.vcdu.sim;



import it.polimi.vcdu.alg.Algorithm;
import it.polimi.vcdu.model.Component;
import it.polimi.vcdu.model.IterationNode;
import it.polimi.vcdu.model.Message;
import it.polimi.vcdu.model.OutPort;
import it.polimi.vcdu.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;


public class SimContainer extends SimObject {
	private Algorithm algorithm;
	private SimNet simNet;
	private HashMap<String,SimAppTx> simAppMap =new HashMap<String,SimAppTx>() ;

	public SimContainer(Component component) {
		super(component);
	}
	
	public void injectRootTx(SimEvent currentEvent){
		currentEvent.clearReferencedEvent();
		SimAppTx simApp= new SimAppTx(this.getHostComponent(),this,new ArrayList<String>());
		simAppMap.put(simApp.getTransaction().getId(), simApp);
		simApp.onInitRootTx(currentEvent);
	}
	//interface for SimAppTx
	public void onInitRootTx(SimEvent currentEvent,SimAppTx simApp){
		
		simApp.getTransaction().getIteration().txToBlockRecordTime();
		
		Transaction tx= simApp.getTransaction();
		this.getHostComponent().addLocalTransaction(tx);

		this.algorithm.onInitRootTx(currentEvent,simApp, new CallBack (simApp){
			public void callback(SimEvent currentEvent, Object[] parameters){
				SimAppTx simApp= (SimAppTx) this.getSimObj();
				simApp.readyToInitRootTx(currentEvent, null);
			}
		});		
	}
	public void onToGoBeforeRootInitFirstSubTx(SimEvent currentEvent,SimAppTx simApp){
		
		simApp.getTransaction().getIteration().txToBlockRecordTime();
		
		this.algorithm.onToGoBeforeRootInitFirstSubTx(currentEvent,simApp,new CallBack (simApp){
			public void callback(SimEvent currentEvent, Object[] parameters){
				SimAppTx simApp= (SimAppTx) this.getSimObj();
				simApp.readyToGoBeforeRootInitFirstSubTx(currentEvent, null);
			}
		});
		
	}
	
	/**
	 * A sub tx is starting
	 */
	public void onStartSubTx(SimEvent currentEvent, SimAppTx simApp){
		
		simApp.getTransaction().getIteration().txToBlockRecordTime();
		
		Transaction tx= simApp.getTransaction();
		this.getHostComponent().addLocalTransaction(tx);
		// Give a chance for algorithm to do its staff
		
		this.algorithm.onStartSubTx(currentEvent,simApp,tx,new CallBack (simApp){
			public void callback(SimEvent currentEvent, Object[] parameters){
				SimAppTx simApp= (SimAppTx) this.getSimObj();
				simApp.onNextStep(currentEvent);
				}
		});
	}

	
	public void onInitSubTx(SimEvent currentEvent, SimAppTx simApp) {
		
		simApp.getTransaction().getIteration().txToBlockRecordTime();
		
		Transaction parentTx = simApp.getTransaction();
		
		IterationNode currentStep = parentTx.getIteration().getCurrentStep();
		
		assert currentStep.getOutPort()!=null;
		
		OutPort outPort= currentStep.getOutPort();
		Message message= new Message("onBeingInitSubTx",outPort,outPort.getPeerPort(),parentTx.getAncestors());
		//send to simNet
		this.simNet.onSend(currentEvent, message);	
		simApp.readyToInitSubTx(currentEvent);
	}
	
	/**
	 * called by simApp (through a scheduled event) when a transaction ends.
	 * @param currentEvent
	 * @param simApp
	 */
	public void onEndingTx(SimEvent currentEvent, SimAppTx simApp) {
		
		simApp.getTransaction().getIteration().txToBlockRecordTime();
		
		Transaction transaction = simApp.getTransaction();
		this.getHostComponent().removeFromLocalTransactions(transaction);
		if (transaction.isRoot()){
			this.algorithm.onEndingRootTx(currentEvent,simApp,new CallBack(simApp){
				public void callback(SimEvent currentEvent, Object[] parameters){
					SimAppTx simApp= (SimAppTx) this.getSimObj();
					simApp.readyToEndTx(currentEvent, null);
					
				}
			});
		}
		else{// send back to the parent
			OutPort outPort= (OutPort)simApp.getInitiatingMessage().getSource();
			Message message= new Message("onBeingEndingTx",outPort.getPeerPort(),outPort,transaction.getAncestors());
			this.simNet.onSend(currentEvent, message);
								
			this.algorithm.onEndingSubTx(currentEvent,simApp,new CallBack(simApp){
				public void callback(SimEvent currentEvent, Object[] parameters){
					SimAppTx simApp= (SimAppTx) this.getSimObj();
					simApp.readyToEndTx(currentEvent, null);
					
				}
			});
		
		}
		
		
	}	
	
	
	/*
	 * Methods from SimNet
	 */
	@SuppressWarnings("unchecked")
	public void onBeingInitSubTx(SimEvent currentEvent,Message message){
		ArrayList <String> ancestors=(ArrayList <String>) message.getContent();
		SimAppTx simApp= new SimAppTx(this.getHostComponent(),this,ancestors);
		simApp.setInitiatingMessage(message);
		simAppMap.put(simApp.getTransaction().getId(), simApp);	
		//first notify the algorithm
		this.algorithm.onBeingInitSubTx(currentEvent,simApp,new CallBack (simApp){
			public void callback(SimEvent currentEvent, Object[] parameters){
				SimAppTx simApp= (SimAppTx) this.getSimObj();
				//before let the SimApp initiating the transaction
				//simApp.onNextStep(currentEvent);
				simApp.onStartSubTx(currentEvent);
			}
		});		
	}
	
	/**
	 * called when a notification of the ending one sub-transaction of a locally hosted transaction.
	 * @param currentEvent
	 * @param message the notification message from the host of the sub-transaction
	 */
	@SuppressWarnings("unchecked")
	public void onBeingEndingTx(SimEvent currentEvent,Message message){
		ArrayList <String> ancestors=(ArrayList <String>) message.getContent();
		//the parentID is the last in the ancestors' list
		String parentId= ancestors.get(ancestors.size()-2);
		SimAppTx parentSimApp= this.simAppMap.get(parentId);
		this.algorithm.onBeingEndingTx(currentEvent,parentSimApp,(OutPort)message.getDestination(),new CallBack(parentSimApp){
			public void callback(SimEvent currentEvent, Object[] parameters){
				SimAppTx simApp= (SimAppTx) this.getSimObj();
				//TODO: simApp.getTransaction().getId() is not correct, should be ?? or change SimAppTx.onSubTxEnd
				simApp.onSubTxEnd(currentEvent,simApp.getTransaction().getId(),null);
			}
		});
	}
	
	/*
	 * Methods to the Algorithm
	 */
	public void dispatchToAlg(SimEvent currentEvent, Message message){
		Object [] content= (Object[])message.getContent();		
		String methodName= (String)content[0];
		content[0]=currentEvent;
		/*Object [] parameters= new Object[content.length-1];
		for (int i=1; i<content.length;i++){
			parameters[i-1]=content[i];
		}	*/	
		//SimEvent.runMethod(methodName, this.algorithm, parameters);
		SimEvent.runMethod(methodName, this.algorithm, content);
	}

	//getter and setters
	
	/**
	 * @param algorithm the algorithm to set
	 */
	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}
	/**
	 * @return the algorithm
	 */
	public Algorithm getAlgorithm() {
		return algorithm;
	}
	
	/**
	 * @param simNet the simNet to set
	 */
	public void setSimNet(SimNet simNet) {
		this.simNet = simNet;
	}

	/**
	 * @return the simNet
	 */
	public SimNet getSimNet() {
		return simNet;
	}


}
