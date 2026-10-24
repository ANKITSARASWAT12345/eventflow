CREATE TABLE webhook_events(
    event_id UUID PRIMARY KEY ,
    event_type VARCHAR(120) NOT NULL ,
    payload JSONB NOT NULL ,
    status VARCHAR(30) NOT NULL ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_events_created_at
    ON webhook_events(created_at DESC );

CREATE INDEX idx_webhook_events_status_created_at
    ON webhook_events (status, created_at DESC);


