package de.halfbit.tinybus.impl;

import junit.framework.TestCase;
import de.halfbit.tinybus.Subscribe;
import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.mocks.Producer1;
import de.halfbit.tinybus.mocks.Subscriber1;

public class FailFastTest extends TestCase {
	
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
	
	public void testRegisterSameSubscriberTwice() {
		try {
			bus.register(subscriber);
			bus.register(subscriber);
			fail("exception is expected");
		} catch (Exception e) {
			// OK
		}
	}
	
	public void testUnregisterNotRegisteredSubscriber() {
		try {
			bus.unregister(subscriber);
			fail("exception is expected");
		} catch (Exception e) {
			// OK
		}
	}
	
	public void testUnregisterSameSubscriberTwice() {
		try {
			bus.register(subscriber);
			bus.unregister(subscriber);
			bus.unregister(subscriber);
			fail("exception is expected");
		} catch (Exception e) {
			// OK
		}
	}
	
	public void testRegisterSameProducerTwice() {
		try {
			bus.register(producer);
			bus.register(producer);
			fail("exception is expected");
		} catch (Exception e) {
			// OK
		}
	}
	
	public void testUnregisterNotRegisteredProducer() {
		try {
			bus.unregister(producer);
			fail("exception is expected");
		} catch (Exception e) {
			// OK
		}
	}
	
	public void testUnregisterSameProducerTwice() {
		try {
			bus.register(producer);
			bus.unregister(producer);
			bus.unregister(producer);
			fail("exception is expected");
		} catch (Exception e) {
			// OK
		}
	}
	
	public void testRegisterNull() {
		try {
			bus.register(null);
			fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}
	}
	
	public void testUnregisterNull() {
		try {
			bus.unregister(null);
			fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}
	}
	
	public void testPostNull() {
		try {
			bus.post(null);
			fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}
	}	

	public void testSubscriberThrowsException() {
		bus.register(new Object() {
			@Subscribe
			public void onEvent(String event) {
				throw new IllegalArgumentException(event);
			}
		});
		
		try {
			bus.post("event");
			fail("exception is expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

}
