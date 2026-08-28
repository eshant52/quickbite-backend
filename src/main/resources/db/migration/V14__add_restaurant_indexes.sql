-- Indexes for restaurant queries and relationships
CREATE INDEX idx_restaurants_owner_id ON restaurants(owner_id);
CREATE INDEX idx_restaurants_status ON restaurants(current_status);
CREATE INDEX idx_restaurant_hours_restaurant_id ON restaurant_hours(restaurant_id);
CREATE INDEX idx_restaurant_images_restaurant_id ON restaurant_images(restaurant_id);
CREATE INDEX idx_restaurant_documents_restaurant_id ON restaurant_documents(restaurant_id);