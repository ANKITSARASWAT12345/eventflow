
CREATE TABLE event_outbox (
                              outbox_id UUID PRIMARY KEY,
                              event_id UUID NOT NULL UNIQUE,
                              event_type VARCHAR(120) NOT NULL,
                              payload JSONB NOT NULL,

                              status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                              attempts INTEGER NOT NULL DEFAULT 0,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              published_at TIMESTAMPTZ,
                              last_error TEXT,

                              CONSTRAINT fk_event_outbox_event
                                  FOREIGN KEY (event_id)
                                      REFERENCES webhook_events(event_id),

                              CONSTRAINT chk_event_outbox_status
                                  CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),

                              CONSTRAINT chk_event_outbox_attempts
                                  CHECK (attempts >= 0)
);

CREATE INDEX idx_event_outbox_pending
    ON event_outbox (created_at)
    WHERE status = 'PENDING';
