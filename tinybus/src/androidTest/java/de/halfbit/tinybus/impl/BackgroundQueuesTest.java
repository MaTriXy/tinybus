package de.halfbit.tinybus.impl;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;
import de.halfbit.tinybus.Subscribe;
import de.halfbit.tinybus.Subscribe.Mode;
import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.impl.TinyBusDepot;
import de.halfbit.tinybus.mocks.Callbacks;

public class BackgroundQueuesTest extends InstrumentationTestCase {

	private static final int TEST_TIMEOUT = 4;
	
	ArrayList<CallbackResult> results;
	
	static class CallbackResult {
		public Object event;
		public String threadName;
	}
	
	synchronized void collectEvent(String event) {
		CallbackResult result = new CallbackResult();
		result.event = event;
		result.threadName = Thread.currentThread().getName();
		results.add(result);
	}
	
	void assertEventsNumber(int number) {
		assertEquals(number, results.size());
	}
	
	void assertResult(int index, Object event, String threadName) {
		CallbackResult result = results.get(index);
		assertEquals(event, result.event);
		assertNotNull(result.threadName);
		assertTrue(result.threadName.startsWith(threadName));
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		results = new ArrayList<CallbackResult>();
	}
	
	
	@Override
	protected void tearDown() throws Exception {
		// destroy cached bus, that we can create buses in different threads
		Context context = getInstrumentation().getContext(); 
		TinyBusDepot depot = TinyBusDepot.get(context);
		depot.onContextStopped(context);
		depot.onContextDestroyed(context);
		
		super.tearDown();
	}
	
	@UiThreadTest
	public void testSerialQueueExecution() throws Exception {
		
		final int numberOfEvents = 25;
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfEvents);

		Callbacks callbacks = new Callbacks() {
			
			@Subscribe(mode=Mode.Background, queue="test")
			public void onEvent(String event) {
				onCallback(event);
				latch.countDown();
				
				long timeout = (long) (2l * Math.random());
				if (timeout > 0l) {
					SystemClock.sleep(timeout);
				}
			}
		};
		
		bus.register(callbacks);
		
		ArrayList<Object> expected = new ArrayList<Object>();
		for(int i=0; i<numberOfEvents; i++) {
			String event = "event" + i; 
			bus.post(event);
			expected.add(event);
		}
		
		latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
		
		callbacks.assertSameEventsList(expected);
	}
	
	public void testNoneBlockingQueues() throws Exception {
		
		final Object lock = new Object();
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		
		final CountDownLatch latch = new CountDownLatch(200);
		final CountDownLatch blockingLatch = new CountDownLatch(100);

		final Callbacks fluentCallback = new Callbacks() {
			@Subscribe(mode=Mode.Background, queue="fluent")
			public void onEvent(String event) {
				onCallback(event);
				synchronized (lock) {
					latch.countDown();
				}
				blockingLatch.countDown();
			}
		};

		Callbacks blockingCallback = new Callbacks() {
			
			@Subscribe(mode=Mode.Background, queue="block")
			public void onEvent(String event) throws InterruptedException {
				
				// block on first event
				if (getEventsCount() == 0) {
					blockingLatch.await(3, TimeUnit.SECONDS);
				}

				// process, if fluent callback is completed only 
				if (fluentCallback.getEventsCount() == 100) {
					onCallback(event);
					synchronized (lock) {
						latch.countDown();
					}
				}
			}
		};
		
		bus.register(blockingCallback);
		bus.register(fluentCallback);
		
		ArrayList<Object> expected = new ArrayList<Object>();
		for(int i=0; i<100; i++) {
			String event = "event" + i; 
			bus.post(event);
			expected.add(event);
		}
		
		latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
		
		fluentCallback.assertSameEventsList(expected);
		blockingCallback.assertSameEventsList(expected);
		
	}
	
	@UiThreadTest
	public void testPostSingleEventToSingleQueue() throws Exception {
		
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(1);
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background)
			public void onEvent(String event) {
				collectEvent(event);
				latch.countDown();
			}
		});
		
		bus.post("event a");
		latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
		
		assertEventsNumber(1);
		assertResult(0, "event a", "tinybus-worker-");
	}
	
	@UiThreadTest
	public void testPostSingleEventToMultipleQueues() throws Exception {
	
		final int numberOfQueues = 6;
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfQueues);
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue0")
			public void onEvent(String event) {
				collectEvent(event + "0");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				collectEvent(event + "1");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue2")
			public void onEvent(String event) {
				collectEvent(event + "2");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue3")
			public void onEvent(String event) {
				collectEvent(event + "3");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue4")
			public void onEvent(String event) {
				collectEvent(event + "4");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue5")
			public void onEvent(String event) {
				collectEvent(event + "5");
				latch.countDown();
			}
		});
		
		bus.post("event");
		latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
		
		ArrayList<String> eventsReduceList = new ArrayList<String>();
		for(int i=0; i<numberOfQueues; i++) {
			eventsReduceList.add("event" + i);
		}
		
		assertEventsNumber(numberOfQueues);
		for(int i=0; i<numberOfQueues; i++) {
			eventsReduceList.remove(results.get(i).event);
		}
		assertEquals(0, eventsReduceList.size());
	}
	
	@UiThreadTest
	public void testPostSingleEventToSameQueueInTwoReceivers() throws Exception {
		
		final int numberOfEvents = 2;
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfEvents);
		
		Callbacks callbacks1, callbacks2;
		
		bus.register(callbacks1 = new Callbacks() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				onCallback(event + "0");
				latch.countDown();
			}
		});
		
		bus.register(callbacks2 = new Callbacks() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				onCallback(event + "1");
				latch.countDown();
			}
		});
		
		bus.post("event");
		latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

		callbacks1.assertEqualEvents("event0");
		callbacks2.assertEqualEvents("event1");
	}
	
	@UiThreadTest
	public void testPostMultipleEventsToSingleQueue() throws Exception {
		
		int numberOfEvents = 100;
		
		TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfEvents);
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background)
			public void onEvent(String event) {
				collectEvent(event);
				latch.countDown();
			}
		});
		
		for(int i=0; i<numberOfEvents; i++) {
			bus.post("event_" + i);
		}
		latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
		
		assertEventsNumber(numberOfEvents);
		for(int i=0; i<numberOfEvents; i++) {
			assertResult(i, "event_" + i, "tinybus-worker-0");
		}
	}

	@UiThreadTest
	public void testPostMultipleEventsToMultipleQueues() throws Exception {
	
		final int numberOfQueues = 5;
		final int numberOfEvents = 20;
		
		final Object lock = new Object();
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfQueues * numberOfEvents);
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue0")
			public void onEvent(String event) {
				collectEvent(event + "0");
				synchronized (lock) {
					latch.countDown();
				}
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				collectEvent(event + "1");
				synchronized (lock) {
					latch.countDown();
				}
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue2")
			public void onEvent(String event) {
				collectEvent(event + "2");
				synchronized (lock) {
					latch.countDown();
				}
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue3")
			public void onEvent(String event) {
				collectEvent(event + "3");
				synchronized (lock) {
					latch.countDown();
				}
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue4")
			public void onEvent(String event) {
				collectEvent(event + "4");
				synchronized (lock) {
					latch.countDown();
				}
			}
		});
		
		for (int i=0; i<numberOfEvents; i++) {
			bus.post("event" + i);
		}
		latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
		
		assertEventsNumber(numberOfQueues * numberOfEvents);
		ArrayList<String> eventsReduceList = new ArrayList<String>();
		for(int i=0; i<numberOfEvents; i++) {
			eventsReduceList.add("event" + i + "0");
			eventsReduceList.add("event" + i + "1");
			eventsReduceList.add("event" + i + "2");
			eventsReduceList.add("event" + i + "3");
			eventsReduceList.add("event" + i + "4");
		}
		
		assertEventsNumber(numberOfEvents * numberOfQueues);
		for(int i=0; i<numberOfEvents * numberOfQueues; i++) {
			eventsReduceList.remove(results.get(i).event);
		}
		assertEquals(0, eventsReduceList.size());
	}
	
}
