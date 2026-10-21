document.addEventListener('DOMContentLoaded', () => {
    const API = '/api';

    // State
    let categories = [];
    let priceRules = [];
    let allInventoryItems = [];

    // České názvy stavů kusů, položek a objednávek (hodnoty enumů z backendu)
    const STATUS_LABELS = {
        AVAILABLE: 'Volné',
        RESERVED: 'Rezervováno',
        RENTED: 'Půjčeno',
        IN_SERVICE: 'V servisu',
        RETIRED: 'Vyřazeno',
        RETURNED: 'Vráceno',
        DAMAGED: 'Poškozeno',
        RESERVATION: 'Rezervace',
        ACTIVE: 'Aktivní',
        CLOSED: 'Uzavřeno',
        EXPIRED: 'Propadlo',
        CANCELLED: 'Zrušeno'
    };
    const statusLabel = (s) => STATUS_LABELS[s] || s;
    const statusBadge = (s) => `<span class="badge-status badge-${s}">${statusLabel(s)}</span>`;

    // DOM Elements
    const toast = document.getElementById('toast-message');

    // Init
    init();

    async function init() {
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
            showToast('Chyba při načítání kategorií', 'error');
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
            document.querySelector('.res-item-category')
        ];
        selects.forEach(select => {
            if (!select) return;
            select.innerHTML = select.id === 'filter-category'
                ? '<option value="">Všechny kategorie</option>'
                : '<option value="">-- Vyberte kategorii --</option>';
            categories.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat.id;
                opt.textContent = `${cat.name} (${cat.code})`;
                select.appendChild(opt);
            });
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
            container.innerHTML = '<p class="loading-text">Aktualizuji matici volného vybavení...</p>';
            const res = await fetch(`${API}/reports/availability-matrix?date=${dateStr}`);
            const data = await res.json();
            renderMatrixTable(data);
        } catch (err) {
            container.innerHTML = `<p class="error-text">Chyba matice: ${err.message}</p>`;
        }
    }

    function renderMatrixTable(matrixData) {
        const container = document.getElementById('matrix-container');
        const catMap = matrixData.categories || {};
        const catNames = Object.keys(catMap);

        if (catNames.length === 0) {
            container.innerHTML = '<p class="empty-state">Žádné kategorie nenalezeny.</p>';
            return;
        }

        let html = '<table class="matrix-table"><thead><tr><th class="cat-col">Kategorie</th>';
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
                                <span class="matrix-avail">${cell.available} ks</span>
                                <span class="matrix-sub">ze ${cell.total} (servis: ${cell.in_service})</span>
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
                pickupsEl.innerHTML = '<p class="empty-state">Dnes nejsou žádné čekající rezervace.</p>';
            } else {
                pickupsEl.innerHTML = reservations.map(o => `
                    <div class="mini-order-item">
                        <div class="mini-order-info">
                            <h4>${o.orderNumber} – ${o.customerName}</h4>
                            <p>📞 ${o.customerPhone} | Termín: ${formatDateTime(o.startsAt)}</p>
                            <p><strong>Položky:</strong> ${o.items.map(i => `${i.inventoryNumber} (${i.categoryName} ${i.size})`).join(', ')}</p>
                        </div>
                        <div>
                            <button class="btn btn-primary btn-sm btn-quick-handout" data-id="${o.id}">
                                📦 Vydat u pultu
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
                returnsEl.innerHTML = '<p class="empty-state">Žádné aktivní výpůjčky k vrácení.</p>';
            } else {
                returnsEl.innerHTML = actives.map(o => `
                    <div class="mini-order-item">
                        <div class="mini-order-info">
                            <h4>${o.orderNumber} – ${o.customerName}</h4>
                            <p>Kauce: ${o.depositAmount} Kč | Očekáváno do: ${formatDateTime(o.expectedEndAt)}</p>
                            <p><strong>Nevráceno:</strong> ${o.items.filter(i => i.status === 'RENTED').map(i => i.inventoryNumber).join(', ')}</p>
                        </div>
                        <div>
                            <button class="btn btn-secondary btn-sm" onclick="showTabAndFilter('tab-orders', '${o.orderNumber}')">
                                🔍 Detail k vrácení
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
        if (!confirm('Opravdu chcete stornovat všechny nevyzvednuté rezervace s časem před 10:00? Všechny jejich kusy budou uvolněny pro frontu!')) {
            return;
        }

        try {
            const res = await fetch(`${API}/orders/expire-unclaimed`, { method: 'POST' });
            const data = await res.json();
            showToast(`Úspěšně stornováno ${data.expiredCount} nevyzvednutých rezervací. Kusy jsou uvolněny!`, 'success');
            await refreshAllData();
        } catch (err) {
            showToast('Chyba při expiraci: ' + err.message, 'error');
        }
    }

    // ================= TAB 2: REZERVACE PO TELEFONU =================

    async function handlePhoneLookup() {
        const phone = document.getElementById('res-phone').value.trim();
        if (!phone) {
            showToast('Zadejte nejprve telefonní číslo.', 'error');
            return;
        }

        try {
            const res = await fetch(`${API}/customers/by-phone/${encodeURIComponent(phone)}`);
            if (res.ok) {
                const c = await res.json();
                document.getElementById('res-firstname').value = c.firstName;
                document.getElementById('res-lastname').value = c.lastName;
                document.getElementById('res-idcard').value = c.idCardNumber;
                showToast(`Zákazník nalezen: ${c.firstName} ${c.lastName}`, 'success');
            } else {
                showToast('Zákazník s tímto telefonem zatím v databázi není. Vyplňte jméno a doklad pro automatické založení.', 'error');
            }
        } catch (err) {
            showToast('Chyba při hledání zákazníka', 'error');
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

        let options = '<option value="">-- Vyberte kategorii --</option>';
        categories.forEach(c => {
            options += `<option value="${c.id}">${c.name} (${c.code})</option>`;
        });

        row.innerHTML = `
            <select class="res-item-category" required>${options}</select>
            <input type="text" class="res-item-size" placeholder="Velikost (např. 170 cm)" required>
            <button type="button" class="btn btn-sm btn-danger btn-remove-res-item">&times; Odebrat</button>
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
            showToast('Zvolte alespoň jednu položku k rezervaci.', 'error');
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
                throw new Error(err.message || 'Chyba při vytváření rezervace.');
            }

            const created = await res.json();
            showToast(`Rezervace ${created.orderNumber} úspěšně vytvořena! Alokované kusy: ${created.items.map(i => i.inventoryNumber).join(', ')}`, 'success');

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
            container.innerHTML = '<p class="loading-text">Načítám objednávky...</p>';
            const res = await fetch(`${API}/orders${status ? `?status=${status}` : ''}`);
            const orders = await res.json();

            if (orders.length === 0) {
                container.innerHTML = '<p class="empty-state">Žádné objednávky v tomto stavu.</p>';
                return;
            }

            container.innerHTML = orders.map(o => createOrderCardHtml(o)).join('');
            attachOrderCardEvents();
        } catch (err) {
            container.innerHTML = `<p class="error-text">Chyba: ${err.message}</p>`;
        }
    }

    function createOrderCardHtml(order) {
        const isReservation = order.status === 'RESERVATION';
        const isActive = order.status === 'ACTIVE';

        let itemsHtml = `
            <table class="order-items-table">
                <thead>
                    <tr>
                        <th>Kus (Inv. č.)</th>
                        <th>Kategorie</th>
                        <th>Velikost</th>
                        <th>Sjednaná sazba</th>
                        <th>Cena položky</th>
                        <th>Stav položky</th>
                        <th>Penále</th>
                        <th>Akce u pultu</th>
                    </tr>
                </thead>
                <tbody>
        `;

        order.items.forEach(item => {
            let itemActionHtml = '';
            if (isReservation) {
                itemActionHtml = `<button class="btn btn-secondary btn-sm btn-swap-item" data-order-id="${order.id}" data-item-id="${item.id}" data-inv="${item.inventoryNumber}">🔄 Vyměnit kus</button>`;
            } else if (isActive && item.status === 'RENTED') {
                itemActionHtml = `<button class="btn btn-success btn-sm btn-return-item" data-order-id="${order.id}" data-item-id="${item.id}" data-inv="${item.inventoryNumber}">📥 Vrátit kus</button>`;
            }

            itemsHtml += `
                <tr>
                    <td><strong>${item.inventoryNumber}</strong></td>
                    <td>${item.categoryName}</td>
                    <td>${item.size}</td>
                    <td>${item.dailyRateSnapshot} Kč za den</td>
                    <td>${item.rentalPrice} Kč</td>
                    <td>${statusBadge(item.status)}</td>
                    <td style="color:${item.lateFee > 0 ? '#ef4444' : 'inherit'}; font-weight:${item.lateFee > 0 ? '700' : 'normal'}">${item.lateFee} Kč</td>
                    <td>${itemActionHtml}</td>
                </tr>
            `;
        });
        itemsHtml += '</tbody></table>';

        let footerActions = '';
        if (isReservation) {
            footerActions += `
                <button class="btn btn-primary btn-handout-order" data-id="${order.id}">📦 Vydat zákazníkovi</button>
                <button class="btn btn-danger btn-cancel-order" data-id="${order.id}">❌ Zrušit rezervaci</button>
            `;
        }
        footerActions += `<button class="btn btn-secondary btn-print-contract" data-id="${order.id}">🖨️ Tisk smlouvy</button>`;

        return `
            <div class="order-card" id="order-card-${order.id}">
                <div class="order-card-header">
                    <div class="order-title">
                        <h3>${order.orderNumber}</h3>
                        ${statusBadge(order.status)}
                    </div>
                    <div class="order-customer">
                        👤 <strong>${order.customerName}</strong> | 📞 ${order.customerPhone} | Doklad: ${order.customerIdCard}
                    </div>
                </div>
                <div class="order-summary-row">
                    <div><strong>Termín:</strong> ${formatDateTime(order.startsAt)} &rarr; ${formatDateTime(order.expectedEndAt)}</div>
                    <div><strong>Kauce:</strong> ${order.depositAmount} Kč</div>
                    <div><strong>Sleva:</strong> ${order.discountAmount} Kč</div>
                    <div><strong>Celkem k úhradě:</strong> ${order.totalPrice} Kč</div>
                    ${order.totalLateFee > 0 ? `<div style="color:#ef4444;"><strong>Penále za prodlení:</strong> ${order.totalLateFee} Kč</div>` : ''}
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
                if (confirm('Opravdu chcete stornovat tuto rezervaci?')) {
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
            document.getElementById('handout-info').innerHTML = `
                Vydáváte objednávku <strong>${o.orderNumber}</strong> pro <strong>${o.customerName}</strong>.<br>
                Položky: ${o.items.map(i => i.inventoryNumber).join(', ')}<br>
                Základní cena výpůjčky: <strong>${o.totalPrice} Kč</strong>
            `;
            // Doporučená kauce = 1000 Kč * počet položek (nebo z ceníku)
            document.getElementById('handout-deposit').value = o.items.length * 1000;
            document.getElementById('handout-discount').value = o.discountAmount || 0;
            document.getElementById('modal-handout').classList.remove('hidden');
        } catch (err) {
            showToast('Chyba: ' + err.message, 'error');
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
                throw new Error(err.message || 'Výdej se nezdařil.');
            }
            document.getElementById('modal-handout').classList.add('hidden');
            showToast('Objednávka úspěšně vydána zákazníkovi!', 'success');
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
            document.getElementById('swap-info').innerHTML = `Výměna kusu <strong>${inv}</strong> za jiný volný kus stejné kategorie a velikosti.`;

            const res = await fetch(`${API}/orders/items/${rentalItemId}/available-swaps`);
            const freeItems = await res.json();
            const select = document.getElementById('swap-new-item');
            select.innerHTML = '';

            if (freeItems.length === 0) {
                select.innerHTML = '<option value="">-- Žádný jiný volný kus v tomto termínu --</option>';
            } else {
                freeItems.forEach(item => {
                    const opt = document.createElement('option');
                    opt.value = item.id;
                    opt.textContent = `${item.inventoryNumber} – velikost ${item.size} (${item.conditionState})`;
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
            showToast('Vyberte platný náhradní kus.', 'error');
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
                throw new Error(err.message || 'Výměna se nezdařila.');
            }
            document.getElementById('modal-swap').classList.add('hidden');
            showToast('Kus byl úspěšně vyměněn!', 'success');
            loadOrders();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    function openReturnModal(orderId, rentalItemId, inv) {
        document.getElementById('return-order-id').value = orderId;
        document.getElementById('return-item-id').value = rentalItemId;
        document.getElementById('return-info').innerHTML = `Zaznamenáváte vrácení kusu <strong>${inv}</strong>.`;
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
                throw new Error(err.message || 'Vrácení se nezdařilo.');
            }
            document.getElementById('modal-return').classList.add('hidden');
            showToast('Kus byl úspěšně vrácen' + (isDamaged ? ' a odeslán do servisu!' : '.'), 'success');
            await refreshAllData();
            loadOrders();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }

    async function cancelOrder(id) {
        try {
            const res = await fetch(`${API}/orders/${id}/cancel`, { method: 'POST' });
            if (!res.ok) throw new Error('Nelze zrušit objednávku.');
            showToast('Objednávka byla zrušena a kusy uvolněny.', 'success');
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
                <h2>SMLOUVA O VÝPŮJČCE SPORTOVNÍHO VYBAVENÍ</h2>
                <p style="text-align:center; font-size:10pt; margin-bottom:1.5rem;">Číslo smlouvy: <strong>${o.orderNumber}</strong></p>

                <div class="contract-parties">
                    <div>
                        <strong>PŮJČOVNA (Pronajímatel):</strong><br>
                        Hory a voda – Martin Řehák<br>
                        IČO: 12345678<br>
                        Sídlo: Pec pod Sněžkou 12<br>
                        Telefon: +420 603 123 456
                    </div>
                    <div>
                        <strong>ZÁKAZNÍK (Nájemce):</strong><br>
                        Jméno: <strong>${o.customerName}</strong><br>
                        Telefon: ${o.customerPhone}<br>
                        Číslo OP nebo pasu: ${o.customerIdCard}
                    </div>
                </div>

                <p><strong>Předmět výpůjčky (Vypůjčené položky):</strong></p>
                <table class="contract-table">
                    <thead>
                        <tr>
                            <th>Inventární číslo</th>
                            <th>Kategorie</th>
                            <th>Velikost</th>
                            <th>Sjednaná denní sazba</th>
                            <th>Celkem nájemné</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${o.items.map(i => `
                            <tr>
                                <td><strong>${i.inventoryNumber}</strong></td>
                                <td>${i.categoryName}</td>
                                <td>${i.size}</td>
                                <td>${i.dailyRateSnapshot} Kč za den</td>
                                <td>${i.rentalPrice} Kč</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>

                <div style="font-size:10pt; margin-bottom:1rem;">
                    <strong>Doba trvání výpůjčky:</strong> Od ${formatDateTime(o.startsAt)} do ${formatDateTime(o.expectedEndAt)}<br>
                    <strong>Celková cena výpůjčky:</strong> ${o.totalPrice} Kč (Sleva: ${o.discountAmount} Kč)<br>
                    <strong>Složená hotovostní jistina (kauce):</strong> ${o.depositAmount} Kč
                </div>

                <p style="font-size:9pt; color:#444;">
                    Nájemce stvrzuje převzetí výše uvedeného vybavení v bezvadném technickém stavu.
                    Zavazuje se vrátit vybavení do dohodnutého termínu. Za pozdní vrácení se účtuje penále za každý započatý den.
                    V případě poškození či ztráty bude vzniklá škoda stržena ze složené jistiny.
                </p>

                <div class="contract-signatures">
                    <div class="signature-box">
                        Za půjčovnu: Martin Řehák
                    </div>
                    <div class="signature-box">
                        Převzal nájemce (podpis)
                    </div>
                </div>
            `;

            document.getElementById('modal-print-contract').classList.remove('hidden');
        } catch (err) {
            showToast('Chyba při přípravě smlouvy: ' + err.message, 'error');
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
            grid.innerHTML = '<p class="empty-state">Žádné kusy neodpovídají filtrům.</p>';
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
                            <span class="cnt-avail" title="Volné">${count(list, 'AVAILABLE')}</span>
                            <span class="cnt-service" title="V servisu">${count(list, 'IN_SERVICE')}</span>
                            <span class="cnt-rented" title="Půjčeno nebo rezervováno">${count(list, 'RENTED') + count(list, 'RESERVED')}</span>
                        </span>
                    </div>
                    <table class="inv-table">
                        <tbody>
                            ${list.map(item => `
                                <tr>
                                    <td class="inv-num">${item.inventoryNumber}</td>
                                    <td>${item.size}</td>
                                    <td class="inv-year">${item.purchaseYear}</td>
                                    <td>${statusBadge(item.status)}</td>
                                    <td class="inv-note" title="${item.conditionState}">${item.conditionState}</td>
                                    <td class="inv-actions">
                                        ${item.status === 'AVAILABLE' ? `<button class="btn-icon btn-action-service" data-id="${item.id}" data-inv="${item.inventoryNumber}" title="Odeslat do servisu">🛠️</button>` : ''}
                                        ${item.status === 'IN_SERVICE' ? `<button class="btn-icon btn-action-restore" data-id="${item.id}" data-inv="${item.inventoryNumber}" title="Ukončit servis">✅</button>` : ''}
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
                const reason = prompt('Zadejte důvod odeslání do servisu:');
                if (reason) {
                    await fetch(`${API}/items/${id}/status`, {
                        method: 'PATCH',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ status: 'IN_SERVICE', note: reason })
                    });
                    showToast('Kus byl přesunut do servisu.', 'success');
                    await refreshAllData();
                    loadInventory();
                }
            });
        });

        grid.querySelectorAll('.btn-action-restore').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (confirm('Ukončit servis a vrátit kus mezi volné?')) {
                    await fetch(`${API}/items/${id}/status`, {
                        method: 'PATCH',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ status: 'AVAILABLE', note: 'Servis hotov' })
                    });
                    showToast('Kus vrácen do fondu volných kusů.', 'success');
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
                throw new Error(err.message || 'Chyba při ukládání.');
            }
            document.getElementById('modal-create').classList.add('hidden');
            document.getElementById('form-create-item').reset();
            showToast('Nový kus zaevidován do inventáře!', 'success');
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
                tbody.innerHTML = '<tr><td colspan="5" class="empty-state">Žádní zákazníci nenalezeni.</td></tr>';
                return;
            }
            tbody.innerHTML = list.map(c => `
                <tr>
                    <td><strong>${c.firstName} ${c.lastName}</strong></td>
                    <td>📞 ${c.phone}</td>
                    <td>${c.idCardNumber}</td>
                    <td>${c.note || '—'}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="startReservationForCustomer('${c.phone}', '${c.firstName}', '${c.lastName}', '${c.idCardNumber}')">
                            📞 Nová rezervace
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
                el.innerHTML = '<p class="empty-state">Výborně! Žádné výpůjčky nejsou po termínu.</p>';
                return;
            }
            el.innerHTML = list.map(o => `
                <div class="mini-order-item" style="border-left: 3px solid #ef4444;">
                    <div class="mini-order-info">
                        <h4>${o.orderNumber} – ${o.customerName} (📞 ${o.customerPhone})</h4>
                        <p>Očekávané vrácení: <strong>${formatDateTime(o.expectedEndAt)}</strong></p>
                        <p>Nevrácené kusy: ${o.items.filter(i => i.status === 'RENTED').map(i => i.inventoryNumber).join(', ')}</p>
                        <p style="color:#ef4444;">Naskakující penále: <strong>${o.totalLateFee} Kč</strong></p>
                    </div>
                </div>
            `).join('');
        } catch (err) {
            el.innerHTML = '<p class="error-text">Chyba přehledu</p>';
        }
    }

    async function loadServiceReport() {
        const el = document.getElementById('report-service-list');
        try {
            const res = await fetch(`${API}/reports/active-services`);
            const list = await res.json();
            if (list.length === 0) {
                el.innerHTML = '<p class="empty-state">V dílně momentálně není žádné vybavení.</p>';
                return;
            }
            el.innerHTML = list.map(s => `
                <div class="mini-order-item" style="border-left: 3px solid #f59e0b;">
                    <div class="mini-order-info">
                        <h4>${s.inventoryNumber} – ${s.categoryName} (${s.size})</h4>
                        <p>V servisu od: ${formatDateTime(s.startedAt)}</p>
                        <p>Popis závady: <em>${s.description}</em></p>
                    </div>
                </div>
            `).join('');
        } catch (err) {
            el.innerHTML = '<p class="error-text">Chyba přehledu</p>';
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
                    <td>${i.totalRevenue} Kč</td>
                </tr>
            `).join('');
        } catch (err) {
            tbody.innerHTML = '<tr><td colspan="6">Chyba načítání</td></tr>';
        }
    }

    // ================= TAB 7: CENÍK =================

    function renderPricingTable() {
        const tbody = document.getElementById('pricing-table-body');
        if (priceRules.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="empty-state">Ceník není nastaven.</td></tr>';
            return;
        }
        tbody.innerHTML = priceRules.map(r => `
            <tr>
                <td><strong>${r.categoryName}</strong></td>
                <td>${r.dailyRate} Kč</td>
                <td><strong style="color:var(--status-avail-text);">${r.weekendRate} Kč</strong></td>
                <td>${r.depositRate} Kč</td>
                <td>${r.validFrom}</td>
            </tr>
        `).join('');
    }

    // ================= HELPERS =================

    function formatDateTime(isoString) {
        if (!isoString) return '—';
        const d = new Date(isoString);
        return d.toLocaleDateString('cs-CZ') + ' ' + d.toLocaleTimeString('cs-CZ', { hour: '2-digit', minute: '2-digit' });
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
