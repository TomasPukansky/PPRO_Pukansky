-- V6: Rozšíření inventáře pro zkoušení rezervací (více kusů v každé kategorii a velikosti)

INSERT INTO equipment_items (category_id, inventory_number, size, purchase_year, condition_state, status) VALUES
-- Sjezdové lyže
(1, 'SKI-007', '150 cm', 2024, 'Nové juniorské lyže', 'AVAILABLE'),
(1, 'SKI-008', '150 cm', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(1, 'SKI-009', '160 cm', 2024, 'Nový kus', 'AVAILABLE'),
(1, 'SKI-010', '160 cm', 2023, 'Velmi dobrý stav, hrany po servisu', 'AVAILABLE'),
(1, 'SKI-011', '160 cm', 2022, 'Běžné oděrky horní fólie', 'AVAILABLE'),
(1, 'SKI-012', '170 cm', 2024, 'Nový kus', 'AVAILABLE'),
(1, 'SKI-013', '170 cm', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(1, 'SKI-014', '170 cm', 2022, 'Běžné oděrky, skluznice v pořádku', 'AVAILABLE'),
(1, 'SKI-015', '180 cm', 2024, 'Nový kus', 'AVAILABLE'),
(1, 'SKI-016', '180 cm', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(1, 'SKI-017', '180 cm', 2022, 'Zachovalé, po servisu skluznice', 'AVAILABLE'),

-- Běžky
(2, 'XC-003', '170 cm', 2024, 'Běžky na klasiku, nový kus', 'AVAILABLE'),
(2, 'XC-004', '170 cm', 2023, 'Běžky na klasiku, zachovalé', 'AVAILABLE'),
(2, 'XC-005', '180 cm', 2024, 'Běžky na klasiku se šupinou', 'AVAILABLE'),
(2, 'XC-006', '180 cm', 2023, 'Běžky na bruslení, zachovalé', 'AVAILABLE'),
(2, 'XC-007', '190 cm', 2024, 'Běžky na bruslení, nový kus', 'AVAILABLE'),
(2, 'XC-008', '190 cm', 2022, 'Běžky na klasiku, běžné oděrky', 'AVAILABLE'),

-- Snowboardy
(3, 'SNB-012', '145 cm', 2024, 'Nové prkno pro začátečníky', 'AVAILABLE'),
(3, 'SNB-013', '145 cm', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(3, 'SNB-014', '150 cm', 2024, 'Nové prkno, hrany nabroušeny', 'AVAILABLE'),
(3, 'SNB-015', '150 cm', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(3, 'SNB-016', '155 cm', 2024, 'Nové prkno na celou horu', 'AVAILABLE'),
(3, 'SNB-017', '155 cm', 2022, 'Běžné oděrky, vázání v pořádku', 'AVAILABLE'),
(3, 'SNB-018', '160 cm', 2024, 'Nové prkno', 'AVAILABLE'),
(3, 'SNB-019', '160 cm', 2023, 'Velmi dobrý stav', 'AVAILABLE'),

-- Lyžařské boty
(4, 'BOT-101', '38 EU', 2024, 'Nové boty', 'AVAILABLE'),
(4, 'BOT-102', '39 EU', 2024, 'Nové boty', 'AVAILABLE'),
(4, 'BOT-103', '40 EU', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(4, 'BOT-104', '40 EU', 2024, 'Nové boty', 'AVAILABLE'),
(4, 'BOT-105', '41 EU', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(4, 'BOT-106', '41 EU', 2024, 'Nové boty', 'AVAILABLE'),
(4, 'BOT-107', '42 EU', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(4, 'BOT-108', '42 EU', 2024, 'Nové boty', 'AVAILABLE'),
(4, 'BOT-109', '43 EU', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(4, 'BOT-110', '43 EU', 2024, 'Nové boty', 'AVAILABLE'),
(4, 'BOT-111', '44 EU', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(4, 'BOT-112', '44 EU', 2024, 'Nové boty', 'AVAILABLE'),
(4, 'BOT-113', '45 EU', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(4, 'BOT-114', '46 EU', 2023, 'Velmi dobrý stav', 'AVAILABLE'),

-- Kanoe a rafty
(5, 'LOD-003', '2-místná', 2023, 'Kanoe bez prasklin, pádla v pořádku', 'AVAILABLE'),
(5, 'LOD-004', '2-místná', 2024, 'Nová kanoe', 'AVAILABLE'),
(5, 'LOD-005', '6-místný', 2023, 'Raft po kontrole těsnosti', 'AVAILABLE'),

-- Záchranné vesty
(6, 'VES-002', 'S', 2024, 'Nová vesta', 'AVAILABLE'),
(6, 'VES-003', 'S', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(6, 'VES-004', 'M', 2024, 'Nová vesta', 'AVAILABLE'),
(6, 'VES-005', 'M', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(6, 'VES-006', 'M', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(6, 'VES-007', 'L', 2024, 'Nová vesta', 'AVAILABLE'),
(6, 'VES-008', 'L', 2023, 'Velmi dobrý stav', 'AVAILABLE'),
(6, 'VES-009', 'XL', 2024, 'Nová vesta', 'AVAILABLE'),
(6, 'VES-010', 'XL', 2023, 'Velmi dobrý stav', 'AVAILABLE');
