package com.example.cw1trafficapp;

import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TrafficSchedulerTest {

    @Test
    public void testDeterministicHourlySchedulerExecution() throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger runCount = new AtomicInteger(0);

        // Schedule task at 50ms test interval for deterministic unit test execution
        ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(runCount::incrementAndGet, 10, 50, TimeUnit.MILLISECONDS);

        Thread.sleep(180);
        assertTrue("Scheduler should execute task multiple times on scheduled intervals", runCount.get() >= 3);

        handle.cancel(true);
        scheduler.shutdownNow();
        assertTrue("Scheduler should shut down cleanly", scheduler.isShutdown());
    }
}
