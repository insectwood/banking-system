package com.example.corebanking.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Custom metrics for transfer
 * The following metrics are collected by Prometheus:
 * - banking_transfer_total{result="success|fail"} : The number of successful/failed transfers
 * - banking_transfer_duration_seconds : Transfer processing time
 */
@Component
public class TransferMetrics {

    private final Counter successCounter;
    private final Counter failCounter;
    private final Timer transferTimer;

    public TransferMetrics(MeterRegistry registry) {
        this.successCounter = Counter.builder("banking_transfer_total")
                .tag("result", "success")
                .description("Total successful transfers")
                .register(registry);

        this.failCounter = Counter.builder("banking_transfer_total")
                .tag("result", "fail")
                .description("Total failed transfers")
                .register(registry);

        this.transferTimer = Timer.builder("banking_transfer_duration")
                .description("Transfer processing duration")
                .register(registry);
    }

    public void recordSuccess() {
        successCounter.increment();
    }

    public void recordFailure() {
        failCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(transferTimer);
    }
}
