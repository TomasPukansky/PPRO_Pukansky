document.addEventListener('DOMContentLoaded', () => {
    // API endpoints
    const API_BASE = '/api';

    // State
    let allItems = [];
    let categories = [];
    let isSaturdayMode = false;

    // DOM Elements
    const cardsGrid = document.getElementById('cards-grid');
    const visibleCount = document.getElementById('visible-count');
    const filterCategory = document.getElementById('filter-category');
    const filterStatus = document.getElementById('filter-status');
    const inputSearch = document.getElementById('input-search');
    const btnSaturdayMode = document.getElementById('btn-saturday-mode');
    const toast = document.getElementById('toast-message');

    // Stats
    const statTotal = document.querySelector('#stat-total .stat-val');
    const statAvailable = document.querySelector('#stat-available .stat-val');
    const statService = document.querySelector('#stat-service .stat-val');
    const statOther = document.querySelector('#stat-other .stat-val');

    // Modal Create
    const modalCreate = document.getElementById('modal-create');
    const btnOpenCreate = document.getElementById('btn-open-create-modal');
    const btnCloseModal = document.getElementById('btn-close-modal');
    const btnCancelModal = document.getElementById('btn-cancel-modal');
    const formCreateItem = document.getElementById('form-create-item');
    const createCategorySelect = document.getElementById('create-category');

    // Modal Service
    const modalService = document.getElementById('modal-service');
    const btnCloseServiceModal = document.getElementById('btn-close-service-modal');
    const btnCancelServiceModal = document.getElementById('btn-cancel-service-modal');
    const formServiceItem = document.getElementById('form-service-item');
    const serviceItemId = document.getElementById('service-item-id');
    const serviceItemInfo = document.getElementById('service-item-info');
    const serviceReason = document.getElementById('service-reason');

    // Init
    init();

    async function init() {
        await loadCategories();
        await loadItems();
        setupEventListeners();
    }

    function setupEventListeners() {
        // Filter changes
        filterCategory.addEventListener('change', renderFilteredItems);
        filterStatus.addEventListener('change', () => {
            if (filterStatus.value !== 'AVAILABLE') {
                isSaturdayMode = false;
                btnSaturdayMode.classList.remove('active');
            }
            renderFilteredItems();
        });
        inputSearch.addEventListener('input', renderFilteredItems);

        // Saturday Morning Mode Button
        btnSaturdayMode.addEventListener('click', () => {
            isSaturdayMode = !isSaturdayMode;
            if (isSaturdayMode) {
                btnSaturdayMode.classList.add('active');
                filterStatus.value = 'AVAILABLE';
                showToast('⚡ Aktivován režim Sobotní ráno: Zobrazeny pouze kusy volné k okamžité výpůjčce!', 'success');
            } else {
                btnSaturdayMode.classList.remove('active');
                filterStatus.value = '';
            }
            renderFilteredItems();
        });

        // Modals open/close
        btnOpenCreate.addEventListener('click', () => modalCreate.classList.remove('hidden'));
        btnCloseModal.addEventListener('click', () => modalCreate.classList.add('hidden'));
        btnCancelModal.addEventListener('click', () => modalCreate.classList.add('hidden'));

        btnCloseServiceModal.addEventListener('click', () => modalService.classList.add('hidden'));
        btnCancelServiceModal.addEventListener('click', () => modalService.classList.add('hidden'));

        // Forms
        formCreateItem.addEventListener('submit', handleCreateItem);
        formServiceItem.addEventListener('submit', handleServiceSubmit);
    }

    async function loadCategories() {
        try {
            const res = await fetch(`${API_BASE}/categories`);
            if (!res.ok) throw new Error('Nelze načíst kategorie.');
            categories = await res.json();

            // Populate category select options
            categories.forEach(cat => {
                const optFilter = document.createElement('option');
                optFilter.value = cat.id;
                optFilter.textContent = `${cat.name} (${cat.code})`;
                filterCategory.appendChild(optFilter);

                const optCreate = document.createElement('option');
                optCreate.value = cat.id;
                optCreate.textContent = `${cat.name} (${cat.code})`;
                createCategorySelect.appendChild(optCreate);
            });
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function loadItems() {
        try {
            cardsGrid.innerHTML = '<p class="loading-text">Načítám vybavení z databáze...</p>';
            const res = await fetch(`${API_BASE}/items`);
            if (!res.ok) throw new Error('Chyba při komunikaci se serverem.');
            allItems = await res.json();
            updateStats(allItems);
            renderFilteredItems();
        } catch (err) {
            cardsGrid.innerHTML = `<p class="error-text">Chyba při načítání: ${err.message}</p>`;
            showToast(err.message, 'error');
        }
    }

    function updateStats(items) {
        statTotal.textContent = items.length;
        const available = items.filter(i => i.status === 'AVAILABLE').length;
        const inService = items.filter(i => i.status === 'IN_SERVICE').length;
        const other = items.filter(i => i.status === 'RENTED' || i.status === 'RESERVED').length;

        statAvailable.textContent = available;
        statService.textContent = inService;
        statOther.textContent = other;
    }

    function renderFilteredItems() {
        const catVal = filterCategory.value;
        const statusVal = filterStatus.value;
        const searchVal = inputSearch.value.trim().toLowerCase();

        let filtered = allItems;

        if (catVal) {
            filtered = filtered.filter(i => i.categoryId == catVal);
        }

        if (statusVal) {
            filtered = filtered.filter(i => i.status === statusVal);
        }

        if (searchVal) {
            filtered = filtered.filter(i => 
                i.inventoryNumber.toLowerCase().includes(searchVal) ||
                i.size.toLowerCase().includes(searchVal) ||
                i.categoryName.toLowerCase().includes(searchVal) ||
                i.conditionState.toLowerCase().includes(searchVal)
            );
        }

        visibleCount.textContent = `Zobrazeno: ${filtered.length} z ${allItems.length}`;

        if (filtered.length === 0) {
            cardsGrid.innerHTML = `
                <div class="empty-state">
                    <p>Žádné kusy vybavení neodpovídají zadaným kritériím.</p>
                </div>
            `;
            return;
        }

        cardsGrid.innerHTML = filtered.map(item => createCardHtml(item)).join('');

        // Attach action buttons
        attachCardActionButtons();
    }

    function createCardHtml(item) {
        const statusLabel = {
            AVAILABLE: 'Volné k výpůjčce',
            IN_SERVICE: 'V servisu',
            RENTED: 'Půjčeno zákazníkovi',
            RESERVED: 'Rezervováno',
            RETIRED: 'Vyřazeno'
        }[item.status] || item.status;

        let actionButtonHtml = '';
        if (item.status === 'AVAILABLE') {
            actionButtonHtml = `<button class="btn btn-action-service" data-id="${item.id}" data-inv="${item.inventoryNumber}">🛠️ Odeslat do servisu</button>`;
        } else if (item.status === 'IN_SERVICE') {
            actionButtonHtml = `<button class="btn btn-action-restore" data-id="${item.id}" data-inv="${item.inventoryNumber}">✅ Ukončit servis</button>`;
        }

        return `
            <div class="equipment-card">
                <div class="card-top">
                    <div>
                        <span class="card-inv">${item.inventoryNumber}</span>
                        <span class="card-category">${item.categoryName}</span>
                    </div>
                    <span class="badge-status badge-${item.status}">${statusLabel}</span>
                </div>
                <div class="card-details">
                    <div class="detail-row">
                        <span class="detail-lbl">Velikost / Parametr:</span>
                        <span class="detail-val">${item.size}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-lbl">Rok pořízení:</span>
                        <span class="detail-val">${item.purchaseYear}</span>
                    </div>
                    <div class="condition-note">
                        ${item.conditionState}
                    </div>
                </div>
                <div class="card-footer">
                    ${actionButtonHtml}
                </div>
            </div>
        `;
    }

    function attachCardActionButtons() {
        document.querySelectorAll('.btn-action-service').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                const inv = e.currentTarget.getAttribute('data-inv');
                openServiceModal(id, inv);
            });
        });

        document.querySelectorAll('.btn-action-restore').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                const inv = e.currentTarget.getAttribute('data-inv');
                restoreFromService(id, inv);
            });
        });
    }

    function openServiceModal(id, inv) {
        serviceItemId.value = id;
        serviceItemInfo.textContent = `Chystáte se odeslat kus ${inv} do servisu. Tento kus bude okamžitě zablokován a nebude nabízen k výpůjčce.`;
        serviceReason.value = '';
        modalService.classList.remove('hidden');
    }

    async function handleServiceSubmit(e) {
        e.preventDefault();
        const id = serviceItemId.value;
        const reason = serviceReason.value.trim();

        try {
            const res = await fetch(`${API_BASE}/items/${id}/status`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    status: 'IN_SERVICE',
                    note: reason
                })
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || 'Chyba při odesílání do servisu.');
            }

            modalService.classList.add('hidden');
            showToast('Kus byl úspěšně přesunut do servisu a zablokován pro výpůjčky.', 'success');
            await loadItems();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function restoreFromService(id, inv) {
        if (!confirm(`Potvrzujete ukončení servisu pro kus ${inv}? Bude opět převeden mezi VOLNÉ k výpůjčce.`)) {
            return;
        }

        try {
            const res = await fetch(`${API_BASE}/items/${id}/status`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    status: 'AVAILABLE',
                    note: 'Servis dokončen'
                })
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || 'Chyba při obnově stavu.');
            }

            showToast(`Kus ${inv} byl úspěšně vrácen do fondu volných kusů.`, 'success');
            await loadItems();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function handleCreateItem(e) {
        e.preventDefault();

        const payload = {
            categoryId: parseInt(document.getElementById('create-category').value),
            inventoryNumber: document.getElementById('create-inv-number').value.trim(),
            size: document.getElementById('create-size').value.trim(),
            purchaseYear: parseInt(document.getElementById('create-year').value),
            status: document.getElementById('create-status').value,
            conditionState: document.getElementById('create-condition').value.trim()
        };

        try {
            const res = await fetch(`${API_BASE}/items`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || 'Nepodařilo se uložit nový kus.');
            }

            modalCreate.classList.add('hidden');
            formCreateItem.reset();
            showToast(`Nový kus ${payload.inventoryNumber} byl úspěšně zaevidován do inventáře.`, 'success');
            await loadItems();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    function showToast(message, type = 'success') {
        toast.textContent = message;
        toast.className = `toast ${type}`;
        setTimeout(() => {
            toast.className = 'toast hidden';
        }, 5000);
    }
});
