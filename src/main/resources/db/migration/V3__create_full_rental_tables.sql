-- V3: Vytvoření kompletního relačního modelu pro zákazníky, ceník, výpůjčky, servis a škody

-- 1. Zákazníci
-- Sjednocení typů ID z V1 s entitami (Long = BIGINT)
ALTER TABLE equipment_categories ALTER COLUMN id TYPE BIGINT;
ALTER TABLE equipment_items ALTER COLUMN id TYPE BIGINT, ALTER COLUMN category_id TYPE BIGINT;

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    id_card_number VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_last_name ON customers(last_name);

-- 2. Ceník podle kategorií a víkendových balíčků
CREATE TABLE price_rules (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES equipment_categories(id) ON DELETE RESTRICT,
    daily_rate DECIMAL(10, 2) NOT NULL,
    weekend_rate DECIMAL(10, 2) NOT NULL,
    deposit_rate DECIMAL(10, 2) NOT NULL DEFAULT 500.00,
    valid_from DATE DEFAULT CURRENT_DATE,
    valid_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_price_rules_category ON price_rules(category_id);

-- 3. Objednávky výpůjček a rezervací (hlavička agregátu)
CREATE TABLE rental_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    status VARCHAR(50) NOT NULL DEFAULT 'RESERVATION', -- RESERVATION, ACTIVE, CLOSED, EXPIRED, CANCELLED
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expected_end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_closed_at TIMESTAMP WITH TIME ZONE,
    deposit_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00, -- kauce v hotovosti
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00, -- ruční sleva
    total_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_late_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rental_orders_customer ON rental_orders(customer_id);
CREATE INDEX idx_rental_orders_status ON rental_orders(status);
CREATE INDEX idx_rental_orders_dates ON rental_orders(starts_at, expected_end_at);

-- 4. Položky výpůjčky (M:N vazba mezi objednávkou a konkrétními kusy)
CREATE TABLE rental_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES rental_orders(id) ON DELETE RESTRICT,
    item_id BIGINT NOT NULL REFERENCES equipment_items(id) ON DELETE RESTRICT,
    daily_rate_snapshot DECIMAL(10, 2) NOT NULL, -- snapshot ceny v době sjednání
    rental_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    returned_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'RESERVED', -- RESERVED, RENTED, RETURNED, DAMAGED
    late_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    damage_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rental_items_order ON rental_items(order_id);
CREATE INDEX idx_rental_items_item ON rental_items(item_id);
CREATE INDEX idx_rental_items_status ON rental_items(status);

-- 5. Servisní záznamy kusů
CREATE TABLE service_logs (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES equipment_items(id) ON DELETE RESTRICT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE,
    description TEXT NOT NULL,
    service_cost DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_logs_item ON service_logs(item_id);
CREATE INDEX idx_service_logs_active ON service_logs(item_id) WHERE finished_at IS NULL;

-- 6. Evidence poškození a škodních událostí
CREATE TABLE damage_records (
    id BIGSERIAL PRIMARY KEY,
    rental_item_id BIGINT REFERENCES rental_items(id) ON DELETE SET NULL,
    item_id BIGINT NOT NULL REFERENCES equipment_items(id) ON DELETE RESTRICT,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT NOT NULL,
    penalty_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    resolved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_damage_records_item ON damage_records(item_id);
CREATE INDEX idx_damage_records_customer ON damage_records(customer_id);
