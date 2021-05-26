package com.newrelic.telemetry;

import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.EventsSinkTask;
import com.newrelic.telemetry.logs.LogBatch;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.TelemetryMetricsSinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;


public class TelemetryBatchRunner<T extends Telemetry> implements Runnable {

    private static Logger log = LoggerFactory.getLogger(TelemetryBatchRunner.class);

    private final TelemetryClient client;

    private final BiFunction<Collection<T>, Attributes, TelemetryBatch<T>> createBatch;

    private final BlockingQueue<T> queue;

    private final int numElements;

    private final long timeout;

    private final TimeUnit unit;

    public TelemetryBatchRunner(TelemetryClient client, BiFunction<Collection<T>, Attributes, TelemetryBatch<T>>createBatch, BlockingQueue<T> queue, int numElements, long timeout, TimeUnit unit) {

        this.client = client;
        this.createBatch = createBatch;
        this.queue = queue;
        this.numElements = numElements;
        this.timeout = timeout;
        this.unit = unit;
    }

    /* instead of introducing another dependency for a single utility method,
     * copypasta: https://github.com/google/guava/blob/10f1853c58b8cd4fc34938888b78817591120372/guava/src/com/google/common/collect/Queues.java#L293
     */
    public static <E> int drain(
            BlockingQueue<E> q,
            Collection<? super E> buffer,
            int numElements,
            long timeout,
            TimeUnit unit)
            throws InterruptedException {

        /*
         * This code performs one System.nanoTime() more than necessary, and in return, the time to
         * execute Queue#drainTo is not added *on top* of waiting for the timeout (which could make
         * the timeout arbitrarily inaccurate, given a queue that is slow to drain).
         */
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        int added = 0;
        while (added < numElements) {
            // we could rely solely on #poll, but #drainTo might be more efficient when there are multiple
            // elements already available (e.g. LinkedBlockingQueue#drainTo locks only once)
            added += q.drainTo(buffer, numElements - added);
            if (added < numElements) { // not enough elements immediately available; will have to poll
                E e = q.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (e == null) {
                    break; // we already waited enough, and there are no more elements in sight
                }
                buffer.add(e);
                added++;
            }
        }
        return added;
    }


    @Override
    public void run() {
        while (true) {
            List<T> buffer = new ArrayList<>();
            try {
                int added = drain(this.queue, buffer, this.numElements, this.timeout, this.unit);
            } catch (InterruptedException e) {
                log.info("Caught interruption.  Sending final batch.");
                break;
            } finally {
                if (buffer.isEmpty()) {
                    log.info("Empty batch.  Doing nothing");
                } else {
                    TelemetryBatch<T> batch = createBatch.apply(buffer, new Attributes());
                    // No polymorphic implementation of sendBatch, only multiple dispatch.
                    if (batch instanceof MetricBatch) {
                        log.info(String.format("Sending batch of %s metrics", buffer.size()));
                        client.sendBatch((MetricBatch) batch);
                    } else if (batch instanceof LogBatch) {
                        log.info(String.format("Sending batch of %s logs", buffer.size()));
                        client.sendBatch((LogBatch) batch);
                    } else if (batch instanceof EventBatch) {
                        log.info(String.format("Sending batch of %s events", buffer.size()));
                        client.sendBatch((EventBatch) batch);
                    }
                }
            }

        }
    }
}

