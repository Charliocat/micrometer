/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.tck;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.micrometer.core.MockClock.clock;
import static org.junit.jupiter.api.Assertions.*;

interface TimerTest {

    @DisplayName("record throwables")
    @Test
    default void recordThrowable() {
        MeterRegistry registry = new SimpleMeterRegistry();

        Supplier<String> timed = () -> registry.timer("timer").record(() -> "");
        timed.get();
    }

    @Test
    @DisplayName("total time and count are preserved for a single timing")
    default void record(MeterRegistry registry) {
        Timer t = registry.timer("myTimer");
        t.record(42, TimeUnit.MILLISECONDS);
        clock(registry).addAndGet(1, TimeUnit.SECONDS);

        assertAll(() -> assertEquals(1L, t.count()),
                () -> assertEquals(42, t.totalTime(TimeUnit.MILLISECONDS), 1.0e-12));
    }

    @Test
    @DisplayName("record durations")
    default void recordDuration(MeterRegistry registry) {
        Timer t = registry.timer("myTimer");
        t.record(Duration.ofMillis(42));
        clock(registry).addAndGet(1, TimeUnit.SECONDS);

        assertAll(() -> assertEquals(1L, t.count()),
            () -> assertEquals(42, t.totalTime(TimeUnit.MILLISECONDS), 1.0e-12));
    }

    @Test
    @DisplayName("negative times are discarded by the Timer")
    default void recordNegative(MeterRegistry registry) {
        Timer t = registry.timer("myTimer");
        t.record(-42, TimeUnit.MILLISECONDS);

        assertAll(() -> assertEquals(0L, t.count()),
                () -> assertEquals(0, t.totalTime(TimeUnit.NANOSECONDS), 1.0e-12));
    }

    @Test
    @DisplayName("zero times contribute to the count of overall events but do not add to total time")
    default void recordZero(MeterRegistry registry) {
        Timer t = registry.timer("myTimer");
        t.record(0, TimeUnit.MILLISECONDS);
        clock(registry).addAndGet(1, TimeUnit.SECONDS);

        assertAll(() -> assertEquals(1L, t.count()),
                () -> assertEquals(0L, t.totalTime(TimeUnit.NANOSECONDS)));
    }

    @Test
    @DisplayName("record a runnable task")
    default void recordWithRunnable(MeterRegistry registry) throws Exception {
        Timer t = registry.timer("myTimer");

        try {
            t.record(() -> clock(registry).addAndGetNanos(10));
            clock(registry).addAndGet(1, TimeUnit.SECONDS);
        } finally {
            assertAll(() -> assertEquals(1L, t.count()),
                    () -> assertEquals(10, t.totalTime(TimeUnit.NANOSECONDS) ,1.0e-12));
        }
    }

    @Test
    @DisplayName("callable task that throws exception is still recorded")
    default void recordCallableException(MeterRegistry registry) {
        Timer t = registry.timer("myTimer");

        assertThrows(Exception.class, () -> {
            t.recordCallable(() -> {
                clock(registry).addAndGetNanos(10);
                throw new Exception("uh oh");
            });
        });

        clock(registry).addAndGet(1, TimeUnit.SECONDS);

        assertAll(() -> assertEquals(1L, t.count()),
                () -> assertEquals(10, t.totalTime(TimeUnit.NANOSECONDS), 1.0e-12));
    }
}
