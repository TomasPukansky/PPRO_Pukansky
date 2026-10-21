document.addEventListener('DOMContentLoaded', () => {
    const API = '/api';

    // State
    let categories = [];
    let priceRules = [];
    let allInventoryItems = [];

    // ================= PŘEKLADY (Z3) =================
    // Texty UI jsou v souborech /i18n/app_cz.arb a /i18n/app_en.arb, zvolený jazyk se ukládá do localStorage.

    const LANGUAGES = {
        cs: { file: 'app_cz.arb', locale: 'cs-CZ' },
        en: { file: 'app_en.arb', locale: 'en-GB' }
    };
    const LANG_STORAGE_KEY = 'lang';
    let currentLang = 'cs';
    let messages = {};

    function loadSavedLanguage() {
        try {
            const saved = localStorage.getItem(LANG_STORAGE_KEY);
            if (LANGUAGES[saved]) return saved;
        } catch (err) {
            // localStorage nemusí být dostupný (anonymní okno, blokovaná data)
        }
        return 'cs';
    }

    async function setLanguage(lang) {
        const res = await fetch(`/i18n/${LANGUAGES[lang].file}`);
        messages = await res.json();
        currentLang = lang;
        try {
            localStorage.setItem(LANG_STORAGE_KEY, lang);
        } catch (err) {
            // volba se pak jen nezapamatuje
        }
        document.documentElement.lang = lang;
        applyStaticTranslations();
        document.querySelectorAll('.lang-btn').forEach(btn =>
            btn.classList.toggle('active', btn.getAttribute('data-lang') === lang));
    }

    // Vrátí překlad klíče a doplní zástupné symboly {param}
    function t(key, params = {}) {
        let text = messages[key] ?? key;
        Object.entries(params).forEach(([name, value]) => {
            text = text.split(`{${name}}`).join(value);
        });
        return text;
    }

    function applyStaticTranslations() {
        document.querySelectorAll('[data-i18n]').forEach(el => { el.innerHTML = t(el.getAttribute('data-i18n')); });
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => { el.placeholder = t(el.getAttribute('data-i18n-placeholder')); });
        document.querySelectorAll('[data-i18n-title]').forEach(el => { el.title = t(el.getAttribute('data-i18n-title')); });
    }

    async function switchLanguage(lang) {
        if (lang === currentLang) return;
        await setLanguage(lang);
        populateCategoryDropdowns();
        await loadHeaderStats();
        // Znovu vykreslit aktivní záložku v novém jazyce
        document.querySelector('.nav-tab.active')?.click();
    }

    // Názvy stavů kusů, položek a objednávek (hodnoty enumů z backendu) -> klíče překladů
    const STATUS_KEYS = {
        AVAILABLE: 'statusAvailable',
        RESERVED: 'statusReserved',
        RENTED: 'statusRented',
        IN_SERVICE: 'statusInService',
        RETIRED: 'statusRetired',
        RETURNED: 'statusReturned',
        DAMAGED: 'statusDamaged',
        RESERVATION: 'statusReservation',
        ACTIVE: 'statusActive',
        CLOSED: 'statusClosed',
        EXPIRED: 'statusExpired',
        CANCELLED: 'statusCancelled'
    };
    const statusLabel = (s) => (STATUS_KEYS[s] ? t(STATUS_KEYS[s]) : s);
    const statusBadge = (s) => `<span class="badge-status badge-${s}">${statusLabel(s)}</span>`;
    const money = (amount) => `${amount} ${t('currency')}`;

    // DOM Elements
    const toast = document.getElementById('toast-message');

    // Init
    init();

    async function init() {
        await setLanguage(loadSavedLanguage());
        setupTabs();
        await loadCategories();
        await loadPriceRules();
        await refreshAllData();
        setupEventListeners();
    }

    function setupTabs() {
        const tabs = document.querySelectorAll('.nav-tab');
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                tabs.forEach(t => t.classList.remove('active'));
                document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

                tab.classList.add('active');
                const targetId = tab.getAttribute('data-tab');
                const pane = document.getElementById(targetId);
                if (pane) pane.classList.add('active');

                // Trigger tab-specific refresh
                if (targetId === 'tab-saturday') loadSaturdayDispatch();
                else if (targetId === 'tab-orders') loadOrders();
                else if (targetId === 'tab-inventory') loadInventory();
                else if (targetId === 'tab-customers') loadCustomers();
                else if (targetId === 'tab-reports') loadReports();
                else if (targetId === 'tab-pricing') renderPricingTable();
            });
        });
    }

    async function refreshAllData() {
        await loadHeaderStats();
        loadSaturdayDispatch();
    }

    function setupEventListeners() {
        // Přepínač jazyka
        document.querySelectorAll('.lang-btn').forEach(btn =>
            btn.addEventListener('click', () => switchLanguage(btn.getAttribute('data-lang'))));

        // Saturday dispatch date
        const matrixDate = document.getElementById('matrix-date');
        if (matrixDate) {
            matrixDate.value = new Date().toISOString().split('T')[0];
            matrixDate.addEventListener('change', () => loadMatrix(matrixDate.value));
        }

        // Expire unclaimed
        document.getElementById('btn-expire-unclaimed')?.addEventListener('click', handleExpireUnclaimed);

        // Phone reservation
        document.getElementById('btn-lookup-phone')?.addEventListener('click', handlePhoneLookup);
        document.getElementById('btn-set-weekend')?.addEventListener('click', setWeekendDates);
        document.getElementById('btn-set-tomorrow')?.addEventListener('click', setTomorrowDates);
        document.getElementById('btn-add-res-item')?.addEventListener('click', addReservationItemRow);
        document.getElementById('form-phone-reservation')?.addEventListener('submit', handleCreateReservation);

        // Orders filter
        document.getElementById('filter-order-status')?.addEventListener('change', loadOrders);

        // Filtry inventáře a založení kusu
        document.getElementById('filter-category')?.addEventListener('change', renderFilteredInventory);
        document.getElementById('filter-status')?.addEventListener('change', renderFilteredInventory);
        document.getElementById('input-search')?.addEventListener('input', renderFilteredInventory);
        document.getElementById('btn-open-create-modal')?.addEventListener('click', () => document.getElementById('modal-create').classList.remove('hidden'));
        document.getElementById('btn-close-modal')?.addEventListener('click', () => document.getElementById('modal-create').classList.add('hidden'));
        document.getElementById('btn-cancel-modal')?.addEventListener('click', () => document.getElementById('modal-create').classList.add('hidden'));
        document.getElementById('form-create-item')?.addEventListener('submit', handleCreateInventoryItem);

        // Customers search
        document.getElementById('input-search-customers')?.addEventListener('input', (e) => loadCustomers(e.target.value));

        // Modals: Handout, Swap, Return, Print
        setupModalListeners();
    }

    function setupModalListeners() {
        // Handout modal
        document.getElementById('btn-close-handout')?.addEventListener('click', () => document.getElementById('modal-handout').classList.add('hidden'));
        document.getElementById('btn-cancel-handout')?.addEventListener('click', () => document.getElementById('modal-handout').classList.add('hidden'));
        document.getElementById('form-handout')?.addEventListener('submit', handleHandoutSubmit);

        // Swap modal
        document.getElementById('btn-close-swap')?.addEventListener('click', () => document.getElementById('modal-swap').classList.add('hidden'));
        document.getElementById('btn-cancel-swap')?.addEventListener('click', () => document.getElementById('modal-swap').classList.add('hidden'));
        document.getElementById('form-swap')?.addEventListener('submit', handleSwapSubmit);

        // Return modal
        document.getElementById('btn-close-return')?.addEventListener('click', () => document.getElementById('modal-return').classList.add('hidden'));
        document.getElementById('btn-cancel-return')?.addEventListener('click', () => document.getElementById('modal-return').classList.add('hidden'));
        document.getElementById('form-return')?.addEventListener('submit', handleReturnSubmit);
        document.getElementById('return-damaged')?.addEventListener('change', (e) => {
            const group = document.getElementById('damage-details-group');
            if (e.target.checked) group.classList.remove('hidden');
            else group.classList.add('hidden');
        });

        // Print modal
        document.getElementById('btn-close-print')?.addEventListener('click', () => document.getElementById('modal-print-contract').classList.add('hidden'));
        document.getElementById('btn-cancel-print')?.addEventListener('click', () => document.getElementById('modal-print-contract').classList.add('hidden'));
        document.getElementById('btn-do-print')?.addEventListener('click', () => window.print());
    }

    // ================= DATA LOADERS =================

    async function loadCategories() {
        try {
            const res = await fetch(`${API}/categories`);
            categories = await res.json();
            populateCategoryDropdowns();
        } catch (err) {
            showToast(t('errLoadCategories'), 'error');
        }
    }

    async function loadPriceRules() {
        try {
            const res = await fetch(`${API}/price-rules`);
            priceRules = await res.json();
        } catch (err) {
            console.error(err);
        }
    }

    function populateCategoryDropdowns() {
        const selects = [
            document.getElementById('filter-category'),
            document.getElementById('create-category'),
            ...document.querySelectorAll('.res-item-category')
        ];
        selects.forEach(select => {
            if (!select) return;
            const selected = select.value;
            select.innerHTML = select.id === 'filter-category'
                ? `<option value="">${t('allCategories')}</option>`
                : `<option value="">${t('selectCategory')}</option>`;
            categories.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat.id;
                opt.textContent = `${cat.name} (${cat.code})`;
                select.appendChild(opt);
            });
            select.value = selected;
        });
    }

    async function loadHeaderStats() {
        try {
            const [itemsRes, overdueRes, ordersRes] = await Promise.all([
                fetch(`${API}/items`),
                fetch(`${API}/reports/overdue`),
                fetch(`${API}/orders?status=ACTIVE`)
            ]);
            const items = await itemsRes.json();
            const overdue = await overdueRes.json();
            const activeOrders = await ordersRes.json();

            const available = items.filter(i => i.status === 'AVAILABLE').length;
            const inService = items.filter(i => i.status === 'IN_SERVICE').length;
            const rented = items.filter(i => i.status === 'RENTED').length;

            document.querySelector('#stat-available .stat-val').textContent = available;
            document.querySelector('#stat-service .stat-val').textContent = inService;
            document.querySelector('#stat-rented .stat-val').textContent = rented;
            document.querySelector('#stat-overdue .stat-val').textContent = overdue.length;
        } catch (err) {
            console.error('Stats error', err);
        }
    }

    // ================= TAB 1: SOBOTA RÁNO =================

    async function loadSaturdayDispatch() {
        const dateInput = document.getElementById('matrix-date');
        const selectedDate = dateInput ? dateInput.value : new Date().toISOString().split('T')[0];
        await loadMatrix(selectedDate);
        await loadSaturdayOrders();
    }

    async function loadMatrix(dateStr) {
        const container = document.getElementById('matrix-container');
        try {
            container.innerHTML = `<p class="loading-text">${t('matrixUpdating')}</p>`;
            const res = await fetch(`${API}/reports/availability-matrix?date=${dateStr}`);
            const data = await res.json();
            renderMatrixTable(data);
        } catch (err) {
            container.innerHTML = `<p class="error-text">${t('errMatrix', { message: err.message })}</p>`;
        }
    }

    function renderMatrixTable(matrixData) {
        const container = document.getElementById('matrix-container');
        const catMap = matrixData.categories || {};
        const catNames = Object.keys(catMap);

        if (catNames.length === 0) {
            container.innerHTML = `<p class="empty-state">${t('noCategories')}</p>`;
            return;
        }

        let html = `<table class="matrix-table"><thead><tr><th class="cat-col">${t('colCategory')}</th>`;
        // Získat všechny unikátní velikosti napříč kategoriemi
        const allSizes = new Set();
        catNames.forEach(c => {
            Object.keys(catMap[c]).forEach(s => allSizes.add(s));
        });
        const sizesArr = Array.from(allSizes).sort();

        sizesArr.forEach(s => { html += `<th>${s}</th>`; });
        html += '</tr></thead><tbody>';

        catNames.forEach(catName => {
            html += `<tr><th class="cat-col">${catName}</th>`;
            sizesArr.forEach(size => {
                const cell = catMap[catName][size];
                if (cell) {
                    html += `
                        <td>
                            <div class="matrix-cell">
                                <span class="matrix-avail">${t('matrixAvailable', { count: cell.available })}</span>
                                <span class="matrix-sub">${t('matrixOf', { total: cell.total, inService: cell.in_service })}</span>
                            </div>
                        </td>
                    `;
                } else {
                    html += '<td style="color:#64748b;">—</td>';
                }
            });
            html += '</tr>';
        });

        html += '</tbody></table>';
        container.innerHTML = html;
    }

    async function loadSaturdayOrders() {
        try {
            const [resOrders, retOrders] = await Promise.all([
                fetch(`${API}/orders?status=RESERVATION`),
                fetch(`${API}/orders?status=ACTIVE`)
            ]);
            const reservations = await resOrders.json();
            const actives = await retOrders.json();

            // Pickups list
            const pickupsEl = document.getElementById('saturday-pickups-list');
            document.getElementById('saturday-pickups-count').textContent = reservations.length;

            if (reservations.length === 0) {
                pickupsEl.innerHTML = `<p class="empty-state">${t('noPickupsToday')}</p>`;
            } else {
                pickupsEl.innerHTML = reservations.map(o => `
                    <div class="mini-order-item">
                        <div class="mini-order-info">
                            <h4>${o.orderNumber} – ${o.customerName}</h4>
                            <p>${t('pickupLine', { phone: o.customerPhone, date: formatDateTime(o.startsAt) })}</p>
                            <p><strong>${t('labelItems')}</strong> ${o.items.map(i => `${i.inventoryNumber} (${i.categoryName} ${i.size})`).join(', ')}</p>
                        </div>
                        <div>
                            <button class="btn btn-primary btn-sm btn-quick-handout" data-id="${o.id}">
                                ${t('btnQuickHandout')}
                            </button>
                        </div>
                    </div>
                `).join('');

                pickupsEl.querySelectorAll('.btn-quick-handout').forEach(btn => {
                    btn.addEventListener('click', (e) => {
                        const id = e.currentTarget.getAttribute('data-id');
                        openHandoutModal(id);
                    });
                });
            }

            // Expected returns list
            const returnsEl = document.getElementById('saturday-returns-list');
            document.getElementById('saturday-returns-count').textContent = actives.length;

            if (actives.length === 0) {
                returnsEl.innerHTML = `<p class="empty-state">${t('noActiveRentals')}</p>`;
            } else {
                returnsEl.innerHTML = actives.map(o => `
                    <div class="mini-order-item">
                        <div class="mini-order-info">
                            <h4>${o.orderNumber} – ${o.customerName}</h4>
                            <p>${t('returnLine', { deposit: money(o.depositAmount), date: formatDateTime(o.expectedEndAt) })}</p>
                            <p><strong>${t('labelNotReturned')}</strong> ${o.items.filter(i => i.status === 'RENTED').map(i => i.inventoryNumber).join(', ')}</p>
                        </div>
                        <div>
                            <button class="btn btn-secondary btn-sm" onclick="showTabAndFilter('tab-orders', '${o.orderNumber}')">
                                ${t('btnReturnDetail')}
                            </button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (err) {
            console.error(err);
        }
    }

    async function handleExpireUnclaimed() {
        if (!confirm(t('confirmExpire'))) {
            return;
        }

        try {
            const res = await fetch(`${API}/orders/expire-unclaimed`, { method: 'POST' });
            const data = await res.json();
            showToast(t('toastExpired', { count: data.expiredCount }), 'success');
            await refreshAllData();
        } catch (err) {
            showToast(t('errExpire', { message: err.message }), 'error');
        }
    }

    // ================= TAB 2: REZERVACE PO TELEFONU =================

    async function handlePhoneLookup() {
        const phone = document.getElementById('res-phone').value.trim();
        if (!phone) {
            showToast(t('errEnterPhone'), 'error');
            return;
        }

        try {
            const res = await fetch(`${API}/customers/by-phone/${encodeURIComponent(phone)}`);
            if (res.ok) {
                const c = await res.json();
                document.getElementById('res-firstname').value = c.firstName;
                document.getElementById('res-lastname').value = c.lastName;
                document.getElementById('res-idcard').value = c.idCardNumber;
                showToast(t('toastCustomerFound', { name: `${c.firstName} ${c.lastName}` }), 'success');
            } else {
                showToast(t('toastCustomerNotFound'), 'error');
            }
        } catch (err) {
            showToast(t('errCustomerLookup'), 'error');
        }
    }

    function setWeekendDates() {
        // Nejbližší pátek 14:00 až neděle 18:00
        const now = new Date();
        const friday = new Date(now);
        const dayDiff = (5 - now.getDay() + 7) % 7;
        friday.setDate(now.getDate() + (dayDiff === 0 && now.getHours() >= 14 ? 7 : dayDiff));
        friday.setHours(14, 0, 0, 0);

        const sunday = new Date(friday);
        sunday.setDate(friday.getDate() + 2);
        sunday.setHours(18, 0, 0, 0);

        document.getElementById('res-start').value = formatInputDateTime(friday);
        document.getElementById('res-end').value = formatInputDateTime(sunday);
    }

    function setTomorrowDates() {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        tomorrow.setHours(9, 0, 0, 0);

        const tomorrowEnd = new Date(tomorrow);
        tomorrowEnd.setHours(18, 0, 0, 0);

        document.getElementById('res-start').value = formatInputDateTime(tomorrow);
        document.getElementById('res-end').value = formatInputDateTime(tomorrowEnd);
    }

    function addReservationItemRow() {
        const container = document.getElementById('reservation-items-container');
        const count = container.querySelectorAll('.reservation-item-row').length;
        const row = document.createElement('div');
        row.className = 'reservation-item-row';
        row.setAttribute('data-index', count);

        let options = `<option value="">${t('selectCategory')}</option>`;
        categories.forEach(c => {
            options += `<option value="${c.id}">${c.name} (${c.code})</option>`;
        });

        row.innerHTML = `
            <select class="res-item-category" required>${options}</select>
            <input type="text" class="res-item-size" placeholder="${t('phSize')}" data-i18n-placeholder="phSize" required>
            <button type="button" class="btn btn-sm btn-danger btn-remove-res-item" data-i18n="btnRemoveItem">${t('btnRemoveItem')}</button>
        `;

        row.querySelector('.btn-remove-res-item').addEventListener('click', () => row.remove());
        container.appendChild(row);
    }

    async function handleCreateReservation(e) {
        e.preventDefault();

        const phone = document.getElementById('res-phone').value.trim();
        const firstName = document.getElementById('res-firstname').value.trim();
        const lastName = document.getElementById('res-lastname').value.trim();
        const idCard = document.getElementById('res-idcard').value.trim();
        const startVal = document.getElementById('res-start').value;
        const endVal = document.getElementById('res-end').value;
        const discountVal = parseFloat(document.getElementById('res-discount').value) || 0;
        const noteVal = document.getElementById('res-note').value.trim();

        const itemRows = document.querySelectorAll('.reservation-item-row');
        const requestedItems = [];
        itemRows.forEach(r => {
            const catId = r.querySelector('.res-item-category').value;
            const size = r.querySelector('.res-item-size').value.trim();
            if (catId) {
                requestedItems.push({ categoryId: parseInt(catId), size: size });
            }
        });

        if (requestedItems.length === 0) {
            showToast(t('errNoItems'), 'error');
            return;
        }

        const payload = {
            newCustomer: {
                firstName: firstName,
                lastName: lastName,
                phone: phone,
                idCardNumber: idCard
            },
            startsAt: new Date(startVal).toISOString(),
            expectedEndAt: new Date(endVal).toISOString(),
            discountAmount: discountVal,
            note: noteVal,
            requestedItems: requestedItems
        };

        try {
            const res = await fetch(`${API}/orders/reservations`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || t('errCreateReservation'));
            }

            const created = await res.json();
            showToast(t('toastReservationCreated', {
                number: created.orderNumber,
                items: created.items.map(i => i.inventoryNumber).join(', ')
            }), 'success');

            // Reset form
            document.getElementById('form-phone-reservation').reset();
            await refreshAllData();

            // Přejít do záložky výdej a vrácení
            document.querySelector('[data-tab="tab-orders"]').click();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    // ================= TAB 3: VÝDEJ A VRÁCENÍ =================

    async function loadOrders() {
        const status = document.getElementById('filter-order-status').value;
        const container = document.getElementById('orders-list-container');
        try {
            container.innerHTML = `<p class="loading-text">${t('ordersLoading')}</p>`;
            const res = await fetch(`${API}/orders${status ? `?status=${status}` : ''}`);
            const orders = await res.json();

            if (orders.length === 0) {
                container.innerHTML = `<p class="empty-state">${t('noOrders')}</p>`;
                return;
            }

            container.innerHTML = orders.map(o => createOrderCardHtml(o)).join('');
            attachOrderCardEvents();
        } catch (err) {
            container.innerHTML = `<p class="error-text">${t('errGeneric', { message: err.message })}</p>`;
        }
    }

    function createOrderCardHtml(order) {
        const isReservation = order.status === 'RESERVATION';
        const isActive = order.status === 'ACTIVE';

        let itemsHtml = `
            <table class="order-items-table">
                <thead>
                    <tr>
                        <th>${t('colItemInv')}</th>
                        <th>${t('colCategory')}</th>
                        <th>${t('colSize')}</th>
                        <th>${t('colAgreedRate')}</th>
                        <th>${t('colItemPrice')}</th>
                        <th>${t('colItemStatus')}</th>
                        <th>${t('colLateFee')}</th>
                        <th>${t('colCounterActions')}</th>
                    </tr>
                </thead>
                <tbody>
        `;

        order.items.forEach(item => {
            let itemActionHtml = '';
            if (isReservation) {
                itemActionHtml = `<button class="btn btn-secondary btn-sm btn-swap-item" data-order-id="${order.id}" data-item-id="${item.id}" data-inv="${item.inventoryNumber}">${t('btnSwapItem')}</button>`;
            } else if (isActive && item.status === 'RENTED') {
                itemActionHtml = `<button class="btn btn-success btn-sm btn-return-item" data-order-id="${order.id}" data-item-id="${item.id}" data-inv="${item.inventoryNumber}">${t('btnReturnItem')}</button>`;
            }

            itemsHtml += `
                <tr>
                    <td><strong>${item.inventoryNumber}</strong></td>
                    <td>${item.categoryName}</td>
                    <td>${item.size}</td>
                    <td>${t('perDay', { amount: money(item.dailyRateSnapshot) })}</td>
                    <td>${money(item.rentalPrice)}</td>
                    <td>${statusBadge(item.status)}</td>
                    <td style="color:${item.lateFee > 0 ? '#ef4444' : 'inherit'}; font-weight:${item.lateFee > 0 ? '700' : 'normal'}">${money(item.lateFee)}</td>
                    <td>${itemActionHtml}</td>
                </tr>
            `;
        });
        itemsHtml += '</tbody></table>';

        let footerActions = '';
        if (isReservation) {
            footerActions += `
                <button class="btn btn-primary btn-handout-order" data-id="${order.id}">${t('btnHandoutOrder')}</button>
                <button class="btn btn-danger btn-cancel-order" data-id="${order.id}">${t('btnCancelReservation')}</button>
            `;
        }
        footerActions += `<button class="btn btn-secondary btn-print-contract" data-id="${order.id}">${t('btnPrintContractShort')}</button>`;

        return `
            <div class="order-card" id="order-card-${order.id}">
                <div class="order-card-header">
                    <div class="order-title">
                        <h3>${order.orderNumber}</h3>
                        ${statusBadge(order.status)}
                    </div>
                    <div class="order-customer">
                        <strong>${order.customerName}</strong> | ${order.customerPhone} | ${t('labelDocument')} ${order.customerIdCard}
                    </div>
                </div>
                <div class="order-summary-row">
                    <div><strong>${t('labelTerm')}</strong> ${formatDateTime(order.startsAt)} &rarr; ${formatDateTime(order.expectedEndAt)}</div>
                    <div><strong>${t('labelDepositShort')}</strong> ${money(order.depositAmount)}</div>
                    <div><strong>${t('labelDiscount')}</strong> ${money(order.discountAmount)}</div>
                    <div><strong>${t('labelTotal')}</strong> ${money(order.totalPrice)}</div>
                    ${order.totalLateFee > 0 ? `<div style="color:#ef4444;"><strong>${t('labelLateFeeTotal')}</strong> ${money(order.totalLateFee)}</div>` : ''}
                </div>
                ${itemsHtml}
                <div class="order-footer-actions">
                    ${footerActions}
                </div>
            </div>
        `;
    }

    function attachOrderCardEvents() {
        // Handout buttons
        document.querySelectorAll('.btn-handout-order').forEach(btn => {
            btn.addEventListener('click', (e) => openHandoutModal(e.currentTarget.getAttribute('data-id')));
        });

        // Swap buttons
        document.querySelectorAll('.btn-swap-item').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const orderId = e.currentTarget.getAttribute('data-order-id');
                const rentalItemId = e.currentTarget.getAttribute('data-item-id');
                const inv = e.currentTarget.getAttribute('data-inv');
                openSwapModal(orderId, rentalItemId, inv);
            });
        });

        // Return buttons
        document.querySelectorAll('.btn-return-item').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const orderId = e.currentTarget.getAttribute('data-order-id');
                const rentalItemId = e.currentTarget.getAttribute('data-item-id');
                const inv = e.currentTarget.getAttribute('data-inv');
                openReturnModal(orderId, rentalItemId, inv);
            });
        });

        // Cancel order buttons
        document.querySelectorAll('.btn-cancel-order').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (confirm(t('confirmCancelOrder'))) {
                    await cancelOrder(id);
                }
            });
        });

        // Print contract
        document.querySelectorAll('.btn-print-contract').forEach(btn => {
            btn.addEventListener('click', (e) => openPrintContractModal(e.currentTarget.getAttribute('data-id')));
        });
    }

    // ================= MODAL HANDLERS =================

    async function openHandoutModal(orderId) {
        try {
            const res = await fetch(`${API}/orders/${orderId}`);
            const o = await res.json();
            document.getElementById('handout-order-id').value = o.id;
            document.getElementById('handout-info').innerHTML = t('handoutInfo', {
                number: o.orderNumber,
                customer: o.customerName,
                items: o.items.map(i => i.inventoryNumber).join(', '),
                price: money(o.totalPrice)
            });
            // Doporučená kauce = 1000 Kč * počet položek (nebo z ceníku)
            document.getElementById('handout-deposit').value = o.items.length * 1000;
            document.getElementById('handout-discount').value = o.discountAmount || 0;
            document.getElementById('modal-handout').classList.remove('hidden');
        } catch (err) {
            showToast(t('errGeneric', { message: err.message }), 'error');
        }
    }

    async function handleHandoutSubmit(e) {
        e.preventDefault();
        const id = document.getElementById('handout-order-id').value;
        const deposit = parseFloat(document.getElementById('handout-deposit').value) || 0;
        const discount = parseFloat(document.getElementById('handout-discount').value) || 0;
        const note = document.getElementById('handout-note').value.trim();

        try {
            const res = await fetch(`${API}/orders/${id}/handout`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    depositAmount: deposit,
                    discountAmount: discount,
                    note: note
                })
            });
            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || t('errHandout'));
            }
            document.getElementById('modal-handout').classList.add('hidden');
            showToast(t('toastHandedOut'), 'success');
            await refreshAllData();
            loadOrders();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function openSwapModal(orderId, rentalItemId, inv) {
        try {
            document.getElementById('swap-order-id').value = orderId;
            document.getElementById('swap-rental-item-id').value = rentalItemId;
            document.getElementById('swap-info').innerHTML = t('swapInfo', { inv: inv });

            const res = await fetch(`${API}/orders/items/${rentalItemId}/available-swaps`);
            const freeItems = await res.json();
            const select = document.getElementById('swap-new-item');
            select.innerHTML = '';

            if (freeItems.length === 0) {
                select.innerHTML = `<option value="">${t('noSwapItems')}</option>`;
            } else {
                freeItems.forEach(item => {
                    const opt = document.createElement('option');
                    opt.value = item.id;
                    opt.textContent = t('swapOption', { inv: item.inventoryNumber, size: item.size, condition: item.conditionState });
                    select.appendChild(opt);
                });
            }

            document.getElementById('modal-swap').classList.remove('hidden');
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function handleSwapSubmit(e) {
        e.preventDefault();
        const orderId = document.getElementById('swap-order-id').value;
        const rentalItemId = document.getElementById('swap-rental-item-id').value;
        const newItemId = document.getElementById('swap-new-item').value;

        if (!newItemId) {
            showToast(t('errSelectSwap'), 'error');
            return;
        }

        try {
            const res = await fetch(`${API}/orders/${orderId}/items/${rentalItemId}/swap`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ newItemId: parseInt(newItemId) })
            });
            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || t('errSwap'));
            }
            document.getElementById('modal-swap').classList.add('hidden');
            showToast(t('toastSwapped'), 'success');
            loadOrders();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    function openReturnModal(orderId, rentalItemId, inv) {
        document.getElementById('return-order-id').value = orderId;
        document.getElementById('return-item-id').value = rentalItemId;
        document.getElementById('return-info').innerHTML = t('returnInfo', { inv: inv });
        document.getElementById('return-damaged').checked = false;
        document.getElementById('damage-details-group').classList.add('hidden');
        document.getElementById('return-damage-desc').value = '';
        document.getElementById('return-damage-fee').value = 0;
        document.getElementById('return-note').value = '';
        document.getElementById('modal-return').classList.remove('hidden');
    }

    async function handleReturnSubmit(e) {
        e.preventDefault();
        const orderId = document.getElementById('return-order-id').value;
        const itemId = document.getElementById('return-item-id').value;
        const isDamaged = document.getElementById('return-damaged').checked;
        const damageDesc = document.getElementById('return-damage-desc').value.trim();
        const damageFee = parseFloat(document.getElementById('return-damage-fee').value) || 0;
        const note = document.getElementById('return-note').value.trim();

        try {
            const res = await fetch(`${API}/orders/${orderId}/items/${itemId}/return`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    damaged: isDamaged,
                    damageDescription: damageDesc,
                    damagePenaltyFee: damageFee,
                    note: note
                })
            });
            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || t('errReturn'));
            }
            document.getElementById('modal-return').classList.add('hidden');
            showToast(t(isDamaged ? 'toastReturnedDamaged' : 'toastReturned'), 'success');
            await refreshAllData();
            loadOrders();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function cancelOrder(id) {
        try {
            const res = await fetch(`${API}/orders/${id}/cancel`, { method: 'POST' });
            if (!res.ok) throw new Error(t('errCancelOrder'));
            showToast(t('toastOrderCancelled'), 'success');
            await refreshAllData();
            loadOrders();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    // ================= PRINT CONTRACT =================

    async function openPrintContractModal(orderId) {
        try {
            const res = await fetch(`${API}/orders/${orderId}`);
            const o = await res.json();
            const area = document.getElementById('printable-contract-area');

            area.innerHTML = `
                <h2>${t('contractHeading')}</h2>
                <p style="text-align:center; font-size:10pt; margin-bottom:1.5rem;">${t('contractNumber', { number: o.orderNumber })}</p>

                <div class="contract-parties">
                    <div>
                        <strong>${t('contractLessor')}</strong><br>
                        Hory a voda – Martin Řehák<br>
                        ${t('contractCompanyId')} 12345678<br>
                        ${t('contractSeat')} Pec pod Sněžkou 12<br>
                        ${t('labelPhone')} +420 603 123 456
                    </div>
                    <div>
                        <strong>${t('contractLessee')}</strong><br>
                        ${t('labelName')} <strong>${o.customerName}</strong><br>
                        ${t('labelPhone')} ${o.customerPhone}<br>
                        ${t('labelIdOrPassport')} ${o.customerIdCard}
                    </div>
                </div>

                <p><strong>${t('contractSubject')}</strong></p>
                <table class="contract-table">
                    <thead>
                        <tr>
                            <th>${t('colInventoryNumber')}</th>
                            <th>${t('colCategory')}</th>
                            <th>${t('colSize')}</th>
                            <th>${t('colAgreedDailyRate')}</th>
                            <th>${t('colTotalRent')}</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${o.items.map(i => `
                            <tr>
                                <td><strong>${i.inventoryNumber}</strong></td>
                                <td>${i.categoryName}</td>
                                <td>${i.size}</td>
                                <td>${t('perDay', { amount: money(i.dailyRateSnapshot) })}</td>
                                <td>${money(i.rentalPrice)}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>

                <div style="font-size:10pt; margin-bottom:1rem;">
                    ${t('contractPeriod', { from: formatDateTime(o.startsAt), to: formatDateTime(o.expectedEndAt) })}<br>
                    ${t('contractTotal', { total: money(o.totalPrice), discount: money(o.discountAmount) })}<br>
                    ${t('contractDeposit', { deposit: money(o.depositAmount) })}
                </div>

                <p style="font-size:9pt; color:#444;">
                    ${t('contractTerms')}
                </p>

                <div class="contract-signatures">
                    <div class="signature-box">
                        ${t('contractSignLessor')}
                    </div>
                    <div class="signature-box">
                        ${t('contractSignLessee')}
                    </div>
                </div>
            `;

            document.getElementById('modal-print-contract').classList.remove('hidden');
        } catch (err) {
            showToast(t('errContract', { message: err.message }), 'error');
        }
    }

    // ================= TAB 4: INVENTÁŘ A SERVIS =================

    async function loadInventory() {
        try {
            const res = await fetch(`${API}/items`);
            allInventoryItems = await res.json();
            renderFilteredInventory();
        } catch (err) {
            console.error(err);
        }
    }

    function renderFilteredInventory() {
        const catVal = document.getElementById('filter-category').value;
        const statusVal = document.getElementById('filter-status').value;
        const searchVal = document.getElementById('input-search').value.trim().toLowerCase();

        let filtered = allInventoryItems;
        if (catVal) filtered = filtered.filter(i => i.categoryId == catVal);
        if (statusVal) filtered = filtered.filter(i => i.status === statusVal);
        if (searchVal) {
            filtered = filtered.filter(i =>
                i.inventoryNumber.toLowerCase().includes(searchVal) ||
                i.size.toLowerCase().includes(searchVal) ||
                i.categoryName.toLowerCase().includes(searchVal)
            );
        }

        const grid = document.getElementById('cards-grid');
        if (filtered.length === 0) {
            grid.innerHTML = `<p class="empty-state">${t('noItemsMatch')}</p>`;
            return;
        }

        // Kompaktní přehled: kusy seskupené podle kategorie, jeden řádek na kus, skupiny vedle sebe
        const groups = {};
        filtered.forEach(item => (groups[item.categoryName] ||= []).push(item));
        const count = (list, s) => list.filter(i => i.status === s).length;

        grid.innerHTML = Object.keys(groups).sort().map(catName => {
            const list = groups[catName].sort((a, b) => a.inventoryNumber.localeCompare(b.inventoryNumber));
            return `
                <div class="inv-group">
                    <div class="inv-group-header">
                        <h3>${catName}</h3>
                        <span class="inv-group-counts">
                            <span class="cnt-avail" title="${t('statusAvailable')}">${count(list, 'AVAILABLE')}</span>
                            <span class="cnt-service" title="${t('statusInService')}">${count(list, 'IN_SERVICE')}</span>
                            <span class="cnt-rented" title="${t('countRentedTitle')}">${count(list, 'RENTED') + count(list, 'RESERVED')}</span>
                        </span>
                    </div>
                    <table class="inv-table">
                        <tbody>
                            ${list.map(item => `
                                <tr title="${item.conditionState}">
                                    <td class="inv-num">${item.inventoryNumber}</td>
                                    <td>${item.size}</td>
                                    <td class="inv-year">${item.purchaseYear}</td>
                                    <td>${statusBadge(item.status)}</td>
                                    <td class="inv-actions">
                                        ${item.status === 'AVAILABLE' ? `<button class="btn-mini btn-action-service" data-id="${item.id}" data-inv="${item.inventoryNumber}">${t('btnSendToService')}</button>` : ''}
                                        ${item.status === 'IN_SERVICE' ? `<button class="btn-mini btn-action-restore" data-id="${item.id}" data-inv="${item.inventoryNumber}">${t('btnEndService')}</button>` : ''}
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            `;
        }).join('');

        grid.querySelectorAll('.btn-action-service').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                const reason = prompt(t('promptServiceReason'));
                if (reason) {
                    await fetch(`${API}/items/${id}/status`, {
                        method: 'PATCH',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ status: 'IN_SERVICE', note: reason })
                    });
                    showToast(t('toastSentToService'), 'success');
                    await refreshAllData();
                    loadInventory();
                }
            });
        });

        grid.querySelectorAll('.btn-action-restore').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (confirm(t('confirmEndService'))) {
                    await fetch(`${API}/items/${id}/status`, {
                        method: 'PATCH',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ status: 'AVAILABLE', note: t('serviceDoneNote') })
                    });
                    showToast(t('toastServiceEnded'), 'success');
                    await refreshAllData();
                    loadInventory();
                }
            });
        });
    }

    async function handleCreateInventoryItem(e) {
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
            const res = await fetch(`${API}/items`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || t('errSave'));
            }
            document.getElementById('modal-create').classList.add('hidden');
            document.getElementById('form-create-item').reset();
            showToast(t('toastItemCreated'), 'success');
            await refreshAllData();
            loadInventory();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    // ================= TAB 5: ZÁKAZNÍCI =================

    async function loadCustomers(query = '') {
        const tbody = document.getElementById('customers-table-body');
        try {
            const res = await fetch(`${API}/customers${query ? `?query=${encodeURIComponent(query)}` : ''}`);
            const list = await res.json();
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" class="empty-state">${t('noCustomers')}</td></tr>`;
                return;
            }
            tbody.innerHTML = list.map(c => `
                <tr>
                    <td><strong>${c.firstName} ${c.lastName}</strong></td>
                    <td>${c.phone}</td>
                    <td>${c.idCardNumber}</td>
                    <td>${c.note || '—'}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="startReservationForCustomer('${c.phone}', '${c.firstName}', '${c.lastName}', '${c.idCardNumber}')">
                            ${t('btnNewReservation')}
                        </button>
                    </td>
                </tr>
            `).join('');
        } catch (err) {
            console.error(err);
        }
    }

    window.startReservationForCustomer = (phone, fn, ln, op) => {
        document.querySelector('[data-tab="tab-phone-reservation"]').click();
        document.getElementById('res-phone').value = phone;
        document.getElementById('res-firstname').value = fn;
        document.getElementById('res-lastname').value = ln;
        document.getElementById('res-idcard').value = op;
    };

    window.showTabAndFilter = (tabId, orderNumber) => {
        document.querySelector(`[data-tab="${tabId}"]`).click();
        const el = document.getElementById(`order-card-${orderNumber}`);
        if (el) el.scrollIntoView({ behavior: 'smooth' });
    };

    // ================= TAB 6: PŘEHLEDY PRO KLIENTA =================

    async function loadReports() {
        loadOverdueReport();
        loadServiceReport();
        loadUsageReport();
    }

    async function loadOverdueReport() {
        const el = document.getElementById('report-overdue-list');
        try {
            const res = await fetch(`${API}/reports/overdue`);
            const list = await res.json();
            if (list.length === 0) {
                el.innerHTML = `<p class="empty-state">${t('noOverdue')}</p>`;
                return;
            }
            el.innerHTML = list.map(o => `
                <div class="mini-order-item" style="border-left: 3px solid #ef4444;">
                    <div class="mini-order-info">
                        <h4>${o.orderNumber} – ${o.customerName} (${o.customerPhone})</h4>
                        <p>${t('labelExpectedReturn')} <strong>${formatDateTime(o.expectedEndAt)}</strong></p>
                        <p>${t('labelUnreturnedItems')} ${o.items.filter(i => i.status === 'RENTED').map(i => i.inventoryNumber).join(', ')}</p>
                        <p style="color:#ef4444;">${t('labelAccruingFee')} <strong>${money(o.totalLateFee)}</strong></p>
                    </div>
                </div>
            `).join('');
        } catch (err) {
            el.innerHTML = `<p class="error-text">${t('errReport')}</p>`;
        }
    }

    async function loadServiceReport() {
        const el = document.getElementById('report-service-list');
        try {
            const res = await fetch(`${API}/reports/active-services`);
            const list = await res.json();
            if (list.length === 0) {
                el.innerHTML = `<p class="empty-state">${t('noService')}</p>`;
                return;
            }
            el.innerHTML = list.map(s => `
                <div class="mini-order-item" style="border-left: 3px solid #f59e0b;">
                    <div class="mini-order-info">
                        <h4>${s.inventoryNumber} – ${s.categoryName} (${s.size})</h4>
                        <p>${t('labelInServiceSince')} ${formatDateTime(s.startedAt)}</p>
                        <p>${t('labelDefect')} <em>${s.description}</em></p>
                    </div>
                </div>
            `).join('');
        } catch (err) {
            el.innerHTML = `<p class="error-text">${t('errReport')}</p>`;
        }
    }

    async function loadUsageReport() {
        const tbody = document.getElementById('report-usage-table-body');
        try {
            const res = await fetch(`${API}/reports/item-usage`);
            const list = await res.json();
            tbody.innerHTML = list.map(i => `
                <tr>
                    <td><strong>${i.inventoryNumber}</strong></td>
                    <td>${i.categoryName}</td>
                    <td>${i.size}</td>
                    <td>${statusBadge(i.status)}</td>
                    <td><strong>${i.usageCount} &times;</strong></td>
                    <td>${money(i.totalRevenue)}</td>
                </tr>
            `).join('');
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="6">${t('errLoading')}</td></tr>`;
        }
    }

    // ================= TAB 7: CENÍK =================

    function renderPricingTable() {
        const tbody = document.getElementById('pricing-table-body');
        if (priceRules.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-state">${t('noPricing')}</td></tr>`;
            return;
        }
        tbody.innerHTML = priceRules.map(r => `
            <tr>
                <td><strong>${r.categoryName}</strong></td>
                <td>${money(r.dailyRate)}</td>
                <td><strong style="color:var(--status-avail-text);">${money(r.weekendRate)}</strong></td>
                <td>${money(r.depositRate)}</td>
                <td>${formatDate(r.validFrom)}</td>
            </tr>
        `).join('');
    }

    // ================= HELPERS =================

    function formatDate(isoString) {
        if (!isoString) return '—';
        return new Date(isoString).toLocaleDateString(LANGUAGES[currentLang].locale);
    }

    function formatDateTime(isoString) {
        if (!isoString) return '—';
        const d = new Date(isoString);
        const locale = LANGUAGES[currentLang].locale;
        return d.toLocaleDateString(locale) + ' ' + d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
    }

    function formatInputDateTime(date) {
        const pad = (n) => String(n).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }

    function showToast(msg, type = 'success') {
        toast.textContent = msg;
        toast.className = `toast ${type}`;
        setTimeout(() => {
            toast.className = 'toast hidden';
        }, 5000);
    }
});
