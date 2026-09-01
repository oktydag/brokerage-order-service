package com.brokerage.common.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class IdempotencyRecordCleaner {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyRecordCleaner.class);

    private final IdempotencyRecordRepository records;
    private final IdempotencyProperties properties;
    private final Clock clock;

    public IdempotencyRecordCleaner(IdempotencyRecordRepository records,
                                    IdempotencyProperties properties, Clock clock) {
        this.records = records;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.idempotency.cleanup-interval:PT1H}")
    @Transactional
    public void purgeExpired() {
        int removed = records.deleteOlderThan(clock.instant().minus(properties.retention()));
        if (removed > 0) {
            log.info("Purged {} idempotency records older than {}", removed, properties.retention());
        }
    }
}
