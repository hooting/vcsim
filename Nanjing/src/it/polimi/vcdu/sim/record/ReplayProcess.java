package it.polimi.vcdu.sim.record;

import it.unipr.ce.dsg.deus.core.Engine;
import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.InvalidParamsException;
import it.unipr.ce.dsg.deus.core.Process;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;

public class ReplayProcess extends Process {

	ArrayList<SimpleEntry<String, Float>> list;
	Iterator<SimpleEntry<String, Float>> iterator;

	public ReplayProcess(String id,
			ArrayList<SimpleEntry<String, Float>> list,
			ArrayList<Event> refEvents) throws InvalidParamsException {
		super(id, null, null, refEvents);
		this.list = list;
		this.iterator = list.iterator();
	}

	@Override
	public float getNextTriggeringTime(Event event, float virtualTime) {
		if(!iterator.hasNext()) return Engine.getDefault().getMaxVirtualTime()+1;
		SimpleEntry<String, Float> entry = iterator.next();
		return entry.getValue();
	}

	@Override
	public void initialize() throws InvalidParamsException {
		this.iterator = list.iterator();

	}

}
