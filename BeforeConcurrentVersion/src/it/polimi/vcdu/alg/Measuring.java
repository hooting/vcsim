package it.polimi.vcdu.alg;

import it.polimi.vcdu.sim.SimContainer;
import it.polimi.vcdu.sim.SimEvent;

public class Measuring extends DefaultAlg {

	public Measuring(SimContainer simCon) {
		super(simCon);
		// TODO Auto-generated constructor stub
	}

	public void onFirstMeasurement(SimEvent currentEvent){
		this.collectReqSettingCallBack.callback(currentEvent, null);
	}
	
	public void onSecondMeasurement(SimEvent currentEvent){
		this.collectResultCallBack.callback(currentEvent, null);
	};
}
