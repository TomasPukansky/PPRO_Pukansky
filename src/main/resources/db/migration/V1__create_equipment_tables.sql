-- V1: Inicializace tabulek pro číselník kategorií a kusy vybavení

CREATE TABLE equipment_categories (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE equipment_items (
    id SERIAL PRIMARY KEY,
    category_id INTEGER NOT NULL REFERENCES equipment_categories(id) ON DELETE RESTRICT,
    inventory_number VARCHAR(50) NOT NULL UNIQUE,
    size VARCHAR(50) NOT NULL,
    purchase_year INTEGER NOT NULL,
    condition_state VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_equipment_items_category ON equipment_items(category_id);
CREATE INDEX idx_equipment_items_status ON equipment_items(status);
CREATE INDEX idx_equipment_items_inv_num ON equipment_items(inventory_number);
