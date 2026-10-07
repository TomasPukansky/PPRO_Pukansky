-- V4: Syntetická demo data pro zákazníky, ceník, výpůjčky, rezervace, servis a škody

-- 1. Zákazníci půjčovny
INSERT INTO customers (id, first_name, last_name, phone, id_card_number, note) VALUES
(1, 'Martin', 'Novák', '+420 777 123 456', '112233445', 'Pravidelný spolehlivý zákazník'),
(2, 'Lucie', 'Černá', '+420 775 234 567', '223344556', 'Preferuje carvingové lyže kratší délky'),
(3, 'Jan', 'Dvořák', '+420 776 345 678', '334455667', 'Často vrací na poslední chvíli'),
(4, 'Eva', 'Kučerová', '+420 774 456 789', '445566778', 'Rodinné výpůjčky (dětské sety)'),
(5, 'Tomáš', 'Procházka', '+420 773 567 890', '556677889', 'Volal v úterý – rezervace na víkend'),
(6, 'Petra', 'Veselá', '+420 772 678 901', '667788990', 'Ranní víkendová rezervace'),
(7, 'Pavel', 'Horák', '+420 771 789 012', '778899001', 'Vodák, půjčuje lodě a vesty v létě'),
(8, 'Kateřina', 'Němcová', '+420 770 890 123', '889900112', 'Snowboardistka');

ALTER SEQUENCE customers_id_seq RESTART WITH 20;

-- 2. Ceník pro všechny kategorie
INSERT INTO price_rules (category_id, daily_rate, weekend_rate, deposit_rate, valid_from) VALUES
(1, 350.00, 600.00, 1000.00, CURRENT_DATE - INTERVAL '30 days'),
(2, 250.00, 450.00, 800.00, CURRENT_DATE - INTERVAL '30 days'),
(3, 380.00, 650.00, 1000.00, CURRENT_DATE - INTERVAL '30 days'),
(4, 150.00, 260.00, 500.00, CURRENT_DATE - INTERVAL '30 days'),
(5, 400.00, 700.00, 1500.00, CURRENT_DATE - INTERVAL '30 days'),
(6, 80.00, 140.00, 200.00, CURRENT_DATE - INTERVAL '30 days');

-- 3. Servisní záznamy (Service Logs)
INSERT INTO service_logs (id, item_id, started_at, finished_at, description, service_cost) VALUES
(1, 4, CURRENT_TIMESTAMP - INTERVAL '3 days', NULL, 'Hluboká rýha na patce skluznice – vyžaduje zalití kofixem a broušení hran', 0.00),
(2, 10, CURRENT_TIMESTAMP - INTERVAL '5 days', NULL, 'Vytržené vázání – oprava montážních insertů v dílně', 0.00),
(3, 1, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '17 days', 'Předsezónní velký servis a kontrola vypínacích sil vázání', 450.00);

ALTER SEQUENCE service_logs_id_seq RESTART WITH 10;

-- 4. Ukázkové objednávky výpůjček a rezervací
-- Order 1: Aktivní výpůjčka venku (Martin Novák)
INSERT INTO rental_orders (id, order_number, customer_id, status, starts_at, expected_end_at, deposit_amount, discount_amount, total_price, total_late_fee, note) VALUES
(1, 'VYP-2026-001', 1, 'ACTIVE', 
 CURRENT_TIMESTAMP - INTERVAL '1 day', 
 CURRENT_TIMESTAMP + INTERVAL '1 day', 
 1500.00, 0.00, 1000.00, 0.00, 'Zapůjčeno na 2 dny');

-- Order 2: Výpůjčka po termínu – prodlení a penále (Jan Dvořák)
INSERT INTO rental_orders (id, order_number, customer_id, status, starts_at, expected_end_at, deposit_amount, discount_amount, total_price, total_late_fee, note) VALUES
(2, 'VYP-2026-002', 3, 'ACTIVE', 
 CURRENT_TIMESTAMP - INTERVAL '3 days', 
 CURRENT_TIMESTAMP - INTERVAL '1 day', 
 1000.00, 0.00, 760.00, 380.00, 'Zpoždění vrácení o 1 den – naskakuje penále!');

-- Order 3: Částečně vrácená výpůjčka – lyže vráceny, boty venku (Eva Kučerová)
INSERT INTO rental_orders (id, order_number, customer_id, status, starts_at, expected_end_at, deposit_amount, discount_amount, total_price, total_late_fee, note) VALUES
(3, 'VYP-2026-003', 4, 'ACTIVE', 
 CURRENT_TIMESTAMP - INTERVAL '1 day', 
 CURRENT_TIMESTAMP + INTERVAL '5 hours', 
 1500.00, 50.00, 950.00, 0.00, 'Lyže vráceny v poledne, boty zůstaly do večera');

-- Order 4: Rezervace po telefonu na nejbližší sobotu (Tomáš Procházka)
INSERT INTO rental_orders (id, order_number, customer_id, status, starts_at, expected_end_at, deposit_amount, discount_amount, total_price, total_late_fee, note) VALUES
(4, 'REZ-2026-004', 5, 'RESERVATION', 
 date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '5 days 8 hours 30 minutes', -- sobota 08:30
 date_trunc('week', CURRENT_TIMESTAMP) + INTERVAL '6 days 17 hours', -- neděle 17:00
 0.00, 0.00, 600.00, 0.00, 'Telefonická rezervace z úterý, zákazník dorazí v sobotu ráno');

-- Order 5: Nevyzvednutá ranní rezervace – po 10:00 ke stornu (Petra Veselá)
INSERT INTO rental_orders (id, order_number, customer_id, status, starts_at, expected_end_at, deposit_amount, discount_amount, total_price, total_late_fee, note) VALUES
(5, 'REZ-2026-005', 6, 'RESERVATION', 
 CURRENT_DATE + TIME '08:00:00', 
 CURRENT_DATE + TIME '18:00:00', 
 0.00, 0.00, 350.00, 0.00, 'Měla vyzvednout do 10:00, nedorazila – vhodné pro storno');

-- Order 6: Historicky uzavřená výpůjčka s poškozením (Lucie Černá)
INSERT INTO rental_orders (id, order_number, customer_id, status, starts_at, expected_end_at, actual_closed_at, deposit_amount, discount_amount, total_price, total_late_fee, note) VALUES
(6, 'VYP-2026-000', 2, 'CLOSED', 
 CURRENT_TIMESTAMP - INTERVAL '8 days', 
 CURRENT_TIMESTAMP - INTERVAL '6 days', 
 CURRENT_TIMESTAMP - INTERVAL '6 days', 
 1000.00, 0.00, 700.00, 0.00, 'Vráceno v pořádku, ale se škodou na hraně');

ALTER SEQUENCE rental_orders_id_seq RESTART WITH 20;

-- 5. Položky výpůjček (Rental Items)
-- Pro Order 1 (Novák)
INSERT INTO rental_items (id, order_id, item_id, daily_rate_snapshot, rental_price, status) VALUES
(1, 1, 1, 350.00, 700.00, 'RENTED'), -- SKI-001
(2, 1, 11, 150.00, 300.00, 'RENTED'); -- BOT-042

-- Pro Order 2 (Dvořák - po termínu)
INSERT INTO rental_items (id, order_id, item_id, daily_rate_snapshot, rental_price, status, late_fee) VALUES
(3, 2, 9, 380.00, 760.00, 'RENTED', 380.00); -- SNB-010

-- Pro Order 3 (Kučerová - částečné vrácení)
INSERT INTO rental_items (id, order_id, item_id, daily_rate_snapshot, rental_price, returned_at, status) VALUES
(4, 3, 3, 350.00, 700.00, CURRENT_TIMESTAMP - INTERVAL '2 hours', 'RETURNED'), -- SKI-003 vráceno!
(5, 3, 12, 150.00, 300.00, NULL, 'RENTED'); -- BOT-044 stále půjčeno

-- Pro Order 4 (Procházka - víkendová rezervace)
INSERT INTO rental_items (id, order_id, item_id, daily_rate_snapshot, rental_price, status) VALUES
(6, 4, 2, 350.00, 600.00, 'RESERVED'); -- SKI-002 víkendový balíček

-- Pro Order 5 (Veselá - nevyzvednutá rezervace)
INSERT INTO rental_items (id, order_id, item_id, daily_rate_snapshot, rental_price, status) VALUES
(7, 5, 5, 350.00, 350.00, 'RESERVED'); -- SKI-005

-- Pro Order 6 (Černá - uzavřená se škodou)
INSERT INTO rental_items (id, order_id, item_id, daily_rate_snapshot, rental_price, returned_at, status, damage_fee) VALUES
(8, 6, 6, 350.00, 700.00, CURRENT_TIMESTAMP - INTERVAL '6 days', 'RETURNED', 400.00); -- SKI-006

ALTER SEQUENCE rental_items_id_seq RESTART WITH 20;

-- 6. Škodní událost (Damage Record)
INSERT INTO damage_records (id, rental_item_id, item_id, customer_id, reported_at, description, penalty_fee, resolved) VALUES
(1, 8, 6, 2, CURRENT_TIMESTAMP - INTERVAL '6 days', 'Vyštípnutá hrana na kamenech – strženo 400 Kč z hotovostní kauce', 400.00, TRUE);

ALTER SEQUENCE damage_records_id_seq RESTART WITH 10;

-- 7. Aktualizace stavů v equipment_items dle reálného seedu
UPDATE equipment_items SET status = 'RENTED' WHERE id IN (1, 9, 11, 12);
UPDATE equipment_items SET status = 'RESERVED' WHERE id IN (2, 5);
UPDATE equipment_items SET status = 'AVAILABLE' WHERE id IN (3, 6, 7, 8, 13, 14, 15, 16);
-- id 4 a 10 zůstávají IN_SERVICE
