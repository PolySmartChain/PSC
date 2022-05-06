package org.tdf.common.event;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@RunWith(JUnit4.class)
public class EventBusTests {
    protected EventBus bus;

    @Before
    public void init() {
        bus = new EventBus(new ThreadFactoryBuilder().setNameFormat("event-bus-%d").build());
    }

    @Test
    public void testPublishEvent() throws Exception {
        SuccessEvent event = new SuccessEvent();
        SuccessCounter counter = new SuccessCounter();
        FailedCounter failedCounter = new FailedCounter();
        bus.subscribe(SuccessEvent.class, counter);
        bus.subscribe(FailedEvent.class, failedCounter);
        bus.publish(event);
        bus.publish(event);
        Thread.sleep(1000);
        assert counter.getCounter() == 2;
        assert failedCounter.counter == 0;
    }

    @Test
    public void testPublishEvent2() {
        FailedEvent event = new FailedEvent();
        SuccessCounter counter = new SuccessCounter();
        bus.subscribe(SuccessEvent.class, counter);
        bus.publish(event);
        bus.publish(event);
        assert counter.getCounter() == 0;
    }

    static class SuccessEvent {
    }

    static class FailedEvent {
    }

    static class SuccessCounter implements Consumer<SuccessEvent> {
        private AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void accept(SuccessEvent successEvent) {
            this.counter.getAndIncrement();
        }

        public int getCounter() {
            return counter.get();
        }
    }

    static class FailedCounter implements Consumer<FailedEvent> {
        private int counter;

        @Override
        public void accept(FailedEvent successEvent) {
            this.counter++;
        }

        public int getCounter() {
            return counter;
        }
    }
}
