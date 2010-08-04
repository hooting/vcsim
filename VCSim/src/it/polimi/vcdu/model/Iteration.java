/**
 * 
 */
package it.polimi.vcdu.model;

import it.polimi.vcdu.util.RandUtils;
import it.unipr.ce.dsg.deus.core.Engine;

import java.util.ArrayList;


/**
 * @author Valerio Panzica La Manna (panzica@elet.polimi.it)
 *
 */
public class Iteration {
	private ArrayList <IterationNode> list;
	private int current_step;
	private float localProcessingTime;
	
	public Iteration(){
		current_step=0;
		list= new ArrayList<IterationNode>();
	}
	/**
	 * @param localProcessingTime
	 * @param size
	 */
	public Iteration(float localProcessingTime,Component host) {
		current_step=0;
		this.localProcessingTime = localProcessingTime;
		list=genRandIteration(localProcessingTime,host.getOutPorts().size(), host);
	}
	/**
	 * @param localProcessingTime
	 * @param nOutPorts
	 * @param nSubTx
	 * @return
	 */
	private ArrayList<IterationNode> genRandIteration(float localProcessingTime, int nOutPorts,Component host) {
		ArrayList<IterationNode> randList = new ArrayList<IterationNode>();
		float delay = 0;
		OutPort selectedOp = null;
		float totalDelay = 0;
		do{
			IterationNode node=new IterationNode(delay,selectedOp);
			randList.add(node);
			if(nOutPorts == 0) break;
			
			delay=RandUtils.RandDelay()/nOutPorts;
			totalDelay+=delay;
			int outPortIndex= RandUtils.selectPort(nOutPorts);
			selectedOp =(OutPort) host.getOutPorts().get(outPortIndex);
		} while (totalDelay<localProcessingTime);

		randList.add(new IterationNode(localProcessingTime+delay-totalDelay,null));
		return randList;
	}
	public void add(float delay, OutPort outPort){
		IterationNode n= new IterationNode(delay,outPort);
		list.add(n);		
	}
	
	public void nextStep(){
		assert current_step<list.size()-1;
//		if( current_step >= list.size()){
//			assert false;
//		}
		current_step++;
	}	
	public IterationNode getCurrentStep(){
		//debugging
/*		if(Engine.getDefault().getVirtualTime()>=10180){
			System.out.println("debug");
		}
		//
*/		return list.get(current_step);
	}
	public boolean isEnd()
	{
		assert current_step<list.size();
		return list.size() - 1 ==current_step;
	}
	/**
	 * @return the list
	 */
	public ArrayList<IterationNode> getList() {
		return list;
	}
	/**
	 * @return the current_step
	 */
	public int getCurrent_step() {
		return current_step;
	}
	/**
	 * @return the localProcessingTime
	 */
	public float getLocalProcessingTime() {
		return localProcessingTime;
	}
	
	@Override
	public String toString(){
		return "<current_step: "+this.current_step+", "+this.list +">";
	}
	
	
	/**
	 * Following are for working time/blocked time statistics
	 */
	private float txCreateTime =-1.0f;
	private float txEndTime = -1.0f;
	
	private float workingTime = 0f;
	private float blockedTime = 0f;
	
	private boolean isWorking = false; // counting workingTime when true; blockedTime when false;
	private float lastSwitchTime = -1.0f;
	
	// Invariant: workingTime+blockedTime == currentVT - txCreateTime; 
	// when tx ended, workingTime == localProcessingTime
	
	public void txCreatingRecordTime(){
		this.setTxCreateTime(Engine.getDefault().getVirtualTime());
		this.isWorking = true;
		this.lastSwitchTime = Engine.getDefault().getVirtualTime();
	}
	
	public void txEnddingRecondTime(){
		this.setTxEndTime(Engine.getDefault().getVirtualTime());
		this.isWorking = false;
		assert (localProcessingTime-0.1 < workingTime) && (localProcessingTime+0.1 > workingTime) : " Working Time Statistic Error!";
		assert Math.abs(workingTime+blockedTime+txCreateTime - txEndTime)<0.1 : " Working Time Statistic Error!";
	}
	
	public float getCurrentWorkingTime(){
		assert this.txCreateTime >= 0;
		float wt = this.workingTime;
		if (this.isWorking) wt += Engine.getDefault().getVirtualTime() - this.lastSwitchTime;
		return wt;
	}
	
	public float getCurrentBlockedTime(){
		assert this.txCreateTime >= 0;
		float bt = this.blockedTime;
		if (! this.isWorking) bt += Engine.getDefault().getVirtualTime() - this.lastSwitchTime;
		return bt;
	}
	
	/**
	 * called when tx is just to be blocked
	 */
	public void txToBlockRecordTime(){
		assert this.isWorking;
		this.isWorking = false;
		float currentTime = Engine.getDefault().getVirtualTime();
		workingTime += currentTime - lastSwitchTime;
		lastSwitchTime =  currentTime;
	}
	public void txToWorkingRecordTime(){
		boolean doingSub =  (0< current_step) && (current_step<this.list.size()-1);
		assert !this.isWorking || doingSub;
		if (this.isWorking) return;
		this.isWorking = true ;
		float currentTime = Engine.getDefault().getVirtualTime();
		blockedTime += currentTime - lastSwitchTime;
		lastSwitchTime =  currentTime;
	}

	/**
	 * @return the txCreateTime
	 */
	public float getTxCreateTime() {
		return txCreateTime;
	}
	/**
	 * @param txCreateTime the txCreateTime to set
	 */
	private void setTxCreateTime(float txCreateTime) {
		assert this.txCreateTime < 0f : "Tx Create time already set!";
		assert txCreateTime >= 0: "Tx Create time should not be negative!";
		this.txCreateTime = txCreateTime;
	}
	/**
	 * @return the txEndTime
	 */
	public float getTxEndTime() {
		return txEndTime;
	}
	/**
	 * @param txEndTime the txEndTime to set
	 */
	private void setTxEndTime(float txEndTime) {
		assert this.txEndTime <0f : "Tx End time already set!";
		assert txEndTime >0f && txEndTime>this.txCreateTime : "Tx End time must be greater than 0 and tx create time.";
		this.txEndTime = txEndTime;
	}
	/**
	 * @return the workingTime
	 */
	public float getWorkingTime() {
		return workingTime;
	}
	/**
	 * @return the blockedTime
	 */
	public float getBlockedTime() {
		return blockedTime;
	}
	

}
