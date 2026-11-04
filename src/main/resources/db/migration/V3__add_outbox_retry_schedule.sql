ALTER TABLE event_outbox
ADD COLUMN  next_retry_at TIMESTAMPTZ;


CREATE INDEX idx_event_outbox_pending_retry
ON event_outbox (next_retry_at, created_at)
WHERE status = 'PENDING';