'use strict';

(function () {
  if (!App.boot()) return;
  const { api, listOf, getMyCustomer, Session, esc, fmt, icon, toast, openDialog, closeDialog, showFormError, confirmAction, withBusy } = App;
  const mine = Session.isCustomer;
  const $ = id => document.getElementById(id);

  const columns = [
    {
      key: 'type', label: 'Account',
      search: r => `${fmt.label(r.type)} ${r.accountNumber ?? ''}`,
      render: r => `<div class="cell-primary">${esc(fmt.label(r.type))}</div>${r.accountNumber ? `<div class="cell-secondary">No. <span class="num">${esc(r.accountNumber)}</span></div>` : ''}`,
    },
    { key: 'accountId', label: 'Account ID', render: r => fmt.id(r.accountId) },
  ];
  if (!mine) columns.push({ key: 'customerId', label: 'Customer ID', render: r => fmt.id(r.customerId) });
  columns.push(
    { key: 'balance', label: 'Balance', align: 'right', value: r => Number(r.balance) || 0, render: r => fmt.amount(r.balance) },
    {
      key: 'actions', label: '', searchable: false, sortable: false,
      render: r => `
        <button type="button" class="icon-btn" data-action="deposit" aria-label="Deposit into ${esc(r.accountId)}" title="Deposit">${icon('deposit')}</button>
        <button type="button" class="icon-btn" data-action="withdraw" aria-label="Withdraw from ${esc(r.accountId)}" title="Withdraw">${icon('withdraw')}</button>
        <button type="button" class="icon-btn" data-action="card" aria-label="${mine ? 'Request' : 'Issue'} a card for ${esc(r.accountId)}" title="${mine ? 'Request a card' : 'Issue card'}">${icon('card')}</button>
        ${mine ? '' : `<button type="button" class="icon-btn danger" data-action="delete" aria-label="Delete ${esc(r.accountId)}" title="Delete account">${icon('trash')}</button>`}`,
    },
  );

  const table = new DataTable($('accountsTable'), {
    columns,
    search: $('accountSearch'),
    countEl: $('accountCount'),
    countLabel: ['account', 'accounts'],
    emptyTitle: mine ? "You don't have any accounts yet" : 'No accounts yet',
    emptyText: mine ? 'Visit a branch or contact the bank to open one.' : 'Open an account for a customer to see it here.',
    sort: { key: 'balance', dir: 'desc' },
  });

  $('typeFilter').addEventListener('change', e => {
    const type = e.target.value;
    table.setFilter(type ? r => r.type === type : null);
  });

  function showStatement(accounts) {
    const total = accounts.reduce((sum, a) => sum + (Number(a.balance) || 0), 0);
    $('totalLabel').textContent = mine ? 'Total balance' : 'Total held across all accounts';
    $('totalValue').innerHTML = `<span class="cur">$</span>${total.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    $('accountCountValue').textContent = accounts.length;
    $('statement').hidden = accounts.length === 0;
  }

  async function load() {
    table.setLoading();
    try {
      let accounts = listOf(await api('/account'));
      if (mine) {
        const me = await getMyCustomer();
        accounts = me ? accounts.filter(a => a.customerId === me.id) : [];
      }
      table.setData(accounts);
      showStatement(accounts);
    } catch (err) {
      table.setError(err.message, load);
    }
  }

  /* Deposit / withdraw */
  const moneyDialog = $('moneyDialog');
  const moneyForm = $('moneyForm');
  let moneyTarget = null;

  function openMoney(account, kind) {
    moneyTarget = { account, kind };
    moneyForm.reset();
    const deposit = kind === 'deposit';
    $('moneyTitle').textContent = deposit ? 'Deposit' : 'Withdraw';
    $('moneyHint').textContent = `${fmt.label(account.type)} account ${account.accountId}. Balance ${fmt.money(account.balance)}.`;
    $('moneySubmit').textContent = deposit ? 'Deposit' : 'Withdraw';
    openDialog(moneyDialog);
  }

  moneyForm.addEventListener('submit', e => {
    e.preventDefault();
    if (!moneyForm.reportValidity()) return;
    const { account, kind } = moneyTarget;
    const amount = parseFloat($('moneyAmount').value);
    // The account service allows negative balances, so only customers are stopped from overdrawing.
    if (mine && kind === 'withdraw' && amount > (Number(account.balance) || 0)) {
      showFormError(moneyForm, `That's more than the balance of ${fmt.money(account.balance)}.`);
      return;
    }
    withBusy($('moneySubmit'), async () => {
      try {
        const res = await api(`/account/${kind === 'deposit' ? 'credit' : 'debit'}`, {
          method: 'POST',
          body: { accountId: account.accountId, amount },
        });
        closeDialog(moneyDialog);
        toast((res && res.message) || (kind === 'deposit' ? 'Deposit complete.' : 'Withdrawal complete.'));
        load();
      } catch (err) {
        showFormError(moneyForm, err.message);
      }
    });
  });

  /* Card request */
  const cardDialog = $('cardDialog');
  const cardForm = $('cardForm');
  let cardAccount = null;

  function openCard(account) {
    cardAccount = account;
    cardForm.reset();
    $('cardTitle').textContent = mine ? 'Request a card' : 'Issue card';
    $('cardSubmit').textContent = mine ? 'Request card' : 'Issue card';
    $('cardHint').textContent = `For ${fmt.label(account.type).toLowerCase()} account ${account.accountId}. New cards start as pending.`;
    openDialog(cardDialog);
  }

  cardForm.addEventListener('submit', e => {
    e.preventDefault();
    if (!cardForm.reportValidity()) return;
    withBusy($('cardSubmit'), async () => {
      try {
        const res = await api('/account/createCard', {
          method: 'POST',
          body: { accountId: cardAccount.accountId, pin: parseInt($('cardPin').value, 10), cardType: $('cardType').value },
        });
        closeDialog(cardDialog);
        toast((res && res.message) || 'Card requested. It will appear on the Cards page as pending.');
      } catch (err) {
        showFormError(cardForm, err.message);
      }
    });
  });

  /* Open account (staff only) */
  const openBtn = $('openAccountBtn');
  if (openBtn) {
    const dialog = $('openAccountDialog');
    const form = $('openAccountForm');
    openBtn.addEventListener('click', () => { form.reset(); openDialog(dialog); });
    form.addEventListener('submit', e => {
      e.preventDefault();
      if (!form.reportValidity()) return;
      withBusy(form.querySelector('[type=submit]'), async () => {
        try {
          const res = await api('/account/createAccount', {
            method: 'POST',
            body: { customerId: $('newAccCustomer').value.trim(), accountType: $('newAccType').value },
          });
          closeDialog(dialog);
          toast((res && res.message) || 'Account opened.');
          load();
        } catch (err) {
          showFormError(form, err.message);
        }
      });
    });
  }

  table.onAction(async (action, row) => {
    if (action === 'deposit' || action === 'withdraw') openMoney(row, action);
    if (action === 'card') openCard(row);
    if (action === 'delete') {
      const ok = await confirmAction({
        title: `Delete account ${row.accountId}?`,
        message: `It has a balance of ${fmt.money(row.balance)}. This can't be undone.`,
        confirmLabel: 'Delete account',
        danger: true,
      });
      if (!ok) return;
      try {
        const res = await api('/account', { method: 'DELETE', query: { id: row.accountId } });
        toast((res && res.message) || 'Account deleted.');
        load();
      } catch (err) {
        toast(err.message, 'error');
      }
    }
  });

  load();
})();
