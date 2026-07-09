ALTER TABLE route_plans
  ADD COLUMN start_date DATE NULL AFTER days,
  ADD COLUMN end_date DATE NULL AFTER start_date;
