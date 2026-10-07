'use strict';

(function () {
  if (!App.boot()) return;
  const { api, listOf, getMyCustomer, Session, esc, fmt, icon, toast, openDialog, closeDialog, showFormError, confirmAction, withBusy } = App;
  const mine = Session.isCustomer;
  const $ = id => document.getElementById(id);

  const STATUS = {
    ACTIVE: { label: 'Active', cls: 'ok' },
    PENDING: { label: 'Pending', cls: 'wait' },
    DECLINED: { label: 'Declined', cls: 'bad' },
  };
  const NETWORK = { VISA: 'Visa', MASTERCARD: 'Mastercard' };
  const shownCvv = new Set();

  const statusCell = r => {
    const s = STATUS[r.status] || { label: fmt.label(r.status) || 'Unknown', cls: 'done' };
    if (mine) return `<span class="status ${s.cls}">${esc(s.label)}</span>`;
    const options = Object.entries(STATUS).map(([value, o]) =>
      `<option value="${value}"${value === r.status ? ' selected' : ''}>${o.label}</option>`).join('');
    return `<label><span class="visually-hidden">Status of card ${esc(r.id)}</span>
      <select class="select status-select ${s.cls}" data-action="status">${options}</select></label>`;
  };

  const columns = [
    {
      key: 'cardNumber', label: 'Card',
      search: r => `${r.cardNumber} ${r.cardType}`,
      render: r => `<div class="cell-primary num">${fmt.cardNumber(r.cardNumber)}</div>
        <div class="cell-secondary">${esc(NETWORK[r.cardType] || r.cardType || '')} card ${esc(r.id)}</div>`,
    },
    { key: 'accountId', label: 'Account ID', render: r => fmt.id(r.accountId) },
  ];
  if (!mine) columns.push({ key: 'customerId', label: 'Customer ID', render: r => fmt.id(r.customerId) });
  columns.push(
    { key: 'expiryDate', label: 'Expires', render: r => `<span class="num">${fmt.expiry(r.expiryDate)}</span>` },
    {
      key: 'cvv', label: 'CVV', sortable: false, searchable: false,
      render: r => {
        const shown = shownCvv.has(r.id);
        return `<button type="button" class="reveal" data-action="cvv" aria-label="${shown ? 'Hide' : 'Show'} CVV">
          ${shown ? esc(r.cvv) : '•••'}${icon(shown ? 'eyeOff' : 'eye')}</button>`;
      },
    },
    { key: 'status', label: 'Status', render: statusCell },
    {
      key: 'actions', label: '', searchable: false, sortable: false,
      render: r => `
        <button type="button" class="icon-btn" data-action="pin" aria-label="View PIN for card ${esc(r.id)}" title="View PIN">${icon('key')}</button>
        ${mine ? '' : `<button type="button" class="icon-btn danger" data-action="revoke" aria-label="Revoke card ${esc(r.id)}" title="Revoke card">${icon('trash')}</button>`}`,
    },
  );

  const table = new DataTable($('cardsTable'), {
    columns,
    search: $('cardSearch'),
    countEl: $('cardCount'),
    countLabel: ['card', 'cards'],
    emptyTitle: mine ? "You don't have any cards yet" : 'No cards yet',
    emptyText: mine ? 'Request one for any of your accounts.' : 'Issue a card from here or from the Accounts page.',
  });

  $('statusFilter').addEventListener('change', e => {
    const status = e.target.value;
    table.setFilter(status ? r => r.status === status : null);
  });

  let myAccounts = [];

  async function load() {
    table.setLoading();
    try {
      let cards = listOf(await api('/card'));
      if (mine) {
        const me = await getMyCustomer();
        cards = me ? cards.filter(c => c.customerId === me.id) : [];
      }
      table.setData(cards);
    } catch (err) {
      table.setError(err.message, load);
    }
  }

  /* Issue / request a card */
  const issueDialog = $('issueDialog');
  const issueForm = $('issueForm');

  $('issueCardBtn').addEventListener('click', async () => {
    issueForm.reset();
    if (mine) {
      const select = $('issueAccountSelect');
      select.innerHTML = '<option value="">Loading your accounts…</option>';
      openDialog(issueDialog);
      try {
        const me = await getMyCustomer();
        myAccounts = me ? listOf(await api('/account')).filter(a => a.customerId === me.id) : [];
        select.innerHTML = myAccounts.length
          ? myAccounts.map(a => `<option value="${esc(a.accountId)}">${esc(fmt.label(a.type))} account ${esc(a.accountId)}</option>`).join('')
          : '<option value="">You have no accounts to link a card to</option>';
      } catch (err) {
        select.innerHTML = '<option value="">Couldn\'t load accounts</option>';
        showFormError(issueForm, err.message);
      }
    } else {
      openDialog(issueDialog);
    }
  });

  issueForm.addEventListener('submit', e => {
    e.preventDefault();
    const accountId = mine ? $('issueAccountSelect').value : $('issueAccountId').value.trim();
    if (!accountId) {
      showFormError(issueForm, mine ? 'Choose the account this card is for.' : 'Enter the account ID.');
      return;
    }
    if (!issueForm.reportValidity()) return;
    withBusy($('issueSubmit'), async () => {
      try {
        await api('/account/createCard', {
          method: 'POST',
          body: { accountId, pin: parseInt($('issuePin').value, 10), cardType: $('issueType').value },
        });
        closeDialog(issueDialog);
        toast(mine ? 'Card requested. It shows as pending until the bank approves it.' : 'Card issued as pending.');
        // The card is created by the account service in the background; give it a moment.
        setTimeout(load, 1000);
      } catch (err) {
        showFormError(issueForm, err.message);
      }
    });
  });

  /* Row actions */
  table.onAction(async (action, row, el) => {
    if (action === 'cvv') {
      shownCvv.has(row.id) ? shownCvv.delete(row.id) : shownCvv.add(row.id);
      table.render();
    }

    if (action === 'pin') {
      const pin = String(row.pin ?? '');
      $('pinValue').textContent = pin.length && pin.length < 4 ? pin.padStart(4, '0') : pin || '—';
      $('pinHint').textContent = `Card ending ${String(row.cardNumber || '').slice(-4)}. Don't share it with anyone.`;
      openDialog($('pinDialog'));
    }

    if (action === 'status') {
      const status = el.value;
      el.disabled = true;
      try {
        await api('/card/status', { method: 'PATCH', query: { id: row.id, status } });
        row.status = status;
        toast(`Card ${row.id} is now ${STATUS[status].label.toLowerCase()}.`);
        table.render();
      } catch (err) {
        toast(err.message, 'error');
        el.value = row.status;
        el.disabled = false;
      }
    }

    if (action === 'revoke') {
      const ok = await confirmAction({
        title: `Revoke card ending ${String(row.cardNumber || '').slice(-4)}?`,
        message: 'The card stops working immediately and is removed. This can\'t be undone.',
        confirmLabel: 'Revoke card',
        danger: true,
      });
      if (!ok) return;
      try {
        await api('/card', { method: 'DELETE', query: { id: row.id } });
        toast('Card revoked.');
        load();
      } catch (err) {
        toast(err.message, 'error');
      }
    }
  });

  load();
})();
