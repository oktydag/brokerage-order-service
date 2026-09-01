CREATE TABLE idempotency_records (
    id              UUID                     NOT NULL,
    customer_id     VARCHAR(64)              NOT NULL,
    idempotency_key VARCHAR(255)             NOT NULL,
    fingerprint     VARCHAR(64)              NOT NULL,
    resource_id     UUID                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_idempotency_records PRIMARY KEY (id),
    CONSTRAINT uq_idempotency_records_customer_key UNIQUE (customer_id, idempotency_key)
);

CREATE INDEX idx_idempotency_records_created_at ON idempotency_records (created_at);
