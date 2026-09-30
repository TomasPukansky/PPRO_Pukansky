-- V2: Seed syntetických dat pro půjčovnu Hory a voda (pan Řehák)

INSERT INTO equipment_categories (id, code, name, description) VALUES
(1, 'SKI_CARV', 'Sjezdové lyže (Carving)', 'Sjezdové lyže různých délek pro rekreační i sportovní lyžaře'),
(2, 'SKI_CROSS', 'Běžky (Cross-country)', 'Běžecké lyže na klasiku i bruslení'),
(3, 'SNOWBOARD', 'Snowboardy', 'Snowboardy freestyle a all-mountain'),
(4, 'SKI_BOOTS', 'Lyžařské boty', 'Sjezdové přezkáče různých velikostí'),
(5, 'BOAT_CANOE', 'Kanoe a rafty', 'Dvoumístné kanoe a vícemístné nafukovací rafty na letní sezónu'),
(6, 'LIFE_VEST', 'Záchranné vesty', 'Certifikované plovací vesty na divokou vodu i klidné řeky');

ALTER SEQUENCE equipment_categories_id_seq RESTART WITH 10;

INSERT INTO equipment_items (category_id, inventory_number, size, purchase_year, condition_state, status) VALUES
-- Sjezdové lyže
(1, 'SKI-001', '170 cm', 2023, 'Výborný stav, po velkém servisu hran a skluznice', 'AVAILABLE'),
(1, 'SKI-002', '170 cm', 2022, 'Běžné provozní oděrky horní folie, skluznice v pořádku', 'AVAILABLE'),
(1, 'SKI-003', '160 cm', 2024, 'Nový kus, jetý pouze 2x', 'AVAILABLE'),
(1, 'SKI-004', '160 cm', 2021, 'Hluboká rýha na patce skluznice – vyžaduje zalití', 'IN_SERVICE'),
(1, 'SKI-005', '150 cm', 2022, 'Zachovalé dětské/juniorské lyže', 'AVAILABLE'),
(1, 'SKI-006', '180 cm', 2023, 'Závodní carvingové lyže, ostré hrany', 'AVAILABLE'),

-- Běžky
(2, 'XC-001', '190 cm', 2023, 'Běžky se šupinou (skintec), stav nového kusu', 'AVAILABLE'),
(2, 'XC-002', '180 cm', 2022, 'Běžky na skate, zachovalé', 'AVAILABLE'),

-- Snowboardy
(3, 'SNB-010', '155 cm', 2023, 'All-mountain prkno, hrany nabroušeny', 'AVAILABLE'),
(3, 'SNB-011', '150 cm', 2021, 'Vytržené vázání – v dílně na opravu závitů', 'IN_SERVICE'),

-- Boty
(4, 'BOT-042', '42 EU', 2023, 'Flex index 90, neponičené přezky', 'AVAILABLE'),
(4, 'BOT-044', '44 EU', 2022, 'Vyměněná vnitřní botička', 'AVAILABLE'),
(4, 'BOT-040', '40 EU', 2024, 'Nové přezkáče z loňského nákupu', 'AVAILABLE'),

-- Letní vybavení (Kanoe, rafty a vesty)
(5, 'LOD-001', '2-místná', 2020, 'Plastová kanoe Vydra, pevná, bez průrazu', 'AVAILABLE'),
(5, 'LOD-002', '6-místný', 2022, 'Nafukovací raft Colorado, lepená jedna komora', 'AVAILABLE'),
(6, 'VES-001', 'L', 2023, 'Univerzální vesta, funkční popruhy', 'AVAILABLE');

ALTER SEQUENCE equipment_items_id_seq RESTART WITH 50;
