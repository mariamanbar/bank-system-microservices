'use strict';

(function () {
  if (!App.boot()) return;
  const { api, listOf, getMyCustomer, Session, esc, fmt, icon, toast, openDialog, closeDialog, showFormError, confirmAction, withBusy } = App;

  if (Session.isCustomer) {
    renderProfile();
    return;
  }

  /* ---------- Customer view: own profile ---------- */

  async function renderProfile() {
    const box = document.getElementById('profile');
    box.innerHTML = '<div class="empty-state"><p>Loading your profile…</p></div>';
    try {
      const me = await getMyCustomer();
      if (!me) {
        box.innerHTML = `<div class="empty-state"><strong>No customer record found</strong>
          <p>There's no customer linked to ${esc(Session.email)}. Ask the bank to check your registration.</p></div>`;
        return;
      }
      const row = (label, value, copy) => `<div><dt>${label}</dt><dd>${value || App.EMPTY}${copy ? `
        <button type="button" class="icon-btn" data-copy="${esc(copy)}" aria-label="Copy ${label.toLowerCase()}" title="Copy">${icon('copy')}</button>` : ''}</dd></div>`;
      box.innerHTML = `
        <div class="profile">
          <div class="profile-main">
            <h2>${esc(me.name)}</h2>
            <p class="email">${esc(me.email)}</p>
            <div class="figure">
              <span class="label">Balance</span>
              <span class="value">${bigMoney(me.balance)}</span>
            </div>
          </div>
          <dl class="details">
            ${row('Customer ID', `<span class="num">${esc(me.id)}</span>`, me.id)}
            ${row('National ID', `<span class="num">${esc(me.natId ?? '')}</span>`)}
            ${row('Phone', `<span class="num">${esc(me.phone ?? '')}</span>`)}
            ${row('Date of birth', me.dob ? fmt.date(me.dob) : '')}
          </dl>
        </div>`;
      box.querySelectorAll('[data-copy]').forEach(btn => btn.addEventListener('click', async () => {
        try {
          await navigator.clipboard.writeText(btn.dataset.copy);
          toast('Customer ID copied.');
        } catch {
          toast("Couldn't copy. Select the ID and copy it manually.", 'error');
        }
      }));
    } catch (err) {
      box.innerHTML = `<div class="empty-state"><strong>Couldn't load your profile</strong><p>${esc(err.message)}</p></div>`;
    }
  }

  function bigMoney(value) {
    const n = Number(value) || 0;
    return `<span class="cur">$</span>${n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
})();

/* ---------- Staff view: customer directory ---------- */

(function () {
  if (!App.Session.token || App.Session.isCustomer) return;
  const { api, listOf, esc, fmt, icon, toast, openDialog, closeDialog, showFormError, confirmAction, withBusy } = App;

  const table = new DataTable(document.getElementById('customersTable'), {
    search: document.getElementById('customerSearch'),
    countEl: document.getElementById('customerCount'),
    countLabel: ['customer', 'customers'],
    emptyTitle: 'No customers yet',
    emptyText: 'Add the first customer to get started.',
    sort: { key: 'name', dir: 'asc' },
    columns: [
      {
        key: 'name', label: 'Customer',
        search: r => `${r.name} ${r.email}`,
        render: r => `<div class="cell-primary">${esc(r.name)}</div><div class="cell-secondary">${esc(r.email)}</div>`,
      },
      { key: 'id', label: 'Customer ID', render: r => fmt.id(r.id) },
      { key: 'natId', label: 'National ID', render: r => fmt.id(r.natId) },
      { key: 'phone', label: 'Phone', render: r => r.phone ? `<span class="num">${esc(r.phone)}</span>` : App.EMPTY },
      { key: 'balance', label: 'Balance', align: 'right', value: r => Number(r.balance) || 0, render: r => fmt.amount(r.balance) },
      {
        key: 'actions', label: '', searchable: false, sortable: false,
        render: r => `
          <button type="button" class="icon-btn" data-action="edit" aria-label="Edit ${esc(r.name)}" title="Edit">${icon('edit')}</button>
          <button type="button" class="icon-btn danger" data-action="delete" aria-label="Delete ${esc(r.name)}" title="Delete">${icon('trash')}</button>`,
      },
    ],
  });

  async function load() {
    table.setLoading();
    try {
      table.setData(listOf(await api('/customer')));
    } catch (err) {
      table.setError(err.message, load);
    }
  }

  /* Dialog */
  const dialog = document.getElementById('customerDialog');
  const form = document.getElementById('customerForm');
  const $ = id => document.getElementById(id);
  const lockable = ['custNatId', 'custPhone', 'custDob'];

  function openForm(customer) {
    form.reset();
    const editing = Boolean(customer);
    $('custId').value = editing ? customer.id : '';
    $('customerDialogTitle').textContent = editing ? 'Edit customer' : 'Add customer';
    $('customerDialogHint').textContent = editing ? `Customer ID ${customer.id}` : 'They can sign in with this email and password.';
    $('customerSubmit').textContent = editing ? 'Save changes' : 'Add customer';
    $('custPasswordField').hidden = editing;
    $('custPassword').required = !editing;
    $('custLockedNote').hidden = !editing;
    lockable.forEach(id => { $(id).disabled = editing; $(id).required = !editing; });
    if (editing) {
      $('custName').value = customer.name || '';
      $('custEmail').value = customer.email || '';
      $('custNatId').value = customer.natId || '';
      $('custPhone').value = customer.phone || '';
      $('custDob').value = customer.dob || '';
      form.dataset.balance = customer.balance ?? 0;
    }
    openDialog(dialog);
  }

  form.addEventListener('submit', e => {
    e.preventDefault();
    if (!form.reportValidity()) return;
    const id = $('custId').value;
    withBusy($('customerSubmit'), async () => {
      try {
        let res;
        if (id) {
          res = await api('/customer', {
            method: 'PUT',
            body: { id, name: $('custName').value.trim(), email: $('custEmail').value.trim(), balance: parseFloat(form.dataset.balance) || 0 },
          });
        } else {
          // Goes through the security service so the password is hashed and the customer can sign in.
          res = await api('/auth/register', {
            method: 'POST',
            body: {
              name: $('custName').value.trim(),
              email: $('custEmail').value.trim(),
              password: $('custPassword').value,
              dob: $('custDob').value,
              phone: $('custPhone').value.trim(),
              natID: $('custNatId').value.trim(),
            },
          });
        }
        closeDialog(dialog);
        toast((res && res.message) || (id ? 'Customer updated.' : 'Customer added.'));
        load();
      } catch (err) {
        showFormError(form, err.message);
      }
    });
  });

  document.getElementById('addCustomerBtn').addEventListener('click', () => openForm(null));

  table.onAction(async (action, row) => {
    if (action === 'edit') openForm(row);
    if (action === 'delete') {
      const ok = await confirmAction({
        title: `Delete ${row.name}?`,
        message: 'This removes the customer record. Their accounts, cards and loans are not deleted automatically.',
        confirmLabel: 'Delete customer',
        danger: true,
      });
      if (!ok) return;
      try {
        const res = await api('/customer', { method: 'DELETE', query: { id: row.id } });
        toast((res && res.message) || 'Customer deleted.');
        load();
      } catch (err) {
        toast(err.message, 'error');
      }
    }
  });

  load();
})();
