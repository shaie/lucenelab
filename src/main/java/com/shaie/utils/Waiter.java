/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shaie.utils;

import java.util.concurrent.TimeUnit;

/** Wait for some condition to become true, or for a timeout to expire. */
public class Waiter {

    private static final int DEFAULT_SLEEP_TIME_BETWEEN_CHECKS_MS = 10;

    private Waiter() {
        // Should not be instantiated
    }

    /**
     * @param condition
     *            The condition to check
     * @param time
     *            The maximum time to wait for the condition to become true
     * @param unit
     *            The time unit of the {@code time} argument
     *
     * @return true if the condition was true before the timeout, false if it wasn't.
     */
    public static boolean waitFor(Condition condition, long time, TimeUnit unit) {
        return waitFor(condition, time, unit, DEFAULT_SLEEP_TIME_BETWEEN_CHECKS_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * @param condition
     *            The condition to check
     * @param time
     *            The maximum time to wait for the condition to become true
     * @param unit
     *            The time unit of the {@code time} argument
     * @param pollEvery
     *            The time to wait between each polling of the condition
     * @param pollUnit
     *            The time unit of the {@code pollEvery} argument
     *
     * @return true if the condition was true before the timeout, false if it wasn't.
     */
    public static boolean waitFor(Condition condition, long time, TimeUnit unit, long pollEvery, TimeUnit pollUnit) {
        final long waitMs = unit.toMillis(time);
        final long pollwaitMs = pollUnit.toMillis(pollEvery);
        final long startMs = System.currentTimeMillis();
        while (System.currentTimeMillis() - startMs < waitMs) {
            if (condition.isSatisfied()) {
                return true;
            }
            try {
                Thread.sleep(pollwaitMs);
            } catch (final InterruptedException e) {
                throw new RuntimeException("WaitFor aborted", e);
            }
        }
        return false;
    }

    public interface Condition {
        /** Returns true/false indicating whether or not the condition has been met. */
        boolean isSatisfied();
    }

}