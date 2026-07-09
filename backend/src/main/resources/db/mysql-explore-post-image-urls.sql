-- Step 1: add new column
ALTER TABLE explore_posts
  ADD COLUMN image_urls LONGTEXT NULL AFTER image_url;

-- Step 2: migrate existing data (run after step 1 succeeds)
-- Wrap single image_url values in a JSON array; skip nulls/empties
UPDATE explore_posts
  SET image_urls = JSON_ARRAY(image_url)
  WHERE image_url IS NOT NULL AND image_url != '';

-- Step 3: drop old column (run after step 2 succeeds)
ALTER TABLE explore_posts
  DROP COLUMN image_url;
