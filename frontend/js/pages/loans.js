'use strict';

(function () {
  if (!App.boot()) return;
  const { api, getMyCustomer, Session, esc, fmt, icon, toast, openDialog, closeDialog, showFormError, confirmAction, withBusy } = App;
  const mine = Session.isCustomer;
  const $ = id => document.getElementById(id);

  const TYPES = { INSTANT: 'Instant loan', HOUSING: 'Housing loan', CAR: 'Car loan', COOL_GIRL: 'Cool Girl loan' };
  const STATUS = {
    ACTIVE: { label: 'Active', cls: 'ok' },
    OVERDUE: { label: 'Overdue', cls: 'bad' },
    PAID: { label: 'Paid off', cls: 'done' },
  };

  let currentCustomer = null;

  const table = new DataTable($('loansTable'), {
    pageSize: 20,
    emptyTitle: mine ? "You don't have any loans" : 'No loans for this customer',
    emptyText: mine ? 'Apply for one with the button above.' : 'Issue one with the New loan button.',
    sort: { key: 'loanId', dir: 'desc' },
    columns: [
      {
        key: 'loanId', label: 'Loan', value: r => Number(r.loanId),
        render: r => `<div class="cell-primary">${esc(TYPES[r.loanType] || fmt.label(r.loanType))}</div><div class="cell-secondary">Loan <span class="num">${esc(r.loanId)}</span></div>`,
      },
      { key: 'principalAmount', label: 'Borrowed', align: 'right', value: r => Number(r.principalAmount) || 0, render: r => fmt.amount(r.principalAmount) },
      { key: 'remainingAmount', label: 'Still owed', align: 'right', value: r => Number(r.remainingAmount) || 0, render: r => fmt.amount(r.remainingAmount, { debt: Number(r.remainingAmount) > 0 }) },
      { key: 'installmentAmount', label: 'Installment', align: 'right', value: r => Number(r.installmentAmount) || 0, render: r => fmt.amount(r.installmentAmount) },
      { key: 'nextInstallmentDate', label: 'Next due', render: r => r.status === 'PAID' ? App.EMPTY : fmt.date(r.nextInstallmentDate) },
      {
        key: 'status', label: 'Status',
        render: r => { const s = STATUS[r.status] || { label: fmt.label(r.status), cls: 'done' }; return `<span class="status ${s.cls}">${esc(s.label)}</span>`; },
      },
      {
        key: 'actions', label: '', searchable: false, sortable: false,
        render: r => r.status === 'PAID' ? '' : `
          <button type="button" class="btn btn-secondary btn-sm" data-action="pay" aria-label="Pay installment on loan ${esc(r.loanId)}">Pay</button>
          <button type="button" class="btn btn-ghost btn-sm" data-action="delay" aria-label="Delay installment on loan ${esc(r.loanId)}">Delay</button>`,
      },
    ],
  });

  async function load(customerId) {
    currentCustomer = customerId;
    $('loanView').hidden = false;
    const empty = $('loanEmpty');
    if (empty) empty.hidden = true;
    table.setLoading();
    $('owedValue').innerHTML = '…';
    $('loanCountValue').textContent = '…';
    try {
      const res = await api('/loan', { query: { customerId } });
      const loans = (res && res.loans) || [];
      const owed = Number(res && res.totalDebt) || 0;
      $('owedLabel').textContent = mine ? 'You owe' : `Customer ${customerId} owes`;
      $('owedValue').innerHTML = `<span class="cur">$</span>${owed.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
      $('loanCountValue').textContent = res && res.count !== undefined ? res.count : loans.length;
      table.setData(loans);
    } catch (err) {
      $('owedValue').textContent = '—';
      $('loanCountValue').textContent = '—';
      table.setError(err.message, () => load(customerId));
    }
  }

  /* Staff: look up a customer */
  const lookupForm = $('lookupForm');
  if (lookupForm) {
    lookupForm.addEventListener('submit', e => {
      e.preventDefault();
      const id = $('lookupId').value.trim();
      if (!id) { $('lookupId').focus(); return; }
      load(id);
    });
  }

  /* Customer: load own loans */
  let myId = null;
  if (mine) {
    getMyCustomer().then(me => {
      if (!me) {
        $('loanView').hidden = true;
        document.querySelector('.page-head').insertAdjacentHTML('afterend',
          `<div class="empty-state"><strong>No customer record found</strong><p>There's no customer linked to ${esc(Session.email)}.</p></div>`);
        $('newLoanBtn').disabled = true;
        return;
      }
      myId = me.id;
      load(me.id);
    }).catch(err => toast(err.message, 'error'));
  }

  /* New loan */
  const loanDialog = $('loanDialog');
  const loanForm = $('loanForm');

  $('newLoanBtn').addEventListener('click', () => {
    loanForm.reset();
    const customerField = $('loanCustomer');
    customerField.value = mine ? (myId || '') : (currentCustomer || '');
    customerField.readOnly = mine;
    openDialog(loanDialog);
  });

  loanForm.addEventListener('submit', e => {
    e.preventDefault();
    if (!loanForm.reportValidity()) return;
    const amount = parseFloat($('loanAmount').value);
    const installmentAmount = parseFloat($('loanInstallment').value);
    if (installmentAmount > amount) {
      showFormError(loanForm, "The monthly installment can't be more than the loan amount.");
      return;
    }
    const customerId = $('loanCustomer').value.trim();
    withBusy($('loanSubmit'), async () => {
      try {
        await api('/loan/request', {
          method: 'POST',
          body: { customerId, amount, installmentAmount, loanType: $('loanType').value, timestamp: new Date().toISOString() },
        });
        closeDialog(loanDialog);
        toast(mine ? 'Loan approved. The money has been paid into your account.' : "Loan issued. The money has been paid into the customer's account.");
        if (!mine && $('lookupId')) $('lookupId').value = customerId;
        load(customerId);
      } catch (err) {
        showFormError(loanForm, err.message);
      }
    });
  });

  /* Pay */
  const payDialog = $('payDialog');
  const payForm = $('payForm');
  let payLoan = null;

  payForm.addEventListener('submit', e => {
    e.preventDefault();
    if (!payForm.reportValidity()) return;
    withBusy($('paySubmit'), async () => {
      try {
        const res = await api('/loan/pay', {
          method: 'POST',
          body: { loanId: Number(payLoan.loanId), amount: parseFloat($('payAmount').value) },
        });
        closeDialog(payDialog);
        toast((res && res.message) || 'Payment received.');
        load(currentCustomer);
      } catch (err) {
        showFormError(payForm, err.message);
      }
    });
  });

  table.onAction(async (action, row) => {
    if (action === 'pay') {
      payLoan = row;
      payForm.reset();
      $('payAmount').value = row.installmentAmount || '';
      $('payHint').textContent = `${TYPES[row.loanType] || fmt.label(row.loanType)} ${row.loanId}. ${fmt.money(row.remainingAmount)} still owed.`;
      openDialog(payDialog);
    }

    if (action === 'delay') {
      const ok = await confirmAction({
        title: `Delay the next installment on loan ${row.loanId}?`,
        message: 'Delaying adds a penalty: the interest rate on this loan goes up.',
        confirmLabel: 'Delay installment',
      });
      if (!ok) return;
      try {
        const res = await api('/loan/delay', { method: 'POST', body: { loanId: Number(row.loanId) } });
        toast((res && res.message) || 'Installment delayed.');
        load(currentCustomer);
      } catch (err) {
        toast(err.message, 'error');
      }
    }
  });
})();
