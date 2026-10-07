/*
 * Mariam Bank: shared frontend code.
 * Every page loads this file first. It replaces jQuery, Bootstrap,
 * BlockUI, PNotify and the old template app.js.
 */
'use strict';

/* ---------- Config ---------- */

// All requests go through the API gateway.
const API_BASE = 'http://localhost:8084';

/* ---------- Session (stored in localStorage, same keys as before) ---------- */

const Session = {
  get token() { return localStorage.getItem('token'); },
  get email() { return localStorage.getItem('userEmail') || ''; },
  get role() { return localStorage.getItem('userRole'); },
  get name() { return localStorage.getItem('userName') || this.email.split('@')[0]; },
  get isCustomer() { return this.role === 'CUSTOMER'; },

  start(res, email) {
    localStorage.setItem('token', res.token);
    localStorage.setItem('userEmail', email);
    localStorage.setItem('userRole', res.role);
    localStorage.setItem('userName', email.split('@')[0]);
  },

  end() {
    ['token', 'userEmail', 'userRole', 'userName', 'myId'].forEach(k => localStorage.removeItem(k));
  },
};

/* ---------- API client ---------- */

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function api(path, { method = 'GET', body, query } = {}) {
  let url = API_BASE + path;
  if (query) {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') params.append(k, v);
    });
    const qs = params.toString();
    if (qs) url += '?' + qs;
  }

  let status, data;

  if (window.DEMO) {
    // Demo mode: answered in the browser by js/demo.js, no backend needed.
    ({ status, data } = await window.DEMO.handle(method, path, query, body));
  } else {
    const headers = {};
    if (Session.token) headers.Authorization = 'Bearer ' + Session.token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let res;
    try {
      res = await fetch(url, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    } catch {
      throw new ApiError("Can't reach the server. Check that the API gateway is running on port 8084.", 0);
    }
    status = res.status;
    const text = await res.text();
    if (text) {
      try { data = JSON.parse(text); } catch { data = text; }
    }
  }

  if (status < 200 || status >= 300) {
    // Expired or invalid token: send the user back to sign in.
    if ((status === 401 || status === 403) && Session.token && !path.startsWith('/auth')) {
      Session.end();
      location.replace('login.html?reason=expired');
    }
    let message = `Request failed (error ${status}).`;
    if (data && typeof data === 'object' && data.message) message = data.message;
    else if (typeof data === 'string' && data.length < 200) message = data;
    throw new ApiError(message, status);
  }
  return data;
}

// Services return either a plain array or { data: [...] }.
function listOf(res) {
  if (Array.isArray(res)) return res;
  if (res && Array.isArray(res.data)) return res.data;
  return [];
}

// The signed-in customer's record (looked up by email). Cached per page load.
let myCustomerPromise = null;
function getMyCustomer() {
  if (!myCustomerPromise) {
    myCustomerPromise = api('/customer')
      .then(res => listOf(res).find(c => c.email === Session.email) || null)
      .catch(err => { myCustomerPromise = null; throw err; });
  }
  return myCustomerPromise;
}

/* ---------- Escaping & formatting ---------- */

function esc(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

const EMPTY = '<span class="empty-cell">—</span>';

const fmt = {
  money(value) {
    const n = Number(value) || 0;
    return '$' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  },

  // Ledger-style amount: muted currency sign, tabular digits.
  amount(value, { debt = false } = {}) {
    const n = Number(value) || 0;
    const digits = Math.abs(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `<span class="amt${debt || n < 0 ? ' debt' : ''}">${n < 0 ? '−' : ''}<span class="cur">$</span>${digits}</span>`;
  },

  date(value) {
    if (!value) return EMPTY;
    const d = new Date(String(value).length === 10 ? value + 'T00:00:00' : value);
    if (isNaN(d)) return esc(value);
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  },

  dateTime(value) {
    if (!value) return EMPTY;
    const d = new Date(value);
    if (isNaN(d)) return esc(value);
    return d.toLocaleString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
    });
  },

  expiry(value) {
    const m = String(value || '').match(/^(\d{4})-(\d{2})/);
    return m ? `${m[2]}/${m[1].slice(2)}` : (value ? esc(value) : EMPTY);
  },

  cardNumber(value) {
    if (!value) return EMPTY;
    return esc(String(value).replace(/\W/g, '').replace(/(.{4})(?=.)/g, '$1 '));
  },

  // LOAN_TYPE -> "Loan type"
  label(value) {
    if (!value) return '';
    const s = String(value).replace(/_/g, ' ').toLowerCase();
    return s.charAt(0).toUpperCase() + s.slice(1);
  },

  id(value) {
    return value === undefined || value === null || value === '' ? EMPTY : `<span class="id">${esc(value)}</span>`;
  },
};

/* ---------- Icons (inline SVG, no icon font needed) ---------- */

const ICONS = {
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6"/><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18 14.3c2.1.7 3.5 2.8 3.5 5.7"/>',
  profile: '<circle cx="12" cy="8" r="4"/><path d="M4 20.5c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"/>',
  wallet: '<path d="M19 7V5.5A1.5 1.5 0 0 0 17.5 4h-12A2.5 2.5 0 0 0 3 6.5v11A2.5 2.5 0 0 0 5.5 20h14a1.5 1.5 0 0 0 1.5-1.5v-10A1.5 1.5 0 0 0 19.5 7H5.5"/><path d="M16.5 13.5h.01"/>',
  card: '<rect x="2.5" y="5" width="19" height="14" rx="2"/><path d="M2.5 10h19M6 15h4"/>',
  loan: '<rect x="2.5" y="6" width="19" height="12" rx="1.5"/><circle cx="12" cy="12" r="2.75"/><path d="M6 9.5v5M18 9.5v5"/>',
  log: '<path d="M8.5 6H20M8.5 12H20M8.5 18H20"/><path d="M4 6h.01M4 12h.01M4 18h.01"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.2-4.2"/>',
  edit: '<path d="M4 20h4L19 9l-4-4L4 16v4Z"/><path d="m13.5 6.5 4 4"/>',
  trash: '<path d="M4 7h16M9.5 7V4.5h5V7M6.5 7l1 13h9l1-13"/>',
  eye: '<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>',
  eyeOff: '<path d="M10.6 5.1A10 10 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-2.8 3.6M6.6 6.6C3.7 8.4 2 12 2 12s3.6 7 10 7c1.9 0 3.5-.6 4.9-1.4"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2M3 3l18 18"/>',
  refresh: '<path d="M20 11a8 8 0 0 0-14.7-4.3L4 8.5M4 4v4.5h4.5M4 13a8 8 0 0 0 14.7 4.3l1.3-1.8M20 20v-4.5h-4.5"/>',
  logout: '<path d="M14 4h4.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H14M10 8l-4 4 4 4M6 12h10"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  close: '<path d="M6 6l12 12M18 6 6 18"/>',
  copy: '<rect x="8.5" y="8.5" width="11.5" height="11.5" rx="2"/><path d="M15.5 8.5V5.5A1.5 1.5 0 0 0 14 4H5.5A1.5 1.5 0 0 0 4 5.5V14a1.5 1.5 0 0 0 1.5 1.5h3"/>',
  deposit: '<path d="M12 4v11M7.5 10.5 12 15l4.5-4.5M4.5 19.5h15"/>',
  withdraw: '<path d="M12 15V4M7.5 8.5 12 4l4.5 4.5M4.5 19.5h15"/>',
  pay: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  key: '<circle cx="8" cy="15" r="4"/><path d="m10.8 12.2 8.7-8.7M16.5 6.5l2.5 2.5M14 9l2 2"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  check: '<circle cx="12" cy="12" r="9"/><path d="m8 12.5 2.8 2.8L16 10"/>',
  alert: '<circle cx="12" cy="12" r="9"/><path d="M12 7.5v5.5M12 16.5h.01"/>',
};

function icon(name, cls = 'icon') {
  return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ''}</svg>`;
}

// <span data-icon="plus"></span> in HTML becomes an SVG.
function hydrateIcons(root = document) {
  root.querySelectorAll('[data-icon]').forEach(el => {
    el.outerHTML = icon(el.dataset.icon);
  });
}

/* ---------- Toasts ---------- */

function toast(message, type = 'success') {
  let box = document.querySelector('.toasts');
  if (!box) {
    box = document.createElement('div');
    box.className = 'toasts';
    box.setAttribute('role', 'status');
    box.setAttribute('aria-live', 'polite');
    document.body.appendChild(box);
  }
  const el = document.createElement('div');
  el.className = 'toast' + (type === 'error' ? ' error' : '');
  el.innerHTML = `${icon(type === 'error' ? 'alert' : 'check')}<p>${esc(message)}</p>
    <button class="icon-btn" type="button" aria-label="Dismiss">${icon('close')}</button>`;
  const remove = () => el.remove();
  el.querySelector('button').addEventListener('click', remove);
  box.appendChild(el);
  setTimeout(remove, type === 'error' ? 7000 : 4500);
}

/* ---------- Dialogs ---------- */

function openDialog(dialog) {
  const form = dialog.querySelector('form');
  dialog.querySelectorAll('.form-error').forEach(e => { e.hidden = true; });
  dialog.showModal();
  const first = form && form.querySelector('input:not([type=hidden]):not([disabled]):not([readonly]), select');
  if (first) first.focus();
}

function closeDialog(dialog) {
  if (dialog.open) dialog.close();
}

function showFormError(form, message) {
  const box = form.querySelector('.form-error');
  if (box) {
    box.textContent = message;
    box.hidden = false;
  } else {
    toast(message, 'error');
  }
}

// Close buttons and backdrop clicks.
document.addEventListener('click', e => {
  const closer = e.target.closest('[data-close]');
  if (closer) closeDialog(closer.closest('dialog'));
  if (e.target.tagName === 'DIALOG') closeDialog(e.target);
});

// A styled replacement for window.confirm(). Resolves true/false.
function confirmAction({ title, message, confirmLabel = 'Confirm', danger = false }) {
  let dlg = document.getElementById('confirmDialog');
  if (!dlg) {
    dlg = document.createElement('dialog');
    dlg.id = 'confirmDialog';
    dlg.innerHTML = `
      <div class="dialog-head"><div><h2></h2><p></p></div></div>
      <div class="dialog-foot">
        <button type="button" class="btn btn-secondary" value="no">Cancel</button>
        <button type="button" class="btn" value="yes"></button>
      </div>`;
    document.body.appendChild(dlg);
  }
  dlg.querySelector('h2').textContent = title;
  dlg.querySelector('p').textContent = message;
  const yes = dlg.querySelector('[value=yes]');
  yes.textContent = confirmLabel;
  yes.className = 'btn ' + (danger ? 'btn-danger' : 'btn-primary');

  return new Promise(resolve => {
    const done = answer => {
      dlg.removeEventListener('close', onClose);
      dlg.querySelectorAll('button').forEach(b => { b.onclick = null; });
      closeDialog(dlg);
      resolve(answer);
    };
    const onClose = () => done(false);
    dlg.querySelector('[value=no]').onclick = () => done(false);
    yes.onclick = () => done(true);
    dlg.addEventListener('close', onClose);
    dlg.showModal();
    yes.focus();
  });
}

/* ---------- Busy state for buttons ---------- */

async function withBusy(button, task) {
  button.disabled = true;
  button.classList.add('is-busy');
  button.setAttribute('aria-busy', 'true');
  try {
    return await task();
  } finally {
    button.disabled = false;
    button.classList.remove('is-busy');
    button.removeAttribute('aria-busy');
  }
}

/* ---------- App shell (sidebar is built here, not copied into every page) ---------- */

const NAV = [
  { page: 'customers', href: 'index.html', label: 'Customers', mine: 'My profile', icon: 'users', mineIcon: 'profile' },
  { page: 'accounts', href: 'accounts.html', label: 'Accounts', mine: 'My accounts', icon: 'wallet' },
  { page: 'cards', href: 'cards.html', label: 'Cards', mine: 'My cards', icon: 'card' },
  { page: 'loans', href: 'loans.html', label: 'Loans', mine: 'My loans', icon: 'loan' },
  { page: 'logs', href: 'logs.html', label: 'Activity log', icon: 'log', staffOnly: true },
];

function signOut() {
  Session.end();
  location.replace('login.html');
}

function renderShell() {
  const page = document.body.dataset.page;
  const mine = Session.isCustomer;

  const items = NAV.filter(n => !(n.staffOnly && mine)).map(n => `
    <li><a href="${n.href}"${n.page === page ? ' aria-current="page"' : ''}>
      ${icon(mine && n.mineIcon ? n.mineIcon : n.icon)}<span>${mine && n.mine ? n.mine : n.label}</span>
    </a></li>`).join('');

  const sidebar = document.createElement('aside');
  sidebar.className = 'sidebar';
  sidebar.id = 'sidebar';
  sidebar.innerHTML = `
    <div class="brand"><a href="index.html">Mariam Bank</a><span>${mine ? 'Online banking' : 'Staff console'}</span></div>
    <nav aria-label="Main"><ul class="nav">${items}</ul></nav>
    <div class="sidebar-user">
      <div class="avatar" aria-hidden="true">${esc(Session.name.slice(0, 2))}</div>
      <div class="who"><strong>${esc(Session.name)}</strong><span>${mine ? 'Customer' : 'Bank staff'}</span></div>
      <button class="icon-btn" type="button" id="signOutBtn" aria-label="Sign out" title="Sign out">${icon('logout')}</button>
    </div>`;

  const topbar = document.createElement('header');
  topbar.className = 'topbar';
  topbar.innerHTML = `
    <button class="icon-btn" type="button" id="menuBtn" aria-label="Open menu" aria-controls="sidebar" aria-expanded="false">${icon('menu')}</button>
    <a href="index.html">Mariam Bank</a>`;

  document.body.prepend(sidebar, topbar);

  if (window.DEMO) {
    const banner = document.createElement('div');
    banner.className = 'demo-banner';
    banner.innerHTML = `<p><strong>Demo mode.</strong> You're using sample data that runs in your browser. Changes last until you close this tab.</p>
      <div>
        <button type="button" class="btn btn-ghost btn-sm" id="demoReset">Reset sample data</button>
        ${window.DEMO.onPages ? '' : '<button type="button" class="btn btn-ghost btn-sm" id="demoExit">Exit demo</button>'}
      </div>`;
    document.querySelector('.main').prepend(banner);
    banner.querySelector('#demoReset').addEventListener('click', () => { window.DEMO.reset(); location.reload(); });
    const exit = banner.querySelector('#demoExit');
    if (exit) exit.addEventListener('click', () => { window.DEMO.exit(); Session.end(); location.replace('login.html'); });
  }

  sidebar.querySelector('#signOutBtn').addEventListener('click', signOut);

  const menuBtn = topbar.querySelector('#menuBtn');
  const setNav = open => {
    document.body.classList.toggle('nav-open', open);
    menuBtn.setAttribute('aria-expanded', String(open));
  };
  menuBtn.addEventListener('click', e => { e.stopPropagation(); setNav(!document.body.classList.contains('nav-open')); });
  document.addEventListener('click', e => {
    if (document.body.classList.contains('nav-open') && !sidebar.contains(e.target)) setNav(false);
  });
  document.addEventListener('keydown', e => { if (e.key === 'Escape') setNav(false); });

  // Page titles: <h1 data-mine="My accounts">Accounts</h1>
  document.querySelectorAll('[data-mine]').forEach(el => {
    if (mine) el.textContent = el.dataset.mine;
  });
  const h1 = document.querySelector('h1');
  if (h1) document.title = `${h1.textContent} | Mariam Bank`;

  // Role-specific elements.
  document.querySelectorAll(mine ? '[data-staff-only]' : '[data-customer-only]').forEach(el => el.remove());
}

// Call at the top of every signed-in page. Returns false if redirecting.
function boot() {
  if (!Session.token) {
    location.replace('login.html');
    return false;
  }
  renderShell();
  hydrateIcons();
  document.body.classList.add('ready');
  return true;
}

// Expose for page scripts.
window.App = {
  api, ApiError, listOf, getMyCustomer, Session, esc, fmt, icon, hydrateIcons,
  toast, openDialog, closeDialog, showFormError, confirmAction, withBusy, boot, EMPTY,
};
