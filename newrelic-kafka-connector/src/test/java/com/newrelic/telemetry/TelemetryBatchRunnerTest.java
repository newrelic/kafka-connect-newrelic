package com.newrelic.telemetry;

import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.EventConverter;
import com.newrelic.telemetry.events.Fixtures;
import com.newrelic.telemetry.metrics.MetricBatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TelemetryBatchRunnerTest {

    TelemetryClient client;

    BlockingQueue<Event> queue;

    ExecutorService batchRunnerExecutor;

    Fixtures eventFixtures = new Fixtures();

    TelemetryBatch<Event> createBatch(Collection<Event> buffer, Attributes attributes) {
        return new EventBatch(buffer, attributes);
    }

    @Before
    public void setUp() {
        this.client = Mockito.mock(TelemetryClient.class);
        LinkedBlockingQueue<Event> q = new LinkedBlockingQueue<>();
        this.queue = Mockito.spy(q);
        this.batchRunnerExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        this.batchRunnerExecutor.shutdownNow();
    }

    @Test
    public void noEvents() throws Exception {
        TelemetryBatchRunner<Event> t = new TelemetryBatchRunner<>(this.client, this::createBatch, this.queue, 10, 100, TimeUnit.MILLISECONDS);
        this.batchRunnerExecutor.execute(t);
        Thread.sleep(150);
        verify(this.client, never()).sendBatch(any(MetricBatch.class));
    }

    @Test
    public void timeOutBatch() throws Exception {
        TelemetryBatchRunner<Event> t = new TelemetryBatchRunner<>(this.client, this::createBatch, this.queue, 10, 300, TimeUnit.MILLISECONDS);
        this.batchRunnerExecutor.execute(t);
        for (int i = 0; i < 9; i++) {
            this.queue.add(EventConverter.toNewRelicEvent(eventFixtures.sampleStructRecord));
        }
        Thread.sleep(500);
        verify(this.client, times(1)).sendBatch(any(EventBatch.class));
        ArgumentCaptor<EventBatch> argument = ArgumentCaptor.forClass(EventBatch.class);
        verify(this.client).sendBatch(argument.capture());
        assertEquals(9, argument.getValue().size());
        assertTrue(this.queue.isEmpty());
    }

    @Test
    public void limitBatch() throws Exception {
        TelemetryBatchRunner<Event> t = new TelemetryBatchRunner<>(this.client, this::createBatch, this.queue, 10, 300, TimeUnit.MILLISECONDS);
        this.batchRunnerExecutor.execute(t);
        for (int i = 0; i < 10; i++) {
            this.queue.add(EventConverter.toNewRelicEvent(eventFixtures.sampleStructRecord));
        }
        Thread.sleep(100);
        verify(this.client, times(1)).sendBatch(any(EventBatch.class));
        ArgumentCaptor<EventBatch> argument = ArgumentCaptor.forClass(EventBatch.class);
        verify(this.client).sendBatch(argument.capture());
        assertEquals(10, argument.getValue().size());
        assertTrue(this.queue.isEmpty());
    }

    @Test
    public void limitAndTimeoutBatch() throws Exception {
        TelemetryBatchRunner<Event> t = new TelemetryBatchRunner<>(this.client, this::createBatch, this.queue, 10, 300, TimeUnit.MILLISECONDS);
        this.batchRunnerExecutor.execute(t);
        for (int i = 0; i < 15; i++) {
            this.queue.add(EventConverter.toNewRelicEvent(eventFixtures.sampleStructRecord));
        }
        Thread.sleep(500);
        verify(this.client, times(2)).sendBatch(any(EventBatch.class));
        ArgumentCaptor<EventBatch> argument = ArgumentCaptor.forClass(EventBatch.class);
        verify(this.client, times(2)).sendBatch(argument.capture());
        assertEquals(10, argument.getAllValues().get(0).size());
        assertEquals(5, argument.getAllValues().get(1).size());
        assertTrue(this.queue.isEmpty());
    }

}