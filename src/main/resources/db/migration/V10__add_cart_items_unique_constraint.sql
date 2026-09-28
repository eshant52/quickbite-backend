ALTER TABLE cart_items
    ADD CONSTRAINT uc_cart_items_cart_menuitem UNIQUE (cart_id, menu_item_id);