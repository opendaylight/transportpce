/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;

class EventSubscriberTest {

    @Test
    void last() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First");
        subscriber.event(Level.ERROR, "Last");

        assertEquals("Last", subscriber.last(Level.ERROR));
    }

    @Test
    void empty() {
        Subscriber subscriber = new EventSubscriber();

        assertEquals("", subscriber.last(Level.ERROR));
    }

    @Test
    void lastDefault() {
        Subscriber subscriber = new EventSubscriber();

        assertEquals("Error", subscriber.last(Level.ERROR, "Error"));
    }

    @Test
    void firstNumber() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.WARN, "First warn");
        subscriber.event(Level.ERROR, "Second error");
        subscriber.event(Level.ERROR, "Third error");


        assertEquals("First error, Second error, Third error", subscriber.first(Level.ERROR, "", 3));
    }

    @Test
    void firstNumberContainsLessThanThree() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.WARN, "First warn");
        subscriber.event(Level.ERROR, "Second error");


        assertEquals("First error, Second error", subscriber.first(Level.ERROR, "", 3));
    }

    @Test
    void firstNumberEmpty() {
        Subscriber subscriber = new EventSubscriber();

        assertEquals("Asdf", subscriber.first(Level.ERROR, "Asdf", 3));
    }

    @Test
    void firstNumberZero() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.ERROR, "Second error");

        assertEquals("First error", subscriber.first(Level.ERROR, "", 0));
    }

    @Test
    void firstNumberNegative() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.ERROR, "Second error");

        assertEquals("First error", subscriber.first(Level.ERROR, "", -1));
    }

    @Test
    void repetitiveMessages() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.ERROR, "Second error");
        subscriber.event(Level.ERROR, "First error");

        assertEquals("First error, Second error", subscriber.first(Level.ERROR, "Error", 3));
    }

    private static Stream<Arguments> writerAndLevel() {
        return Stream.of(
            Arguments.of((BiConsumer<Subscriber, String>) (subscriber, message) -> subscriber.error(message),
                Level.ERROR),
            Arguments.of((BiConsumer<Subscriber, String>) (subscriber, message) -> subscriber.warn(message),
                Level.WARN)
        );
    }

    //Stress test guarding the synchronization contract documented on EventSubscriber: a CyclicBarrier releases
    //every thread at once so they all race to add to the same level, repeated many times to make a regression
    //fail reliably instead of flakily.
    //
    //Probabilistic, not deterministic - but reliable enough in practice: the check-then-act/unsynchronized-set
    //implementation that predates commit fe3a960ca fails this test on the first iteration at these parameters.
    @ParameterizedTest
    @MethodSource("writerAndLevel")
    void concurrentEventsDoNotLoseMessages(BiConsumer<Subscriber, String> writer, Level level)
            throws InterruptedException {
        int iterations = 20;
        int threadCount = 32;
        int messagesPerThread = 50;

        for (int iteration = 0; iteration < iterations; iteration++) {
            Subscriber subscriber = new EventSubscriber();
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            Thread[] threads = new Thread[threadCount];

            for (int t = 0; t < threadCount; t++) {
                int threadIndex = t;
                threads[t] = new Thread(() -> {
                    try {
                        barrier.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (BrokenBarrierException e) {
                        return;
                    }
                    for (int m = 0; m < messagesPerThread; m++) {
                        writer.accept(subscriber, "t" + threadIndex + "-m" + m);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount * messagesPerThread, subscriber.messages(level).length,
                "Lost messages on iteration " + iteration);
        }
    }
}
