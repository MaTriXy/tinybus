package de.halfbit.tinybus.impl;

import java.util.ArrayList;

import junit.framework.TestCase;
import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.mocks.Event1;
import de.halfbit.tinybus.mocks.Producer1;
import de.halfbit.tinybus.mocks.Subscriber1;

public class OneSubscriberOneProducerOneEventTest extends TestCase {

	private TinyBus bus;
	private Subscriber1 subscriber;
	private Producer1 producer;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus();
		subscriber = new Subscriber1();
		producer = new Producer1();
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
		subscriber = null;
		producer = null;
		super.tearDown();
	}
	
	public void testSingleSubscriberOneEvent() {
		Event1 event;
		bus.register(subscriber);
		
		bus.post(event = new Event1("event1"));
		subscriber.assertSameEvents(event);
	}
	
	public void testSingleSubscriberManyEvents() {
		
		Event1 event;
		bus.register(subscriber);

		ArrayList<Object> events = new ArrayList<Object>(); 
		for (int i=0; i<10; i++) {
			bus.post(event = new Event1("event1"));
			events.add(event);
		}
		subscriber.assertSameEventsList(events);
	}
	
	public void testUnregisterSubscriber() {
		bus.register(subscriber);
		bus.post(new Event1("event1"));
		bus.unregister(subscriber);
		subscriber.clearEvents();
		
		bus.post(new Event1("event1"));
		subscriber.assertNoEvents();
	}
	
	public void testSubscriberFirst() {
		bus.register(subscriber);
		bus.register(producer);
		subscriber.assertSameEvents(producer.lastEvent);
	}
	
	public void testProducerFirst() {
		bus.register(producer);
		bus.register(subscriber);
		subscriber.assertSameEvents(producer.lastEvent);
	}
	
	public void testUnregisterProducer() {
		bus.register(producer);
		bus.unregister(producer);
		bus.register(subscriber);
		subscriber.assertNoEvents();
	}
	
	public void testSubscriberFirstWithNullEvent() {
		bus.register(subscriber);
		producer.lastEvent = null;
		bus.register(producer);
		subscriber.assertNoEvents();
	}

	public void testProducerFirstWithNullEvent() {
		producer.lastEvent = null;
		bus.register(producer);
		bus.register(subscriber);
		subscriber.assertNoEvents();
	}
	
	public void testPostEventWithProducer() {
		
		//System.out.println("Current: " + Thread.currentThread());
		//System.out.println("Looper: " + Looper.getMainLooper().getThread());
		
		bus.register(producer);
		bus.register(subscriber);
		
		ArrayList<Object> events = new ArrayList<Object>();
		events.add(producer.lastEvent);
		
		Event1 event;
		bus.post(event = new Event1("second"));
		events.add(event);
		
		subscriber.assertSameEventsList(events);
	}
	
}
