package it.polimi.vcdu.sim;

public abstract class CallBack {
	private SimObject simObj;
	public CallBack(SimObject simObj){
		this.simObj=simObj;
	}
	public abstract void callback(SimEvent currentEvent, Object[] parameters);
	
	public SimObject getSimObj(){
		return simObj;
	}
}
