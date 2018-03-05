package de.halfbit.tinybus.mocks;

import de.halfbit.tinybus.Produce;

public class Producer1 {

	public Event1 lastEvent = new Event1("producer1");
	
	@Produce
	public Event1 getLastEvent() {
		return lastEvent;
	}
	
}
