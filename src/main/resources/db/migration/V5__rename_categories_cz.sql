-- Z1: názvy kategorií bez anglicismů (požadavek klienta 2026-10-21)
UPDATE equipment_categories SET name = 'Sjezdové lyže' WHERE code = 'SKI_CARV';
UPDATE equipment_categories SET name = 'Běžky' WHERE code = 'SKI_CROSS';
