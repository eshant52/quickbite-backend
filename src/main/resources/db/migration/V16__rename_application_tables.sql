-- 1. Rename the application document table
ALTER TABLE application_documents
    RENAME TO restaurant_application_documents;

-- 2. Rename the primary key constraint
ALTER TABLE restaurant_application_documents
    RENAME CONSTRAINT application_documents_pkey TO restaurant_application_documents_pkey;

-- 3. Rename the foreign key constraint
ALTER TABLE restaurant_application_documents
    RENAME CONSTRAINT application_documents_application_id_fkey TO restaurant_application_documents_application_id_fkey;

-- 4. Rename the indexes
ALTER INDEX application_documents_application_id_type_key
    RENAME TO restaurant_application_documents_application_id_type_key;

-- 5. Rename the application hours table
ALTER TABLE application_hours
    RENAME TO restaurant_application_hours;

-- 6. Rename the primary key constraint
ALTER TABLE restaurant_application_hours
    RENAME CONSTRAINT application_hours_pkey TO restaurant_application_hours_pkey;

-- 7. Rename the foreign key constraint
ALTER TABLE restaurant_application_hours
    RENAME CONSTRAINT application_hours_application_id_fkey TO restaurant_application_hours_application_id_fkey;

-- 8. Rename the indexes
ALTER INDEX application_hours_application_id_day_of_week_key
    RENAME TO restaurant_application_hours_application_id_day_of_week_key;

-- 9. Rename the application image table
ALTER TABLE application_images
    RENAME TO restaurant_application_images;

-- 10. Rename the primary key constraint
ALTER TABLE restaurant_application_images
    RENAME CONSTRAINT application_images_pkey TO restaurant_application_images_pkey;

-- 11. Rename the foreign key constraint
ALTER TABLE restaurant_application_images
    RENAME CONSTRAINT application_images_application_id_fkey TO restaurant_application_images_application_id_fkey;
