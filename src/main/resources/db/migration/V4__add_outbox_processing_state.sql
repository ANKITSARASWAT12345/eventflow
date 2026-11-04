ALTER TABLE event_outbox
    ADD COLUMN locked_at TIMESTAMPTZ;

ALTER TABLE event_outbox
DROP CONSTRAINT IF EXISTS event_outbox_status_check;

ALTER TABLE event_outbox
    ADD CONSTRAINT event_outbox_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

CREATE INDEX idx_event_outbox_processing_lease
    ON event_outbox (locked_at)
    WHERE status = 'PROCESSING';
