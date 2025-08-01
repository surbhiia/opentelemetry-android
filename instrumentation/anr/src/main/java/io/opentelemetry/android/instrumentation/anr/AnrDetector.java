/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import android.os.Handler;
import android.os.Looper;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/** Entrypoint for installing the ANR (application not responding) detection instrumentation. */
public final class AnrDetector {
    private final List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors;
    private final Looper mainLooper;
    private final ScheduledExecutorService scheduler;
    private final AppLifecycle appLifecycle;
    private final OpenTelemetry openTelemetry;

    AnrDetector(
            List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors,
            Looper mainLooper,
            ScheduledExecutorService scheduler,
            AppLifecycle appLifecycle,
            OpenTelemetry openTelemetry) {
        this.additionalExtractors = additionalExtractors;
        this.mainLooper = mainLooper;
        this.scheduler = scheduler;
        this.appLifecycle = appLifecycle;
        this.openTelemetry = openTelemetry;
    }

    /**
     * Starts the ANR detection instrumentation.
     *
     * <p>When the main thread is unresponsive for 5 seconds or more, an event including the main
     * thread's stack trace will be reported to the RUM system.
     */
    public void start() {
        Handler uiHandler = new Handler(mainLooper);
        Logger anrLogger = openTelemetry.getLogsBridge().get("io.opentelemetry.anr");
        AnrWatcher anrWatcher =
                new AnrWatcher(uiHandler, mainLooper.getThread(), anrLogger, additionalExtractors);

        AnrDetectorToggler listener = new AnrDetectorToggler(anrWatcher, scheduler);
        // call it manually the first time to enable the ANR detection
        listener.onApplicationForegrounded();

        appLifecycle.registerListener(listener);
    }
}
