// Secretry JS - Core Application Controller

// Global App State
let state = {
    token: localStorage.getItem('secretry_token') || null,
    user: null,
    currentRole: 'RESIDENT', // Default role toggle on login
    flats: [], // Cache of society flats
    activeTab: 'res-dash' // Active tab identifier
};

// API Base Endpoints helper
const API = {
    auth: {
        login: '/api/auth/login',
        logout: '/api/auth/logout',
        me: '/api/auth/me'
    },
    flats: '/api/flats',
    announcements: '/api/announcements',
    complaints: '/api/complaints',
    polls: '/api/polls',
    visitors: '/api/visitors',
    payments: '/api/payments'
};

// Page Load Initializer
document.addEventListener('DOMContentLoaded', () => {
    startLiveClock();
    
    if (state.token) {
        restoreSession();
    } else {
        showLoginPage();
    }
});

// Live running clock widget
function startLiveClock() {
    const clockEl = document.getElementById('live-time');
    if (!clockEl) return;
    
    const updateTime = () => {
        const now = new Date();
        clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    };
    updateTime();
    setInterval(updateTime, 1000);
}

// REST Client Wrapper with Auth Header Injection
async function request(url, options = {}) {
    if (!options.headers) {
        options.headers = {};
    }
    
    // Inject custom Token session auth header if available
    if (state.token) {
        options.headers['Authorization'] = `Bearer ${state.token}`;
    }
    
    if (options.body && typeof options.body === 'object') {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }
    
    try {
        const response = await fetch(url, options);
        const data = await response.json().catch(() => ({}));
        
        if (!response.ok) {
            // Auto handle session expirations
            if (response.status === 401 && state.token) {
                showToast("Session expired. Please log in again.", "error");
                handleLogout();
            }
            throw new Error(data.error || `HTTP ${response.status}: Failed operation.`);
        }
        return data;
    } catch (err) {
        console.error("API error:", err);
        throw err;
    }
}

// Toast Alerts Generator
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    toast.innerHTML = `
        <span>${message}</span>
        <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;
    
    container.appendChild(toast);
    
    // Auto remove after 4 seconds
    setTimeout(() => {
        toast.style.animation = 'fadeIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Login Role Toggler
function setRole(role) {
    state.currentRole = role;
    document.getElementById('btn-role-resident').classList.toggle('active', role === 'RESIDENT');
    document.getElementById('btn-role-admin').classList.toggle('active', role === 'ADMIN');
    
    const idLabel = document.getElementById('login-id-label');
    if (role === 'ADMIN') {
        idLabel.textContent = "Admin ID";
        document.getElementById('login-id').placeholder = "e.g. admin";
    } else {
        idLabel.textContent = "Email or Flat Number";
        document.getElementById('login-id').placeholder = "e.g. john@gmail.com or A-101";
    }
}

// Fill Login Credentials Helper (demo utility)
function fillDemo(loginId, password, role) {
    setRole(role);
    document.getElementById('login-id').value = loginId;
    document.getElementById('login-password').value = password;
    showToast("Demo credentials filled! Click Enter Portal.", "warning");
}

// Process login submit
async function handleLogin(e) {
    e.preventDefault();
    const loginId = document.getElementById('login-id').value;
    const password = document.getElementById('login-password').value;
    
    try {
        const res = await request(API.auth.login, {
            method: 'POST',
            body: { loginId, password, role: state.currentRole }
        });
        
        state.token = res.token;
        localStorage.setItem('secretry_token', res.token);
        
        showToast(`Welcome ${res.username}!`, "success");
        await restoreSession();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Restore Active Session
async function restoreSession() {
    try {
        const user = await request(API.auth.me);
        state.user = user;
        
        // Setup App UI based on user role
        document.getElementById('user-display-name').textContent = user.username;
        document.getElementById('user-display-role').textContent = user.role;
        document.getElementById('user-avatar-initial').textContent = user.username.charAt(0).toUpperCase();
        
        document.getElementById('login-page').classList.add('hidden');
        document.getElementById('app-shell').classList.remove('hidden');
        
        if (user.role === 'ADMIN') {
            document.getElementById('admin-menu-items').classList.remove('hidden');
            document.getElementById('resident-menu-items').classList.add('hidden');
            document.getElementById('admin-notice-form-card').classList.remove('hidden');
            document.getElementById('admin-poll-form-card').classList.remove('hidden');
            document.getElementById('resident-complaint-form-card').classList.add('hidden');
            
            // Pre-cache flats list for select dropdowns
            await cacheFlats();
            switchTab('admin-dash');
        } else {
            document.getElementById('admin-menu-items').classList.add('hidden');
            document.getElementById('resident-menu-items').classList.remove('hidden');
            document.getElementById('admin-notice-form-card').classList.add('hidden');
            document.getElementById('admin-poll-form-card').classList.add('hidden');
            document.getElementById('resident-complaint-form-card').classList.remove('hidden');
            
            switchTab('res-dash');
        }
    } catch (err) {
        handleLogout();
    }
}

// Logout session
async function handleLogout() {
    if (state.token) {
        await fetch(API.auth.logout, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        }).catch(() => {});
    }
    
    state.token = null;
    state.user = null;
    localStorage.removeItem('secretry_token');
    
    document.getElementById('login-page').classList.remove('hidden');
    document.getElementById('app-shell').classList.add('hidden');
    document.getElementById('login-password').value = '';
    
    showToast("Logged out successfully.", "success");
}

function showLoginPage() {
    document.getElementById('login-page').classList.remove('hidden');
    document.getElementById('app-shell').classList.add('hidden');
}

// Cache flats directory list (Admin only helper)
async function cacheFlats() {
    try {
        state.flats = await request(API.flats);
        populateFlatsSelects();
    } catch (err) {
        console.error("Failed to load flats into memory:", err);
    }
}

// Populate select elements with flats
function populateFlatsSelects() {
    const chargeSelect = document.getElementById('charge-target');
    const guestSelect = document.getElementById('guest-flat');
    
    if (chargeSelect) {
        chargeSelect.innerHTML = '<option value="ALL">All Flats (Monthly Maintenance)</option>';
        state.flats.forEach(f => {
            chargeSelect.innerHTML += `<option value="${f.flatNumber}">Flat ${f.flatNumber} (${f.ownerName})</option>`;
        });
    }
    
    if (guestSelect) {
        guestSelect.innerHTML = '<option value="">-- Select Flat --</option>';
        state.flats.forEach(f => {
            guestSelect.innerHTML += `<option value="${f.flatNumber}">Flat ${f.flatNumber} (${f.ownerName})</option>`;
        });
    }
}

// SPA tab switching logic
function switchTab(tabId) {
    state.activeTab = tabId;
    
    // Set Sidebar Active styles
    document.querySelectorAll('.nav-item').forEach(el => {
        el.classList.remove('active');
    });
    
    // Find matching link based on onclick attribute or text
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        if (item.getAttribute('onclick') && item.getAttribute('onclick').includes(`'${tabId}'`)) {
            item.classList.add('active');
        }
    });

    // Toggle Tab Panels
    document.querySelectorAll('.tab-pane').forEach(panel => {
        panel.classList.remove('active');
    });
    
    const activePanel = document.getElementById(`tab-${tabId}`);
    if (activePanel) {
        activePanel.classList.add('active');
    }
    
    // Set Header Title
    const titles = {
        'res-dash': 'Resident Dashboard',
        'admin-dash': 'Society Administration Portal',
        'admin-flats': 'Flats & Residents database',
        'admin-guests': 'Visitor Entry registry',
        'notices': 'Society Notice Board',
        'polls': 'Decision Voting Board',
        'complaints': 'Residents Issue desk'
    };
    document.getElementById('current-section-title').textContent = titles[tabId] || 'Dashboard';
    
    // Tab dynamic data loading routing
    triggerTabLoad(tabId);
}

// Load appropriate data based on switching
function triggerTabLoad(tabId) {
    switch (tabId) {
        case 'res-dash':
            loadResidentDashboard();
            break;
        case 'admin-dash':
            loadAdminDashboard();
            break;
        case 'admin-flats':
            loadFlatsTable();
            break;
        case 'admin-guests':
            loadVisitorsRegistry();
            break;
        case 'notices':
            loadAnnouncements();
            break;
        case 'polls':
            loadPolls();
            break;
        case 'complaints':
            loadComplaints();
            break;
    }
}

// ---------------------- 1. RESIDENT TAB UTILS ----------------------

async function loadResidentDashboard() {
    try {
        // Fetch fresh profile state to sync balance/member changes
        const user = await request(API.auth.me);
        state.user = user;
        
        document.getElementById('banner-resident-name').textContent = user.username;
        document.getElementById('stat-res-flat').textContent = user.flatNumber;
        document.getElementById('stat-res-members').textContent = user.memberCount;
        
        const balance = user.maintenanceBalance || 0;
        document.getElementById('stat-res-dues').textContent = `$${balance.toFixed(2)}`;
        
        // Show/hide payment components
        const payForm = document.getElementById('dues-payment-form');
        const noDuesAlert = document.getElementById('no-dues-alert');
        
        if (balance <= 0) {
            payForm.classList.add('hidden');
            noDuesAlert.classList.remove('hidden');
        } else {
            payForm.classList.remove('hidden');
            noDuesAlert.classList.add('hidden');
            document.getElementById('payment-due-amount').textContent = `$${balance.toFixed(2)}`;
            document.getElementById('pay-amount').value = balance.toFixed(2);
            document.getElementById('pay-amount').max = balance;
        }
        
        // Load miniature notice list
        const notices = await request(API.announcements);
        const timelineMini = document.getElementById('res-mini-notices');
        
        if (notices.length === 0) {
            timelineMini.innerHTML = '<p class="empty-message">No active alerts posted.</p>';
        } else {
            timelineMini.innerHTML = '';
            notices.slice(0, 3).forEach(n => {
                const isWarning = n.category === 'WARNING';
                const isFestival = n.category === 'FESTIVAL';
                const cardClass = isWarning ? 'notice-card-mini warning' : (isFestival ? 'notice-card-mini festival' : 'notice-card-mini');
                
                timelineMini.innerHTML += `
                    <div class="${cardClass}">
                        <h4>${n.title}</h4>
                        <p style="font-size:0.75rem;color:var(--text-dim); margin-top:2px;">${n.category} • ${formatDate(n.createdAt)}</p>
                    </div>
                `;
            });
        }
    } catch (err) {
        showToast("Failed to refresh resident dashboard: " + err.message, "error");
    }
}

// Process Resident simulated dues payment
async function handlePayDues(e) {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('pay-amount').value);
    const description = document.getElementById('pay-description').value;
    
    try {
        const receipt = await request(`${API.payments}/pay`, {
            method: 'POST',
            body: { amount, description }
        });
        
        showToast(`Payment successful! Transaction ID: ${receipt.transactionId}`, "success");
        document.getElementById('pay-description').value = '';
        
        // Reload dashboard
        loadResidentDashboard();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ---------------------- 2. ADMIN DASHBOARD TAB UTILS ----------------------

async function loadAdminDashboard() {
    try {
        await cacheFlats();
        
        const flatsCount = state.flats.length;
        let totalDues = 0;
        state.flats.forEach(f => totalDues += (f.maintenanceBalance || 0));
        
        const activeGuests = await request(`${API.visitors}?active=true`);
        const openComplaints = await request(API.complaints);
        
        // Update Stats Counters
        document.getElementById('stat-adm-flats').textContent = flatsCount;
        document.getElementById('stat-adm-dues').textContent = `$${totalDues.toFixed(2)}`;
        document.getElementById('stat-adm-guests').textContent = activeGuests.length;
        document.getElementById('stat-adm-complaints').textContent = openComplaints.filter(c => c.status !== 'RESOLVED').length;
        
        // Load Active Guests mini-table
        const guestTbody = document.getElementById('active-guests-mini-tbody');
        if (activeGuests.length === 0) {
            guestTbody.innerHTML = '<tr><td colspan="4" class="empty-td">No active guests checked in.</td></tr>';
        } else {
            guestTbody.innerHTML = '';
            activeGuests.slice(0, 5).forEach(g => {
                guestTbody.innerHTML += `
                    <tr>
                        <td style="font-weight: 500;">${g.guestName}</td>
                        <td class="td-flat">Flat ${g.flatNumber}</td>
                        <td>${formatTime(g.checkInTime)}</td>
                        <td>
                            <button class="danger-btn" onclick="checkoutGuest(${g.id}, true)">Checkout</button>
                        </td>
                    </tr>
                `;
            });
        }
    } catch (err) {
        showToast("Failed to load dashboard metrics: " + err.message, "error");
    }
}

// Admin Process Charge Maintenance
async function handleChargeMaintenance(e) {
    e.preventDefault();
    const flatNumber = document.getElementById('charge-target').value;
    const amount = parseFloat(document.getElementById('charge-amount').value);
    
    try {
        const res = await request(`${API.flats}/charge`, {
            method: 'POST',
            body: { flatNumber, amount }
        });
        
        showToast(res.message, "success");
        document.getElementById('charge-amount').value = '';
        
        // Reload admin metrics
        loadAdminDashboard();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ---------------------- 3. FLAT DIRECTORY TAB (ADMIN ONLY) ----------------------

async function loadFlatsTable() {
    const tbody = document.getElementById('flats-table-tbody');
    tbody.innerHTML = '<tr><td colspan="7" class="empty-td">Loading residents database...</td></tr>';
    
    try {
        await cacheFlats();
        
        if (state.flats.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="empty-td">No flats configured. Click Add New Flat Owner.</td></tr>';
            return;
        }
        
        tbody.innerHTML = '';
        state.flats.forEach(f => {
            tbody.innerHTML += `
                <tr>
                    <td class="td-flat">Flat ${f.flatNumber}</td>
                    <td style="font-weight: 500;">${f.ownerName}</td>
                    <td>${f.email}</td>
                    <td>${f.phoneNumber || 'N/A'}</td>
                    <td>${f.memberCount}</td>
                    <td style="font-weight: 600; color: ${f.maintenanceBalance > 0 ? 'var(--color-danger)' : 'var(--color-success)'}">
                        $${f.maintenanceBalance.toFixed(2)}
                    </td>
                    <td class="actions-cell">
                        <button class="action-icon-btn" onclick="showEditFlatModal(${f.id})" title="Edit flat record">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                        </button>
                        <button class="action-icon-btn" style="color:var(--color-danger)" onclick="deleteFlat(${f.id})" title="Remove flat record">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                        </button>
                    </td>
                </tr>
            `;
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-td style="color:var(--color-danger)">Error: ${err.message}</td></tr>`;
    }
}

// Flat Modal functions
function showAddFlatModal() {
    document.getElementById('flat-modal-title').textContent = "Add Flat Resident";
    document.getElementById('modal-flat-id').value = '';
    document.getElementById('flat-modal-form').reset();
    document.getElementById('modal-flat-number').disabled = false;
    document.getElementById('modal-flat-password').placeholder = "Enter initial password";
    document.getElementById('modal-flat-password').required = true;
    
    document.getElementById('flat-modal').classList.remove('hidden');
}

async function showEditFlatModal(id) {
    try {
        const flat = await request(`${API.flats}/${id}`);
        
        document.getElementById('flat-modal-title').textContent = `Edit Flat ${flat.flatNumber} Record`;
        document.getElementById('modal-flat-id').value = flat.id;
        document.getElementById('modal-flat-number').value = flat.flatNumber;
        document.getElementById('modal-flat-number').disabled = true; // Flat numbers should generally be immutable
        document.getElementById('modal-flat-owner').value = flat.ownerName;
        document.getElementById('modal-flat-email').value = flat.email;
        document.getElementById('modal-flat-password').placeholder = "Leave empty to keep existing password";
        document.getElementById('modal-flat-password').required = false;
        document.getElementById('modal-flat-password').value = '';
        document.getElementById('modal-flat-phone').value = flat.phoneNumber || '';
        document.getElementById('modal-flat-members').value = flat.memberCount;
        document.getElementById('modal-flat-balance').value = flat.maintenanceBalance;
        
        document.getElementById('flat-modal').classList.remove('hidden');
    } catch (err) {
        showToast("Failed to load resident details: " + err.message, "error");
    }
}

function closeFlatModal() {
    document.getElementById('flat-modal').classList.add('hidden');
}

async function handleFlatModalSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('modal-flat-id').value;
    
    const flatData = {
        flatNumber: document.getElementById('modal-flat-number').value,
        ownerName: document.getElementById('modal-flat-owner').value,
        email: document.getElementById('modal-flat-email').value,
        password: document.getElementById('modal-flat-password').value || null,
        phoneNumber: document.getElementById('modal-flat-phone').value,
        memberCount: parseInt(document.getElementById('modal-flat-members').value),
        maintenanceBalance: parseFloat(document.getElementById('modal-flat-balance').value)
    };
    
    try {
        if (id) {
            // Edit existing
            await request(`${API.flats}/${id}`, {
                method: 'PUT',
                body: flatData
            });
            showToast("Resident record updated successfully.", "success");
        } else {
            // Save new
            await request(API.flats, {
                method: 'POST',
                body: flatData
            });
            showToast("New resident flat added successfully.", "success");
        }
        closeFlatModal();
        loadFlatsTable();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function deleteFlat(id) {
    if (!confirm("Are you sure you want to completely remove this flat and resident record? This action is irreversible.")) {
        return;
    }
    try {
        const res = await request(`${API.flats}/${id}`, { method: 'DELETE' });
        showToast(res.message, "success");
        loadFlatsTable();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ---------------------- 4. GUEST REGISTRY TAB (ADMIN ONLY) ----------------------

async function loadVisitorsRegistry() {
    await cacheFlats(); // Sync flat list for selects
    
    const historyTbody = document.getElementById('visitors-history-tbody');
    historyTbody.innerHTML = '<tr><td colspan="6" class="empty-td">Loading visitor logs...</td></tr>';
    
    try {
        const logs = await request(API.visitors);
        
        if (logs.length === 0) {
            historyTbody.innerHTML = '<tr><td colspan="6" class="empty-td">No guest logs found in book.</td></tr>';
            return;
        }
        
        historyTbody.innerHTML = '';
        logs.forEach(g => {
            const isCheckedOut = g.checkOutTime !== null;
            const checkoutText = isCheckedOut ? formatDate(g.checkOutTime) : `<button class="danger-btn" onclick="checkoutGuest(${g.id}, false)">Force Checkout</button>`;
            
            historyTbody.innerHTML += `
                <tr>
                    <td style="font-weight: 500;">${g.guestName}</td>
                    <td>${g.guestPhone}</td>
                    <td class="td-flat">Flat ${g.flatNumber}</td>
                    <td>${g.purpose || 'N/A'}</td>
                    <td>${formatDate(g.checkInTime)}</td>
                    <td>${checkoutText}</td>
                </tr>
            `;
        });
    } catch (err) {
        historyTbody.innerHTML = `<tr><td colspan="6" class="empty-td" style="color:var(--color-danger)">Error: ${err.message}</td></tr>`;
    }
}

// Admin Process check-in
async function handleCheckInGuest(e) {
    e.preventDefault();
    const guestName = document.getElementById('guest-name').value;
    const guestPhone = document.getElementById('guest-phone').value;
    const flatNumber = document.getElementById('guest-flat').value;
    const purpose = document.getElementById('guest-purpose').value;
    
    try {
        await request(`${API.visitors}/checkin`, {
            method: 'POST',
            body: { guestName, guestPhone, flatNumber, purpose }
        });
        
        showToast(`Guest ${guestName} checked in successfully.`, "success");
        document.getElementById('guest-name').value = '';
        document.getElementById('guest-phone').value = '';
        document.getElementById('guest-flat').value = '';
        document.getElementById('guest-purpose').value = '';
        
        // Reload visitors logs
        loadVisitorsRegistry();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Process guest check-out
async function checkoutGuest(id, reloadMiniDashboard = false) {
    try {
        await request(`${API.visitors}/${id}/checkout`, { method: 'POST' });
        showToast("Guest checkout logged successfully.", "success");
        
        if (reloadMiniDashboard) {
            loadAdminDashboard();
        } else {
            loadVisitorsRegistry();
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ---------------------- 5. ANNOUNCEMENTS / NOTICES TAB UTILS ----------------------

async function loadAnnouncements() {
    const timeline = document.getElementById('announcements-timeline');
    timeline.innerHTML = '<p class="empty-message">Loading notice board...</p>';
    
    try {
        const notices = await request(API.announcements);
        
        if (notices.length === 0) {
            timeline.innerHTML = '<p class="empty-message">Notice board is clear. No notices published.</p>';
            return;
        }
        
        timeline.innerHTML = '';
        notices.forEach(n => {
            const cat = n.category.toLowerCase();
            let deleteBtn = '';
            
            // Allow admin to delete announcements
            if (state.user && state.user.role === 'ADMIN') {
                deleteBtn = `
                    <button class="danger-btn" style="position:absolute; top: 1.25rem; right: 1.5rem;" onclick="deleteAnnouncement(${n.id})">
                        Delete Post
                    </button>
                `;
            }
            
            timeline.innerHTML += `
                <div class="notice-card ${cat}">
                    ${deleteBtn}
                    <div class="notice-meta">
                        <div class="notice-meta-left">
                            <span class="notice-badge ${cat}">${n.category}</span>
                        </div>
                        <span class="notice-date">${formatDate(n.createdAt)}</span>
                    </div>
                    <h3 class="notice-title">${n.title}</h3>
                    <p class="notice-desc">${n.content}</p>
                </div>
            `;
        });
    } catch (err) {
        timeline.innerHTML = `<p class="empty-message" style="color:var(--color-danger)">Error loading notice board: ${err.message}</p>`;
    }
}

// Post notice
async function handleCreateAnnouncement(e) {
    e.preventDefault();
    const title = document.getElementById('notice-title').value;
    const category = document.getElementById('notice-category').value;
    const content = document.getElementById('notice-content').value;
    
    try {
        await request(API.announcements, {
            method: 'POST',
            body: { title, category, content }
        });
        
        showToast("Notice published on society board.", "success");
        document.getElementById('notice-title').value = '';
        document.getElementById('notice-content').value = '';
        
        loadAnnouncements();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function deleteAnnouncement(id) {
    if (!confirm("Are you sure you want to delete this notice card from the board?")) return;
    try {
        await request(`${API.announcements}/${id}`, { method: 'DELETE' });
        showToast("Notice post removed.", "success");
        loadAnnouncements();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ---------------------- 6. VOTING POLLS TAB UTILS ----------------------

async function loadPolls() {
    const listContainer = document.getElementById('polls-list-container');
    listContainer.innerHTML = '<p class="empty-message">Loading active polls...</p>';
    
    try {
        const polls = await request(API.polls);
        
        if (polls.length === 0) {
            listContainer.innerHTML = '<p class="empty-message">No active voting sessions running.</p>';
            return;
        }
        
        listContainer.innerHTML = '';
        for (const p of polls) {
            // Load detailed stats and user vote for each poll
            const details = await request(`${API.polls}/${p.id}`);
            
            const isClosed = p.status === 'CLOSED';
            const hasVoted = details.hasVoted;
            
            let statusBadge = isClosed 
                ? '<span class="status-tag resolved">Closed</span>' 
                : '<span class="status-tag pending">Active</span>';
                
            let closeBtnHtml = '';
            if (state.user && state.user.role === 'ADMIN' && !isClosed) {
                closeBtnHtml = `<button class="danger-btn" onclick="closePoll(${p.id})">Close Voting Session</button>`;
            }
            
            let pollInteractionHtml = '';
            
            if (isClosed || hasVoted || (state.user && state.user.role === 'ADMIN')) {
                // Visualize results bar
                pollInteractionHtml = '<div class="poll-results-container">';
                
                const total = details.totalVotes || 1; // Avoid divide by zero
                const results = details.results;
                
                for (const option in results) {
                    const count = results[option];
                    const percent = details.totalVotes > 0 ? ((count / total) * 100) : 0;
                    
                    pollInteractionHtml += `
                        <div class="result-bar-wrapper">
                            <div class="result-label">
                                <span>${option}</span>
                                <span>${count} votes (${percent.toFixed(1)}%)</span>
                            </div>
                            <div class="result-progress-track">
                                <div class="result-progress-fill" style="width: ${percent}%"></div>
                            </div>
                        </div>
                    `;
                }
                
                pollInteractionHtml += `<p class="total-poll-votes">Total cast votes: ${details.totalVotes}</p>`;
                
                if (hasVoted) {
                    pollInteractionHtml += `<div class="poll-voted-badge">You voted option: "${details.userVote}"</div>`;
                }
                
                pollInteractionHtml += '</div>';
            } else {
                // User can vote! Render active option buttons
                pollInteractionHtml = '<div class="poll-voting-options">';
                const optsList = p.options.split(',');
                
                optsList.forEach(opt => {
                    pollInteractionHtml += `
                        <button class="vote-option-btn" onclick="castVote(${p.id}, '${opt.trim()}')">
                            ${opt.trim()}
                        </button>
                    `;
                });
                
                pollInteractionHtml += '</div>';
            }
            
            listContainer.innerHTML += `
                <div class="poll-card ${isClosed ? 'closed' : ''}">
                    <div class="poll-header-row">
                        ${statusBadge}
                        ${closeBtnHtml}
                    </div>
                    <h3 class="poll-question">${p.question}</h3>
                    <p class="poll-desc">${p.description || 'No detailed background provided.'}</p>
                    ${pollInteractionHtml}
                    <div style="margin-top: 1rem; font-size: 0.8rem; color: var(--text-dim);">
                        Posted: ${formatDate(p.createdAt)} 
                        ${p.expiresAt ? `| Expiry: ${formatDate(p.expiresAt)}` : ''}
                    </div>
                </div>
            `;
        }
    } catch (err) {
        listContainer.innerHTML = `<p class="empty-message" style="color:var(--color-danger)">Error loading decisions board: ${err.message}</p>`;
    }
}

// Resident votes
async function castVote(pollId, option) {
    try {
        await request(`${API.polls}/${pollId}/vote`, {
            method: 'POST',
            body: { option }
        });
        showToast("Your vote has been cast. Thank you!", "success");
        loadPolls();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Admin manually closes poll
async function closePoll(id) {
    if (!confirm("Are you sure you want to close this decision voting session permanently?")) return;
    try {
        await request(`${API.polls}/${id}/close`, { method: 'PUT' });
        showToast("Poll closed successfully.", "success");
        loadPolls();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Admin starts poll
async function handleCreatePoll(e) {
    e.preventDefault();
    const question = document.getElementById('poll-question').value;
    const description = document.getElementById('poll-desc').value;
    const options = document.getElementById('poll-options').value;
    const durationDays = parseInt(document.getElementById('poll-duration').value);
    
    try {
        await request(API.polls, {
            method: 'POST',
            body: { question, description, options, durationDays }
        });
        
        showToast("Voting decision poll started successfully.", "success");
        document.getElementById('poll-question').value = '';
        document.getElementById('poll-desc').value = '';
        document.getElementById('poll-options').value = '';
        document.getElementById('poll-duration').value = 7;
        
        loadPolls();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ---------------------- 7. COMPLAINTS TAB UTILS ----------------------

async function loadComplaints() {
    const container = document.getElementById('complaints-grid-container');
    container.innerHTML = '<p class="empty-message">Loading complaints...</p>';
    
    try {
        const list = await request(API.complaints);
        
        if (list.length === 0) {
            container.innerHTML = '<p class="empty-message">No issues currently raised.</p>';
            return;
        }
        
        container.innerHTML = '';
        list.forEach(c => {
            const statusClass = c.status.toLowerCase();
            let adminControls = '';
            
            // Admin status updates selector
            if (state.user && state.user.role === 'ADMIN') {
                adminControls = `
                    <div class="comp-admin-actions">
                        <label>Update Status</label>
                        <select onchange="updateComplaintStatus(${c.id}, this.value)">
                            <option value="PENDING" ${c.status === 'PENDING' ? 'selected' : ''}>Pending</option>
                            <option value="IN_PROGRESS" ${c.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                            <option value="RESOLVED" ${c.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
                        </select>
                    </div>
                `;
            }
            
            container.innerHTML += `
                <div class="complaint-card">
                    <div class="comp-header">
                        <span class="status-tag ${statusClass}">${c.status.replace('_', ' ')}</span>
                        <span style="font-weight:600; font-size:0.8rem; color:var(--color-primary)">Flat ${c.flatNumber}</span>
                    </div>
                    <div class="comp-body">
                        <h3 class="comp-title">${c.title}</h3>
                        <p style="margin-top: 6px; white-space: pre-line;">${c.description}</p>
                    </div>
                    <div class="comp-footer">
                        <span>Filed: ${formatDate(c.createdAt)}</span>
                    </div>
                    ${adminControls}
                </div>
            `;
        });
    } catch (err) {
        container.innerHTML = `<p class="empty-message" style="color:var(--color-danger)">Error loading issues: ${err.message}</p>`;
    }
}

// Resident files issue
async function handleRaiseComplaint(e) {
    e.preventDefault();
    const title = document.getElementById('comp-title').value;
    const description = document.getElementById('comp-desc').value;
    
    try {
        await request(API.complaints, {
            method: 'POST',
            body: { title, description }
        });
        
        showToast("Your complaint has been logged formally. The admin will review it shortly.", "success");
        document.getElementById('comp-title').value = '';
        document.getElementById('comp-desc').value = '';
        
        loadComplaints();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Admin updates complaint status
async function updateComplaintStatus(id, status) {
    try {
        await request(`${API.complaints}/${id}/status`, {
            method: 'PUT',
            body: { status }
        });
        showToast("Complaint status updated successfully.", "success");
        loadComplaints();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ---------------------- COMMON HELPER UTILITIES ----------------------

// Format date helper (YYYY-MM-DDTHH:MM:SS -> May 27, 2026 10:30 AM)
function formatDate(dateTimeStr) {
    if (!dateTimeStr) return 'N/A';
    try {
        const date = new Date(dateTimeStr);
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return dateTimeStr;
    }
}

function formatTime(dateTimeStr) {
    if (!dateTimeStr) return 'N/A';
    try {
        const date = new Date(dateTimeStr);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
        return dateTimeStr;
    }
}
