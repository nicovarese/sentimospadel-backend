ALTER TABLE player_notifications
    ADD COLUMN managed_by_sync BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_player_notifications_player_sync_active
    ON player_notifications(player_profile_id, managed_by_sync, active, created_at DESC);
