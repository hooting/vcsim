/**
 * 
 */
package it.polimi.vcdu.sim;

import it.unipr.ce.dsg.deus.core.Event;
import it.unipr.ce.dsg.deus.core.SchedulerListener;

public class MySchedulerListener implements SchedulerListener{

	@Override
	public void newEventScheduled(Event parentEvent, Event newEvent) {
	//	System.out.println("ref Event executed:" + newEvent.getId());
	}
}