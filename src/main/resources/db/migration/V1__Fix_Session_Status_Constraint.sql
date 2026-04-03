-- Fix session status check constraint to include all valid session statuses
-- The constraint should allow: PENDING_APPROVAL, SCHEDULED, IN_PROGRESS, COMPLETED, DISPUTED, CANCELLED

ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_status_check;

ALTER TABLE sessions 
ADD CONSTRAINT sessions_status_check 
CHECK (status IN ('PENDING_APPROVAL', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'DISPUTED', 'CANCELLED'));
