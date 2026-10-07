/*
 * Demo mode: a fake backend that runs in the browser, so the frontend works
 * without any of the Spring services.
 *
 * Turns on automatically on GitHub Pages, or locally by opening any page with ?demo
 * (for example login.html?demo). Turn it off with ?demo=off.
 * Sample data lives in sessionStorage, so changes last until the tab is closed.
 */
'use strict';

(function () {
  const params = new URLSearchParams(location.search);
  const onPages = location.hostname.endsWith('github.io');
  if (params.get('demo') === 'off') sessionStorage.removeItem('mbDemo');
  else if (params.has('demo')) sessionStorage.setItem('mbDemo', '1');
  if (!onPages && sessionStorage.getItem('mbDemo') !== '1') return;

  const STORE_KEY = 'mbDemoData';
  const STAFF_DOMAIN = '@bank.jo.com';

  /* ---------- Sample data ---------- */

  function isoLocal(date) {
    const p = n => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}`;
  }
  const dateOnly = date => isoLocal(date).slice(0, 10);
  const daysFromNow = n => { const d = new Date(); d.setDate(d.getDate() + n); return d; };
  const minutesAgo = n => new Date(Date.now() - n * 60000);

  function seed() {
    const customers = [
      ['Lina Haddad', 'lina@example.com', '9951100001', '0770001122', '1995-12-24'],
      ['Omar Khalil', 'omar@example.com', '9971098765', '0785550101', '1997-08-02'],
      ['Rania Saleh', 'rania@example.com', '9921045671', '0796612340', '1992-03-15'],
      ['Yousef Nasser', 'yousef@example.com', '9881023456', '0779988776', '1988-11-30'],
      ['Dana Mansour', 'dana@example.com', '0001234567', '0781122334', '2000-06-09'],
      ['Khaled Awad', 'khaled@example.com', '9851009988', '0795566778', '1985-01-21'],
      ['Sara Odeh', 'sara@example.com', '9991076543', '0774433221', '1999-09-12'],
      ['Ahmad Zoubi', 'ahmad@example.com', '9931055443', '0788877665', '1993-04-27'],
      ['Noor Hamdan', 'noor@example.com', '0011022334', '0792233445', '2001-02-18'],
      ['Faris Qasem', 'faris@example.com', '9901044221', '0776655443', '1990-07-05'],
      ['Hala Tamimi', 'hala@example.com', '9961033112', '0783344556', '1996-10-01'],
      ['Tariq Shami', 'tariq@example.com', '9871066778', '0799900112', '1987-05-14'],
    ].map(([name, email, natId, phone, dob], i) => ({
      id: 'CU' + (48201 + i * 137), name, email, natId, phone, dob, balance: [5240.5, 120, 18750, 3300, 0, 920.25, 61200, 450, 15, 7800, 2430.8, 11000][i],
    }));
    const c = i => customers[i].id;

    const accounts = [
      [0, 'CURRENT', 512, 3200.75], [0, 'SAVINGS', 301, 12850], [1, 'SALARY', 555, 1840.5], [2, 'CURRENT', 507, 21400],
      [2, 'DEPOSIT', 214, 50000], [3, 'CURRENT', 509, 640.1], [4, 'SAVINGS', 318, 75], [5, 'SALARY', 561, 3900],
      [6, 'DEPOSIT', 226, 120000], [6, 'CURRENT', 515, 8300.4], [7, 'CURRENT', 520, -40.5], [9, 'SAVINGS', 333, 15600],
      [10, 'SALARY', 572, 2210], [11, 'CURRENT', 523, 9875.25],
    ].map(([ci, type, num, balance], i) => ({ accountId: 'AC' + (70001 + i), customerId: c(ci), accountNumber: num, type, balance }));

    const cards = [
      [0, 0, 'VISA', '4532015112830366', '381', 1234, 1580, 'ACTIVE'],
      [0, 1, 'MASTERCARD', '5425233430109903', '902', 7351, 1810, 'PENDING'],
      [2, 3, 'VISA', '4916338506082832', '144', 2468, 1200, 'ACTIVE'],
      [1, 2, 'MASTERCARD', '5105105105105100', '617', 1357, 950, 'DECLINED'],
      [6, 9, 'VISA', '4111111111111111', '275', 8642, 1700, 'ACTIVE'],
      [11, 13, 'VISA', '4012888888881881', '530', 9090, 1400, 'PENDING'],
    ].map(([ci, ai, cardType, cardNumber, cvv, pin, days, status], i) => ({
      id: i + 1, customerId: c(ci), accountId: accounts[ai].accountId, cardType, cardNumber, cvv, pin, expiryDate: dateOnly(daysFromNow(days)), status,
    }));

    const loans = [
      [0, 'HOUSING', 15000, 9800.4, 650, 0.05, 25, 'ACTIVE'],
      [0, 'INSTANT', 2000, 0, 200, 0.02, null, 'PAID'],
      [0, 'COOL_GIRL', 900, 540, 90, 0.2, -4, 'OVERDUE'],
      [2, 'CAR', 22000, 17600, 800, 0.1, 12, 'ACTIVE'],
      [5, 'INSTANT', 3000, 1250, 300, 0.02, 7, 'ACTIVE'],
    ].map(([ci, loanType, principalAmount, remainingAmount, installmentAmount, interestRate, due, status], i) => ({
      loanId: 101 + i, customerId: c(ci), loanType, principalAmount, remainingAmount, installmentAmount, interestRate,
      nextInstallmentDate: due === null ? null : dateOnly(daysFromNow(due)), status,
    }));

    const logs = [];
    const add = (mins, serviceName, type, customerId, accountId, message) =>
      logs.push({ id: 'LG' + (logs.length + 1), serviceName, type, customerId, accountId, message, timestamp: isoLocal(minutesAgo(mins)) });
    add(2900, 'Customer-Service', 'GENERAL', c(0), null, `Customer ${c(0)} registered with email ${customers[0].email}`);
    add(2880, 'Account-Service', 'GENERAL', c(0), accounts[0].accountId, `Current account ${accounts[0].accountId} opened`);
    add(2400, 'Account-Service', 'TRANSACTIONAL', c(0), accounts[1].accountId, `Credit of $5,000.00 to ${accounts[1].accountId}`);
    add(1500, 'Loan-Service', 'TRANSACTIONAL', c(2), null, 'Car loan of $22,000.00 issued');
    add(900, 'Account-Service', 'TRANSACTIONAL', c(7), accounts[10].accountId, `Debit of $240.50 from ${accounts[10].accountId}`);
    add(600, 'Card-Service', 'GENERAL', c(0), accounts[1].accountId, 'Mastercard requested, pending approval');
    add(320, 'Loan-Service', 'ERROR', c(0), null, 'Installment for loan 103 is overdue');
    add(180, 'Customer-Service', 'GENERAL', c(4), null, `Profile updated for ${customers[4].email}`);
    add(45, 'Account-Service', 'TRANSACTIONAL', c(6), accounts[9].accountId, `Credit of $1,200.00 to ${accounts[9].accountId}`);
    add(12, 'Loan-Service', 'TRANSACTIONAL', c(5), null, 'Installment of $300.00 paid on loan 105');

    return { customers, accounts, cards, loans, logs, next: { customer: 90000, account: 70100, card: 100, loan: 200 } };
  }

  let db;
  try { db = JSON.parse(sessionStorage.getItem(STORE_KEY)); } catch { db = null; }
  if (!db) db = seed();
  const save = () => sessionStorage.setItem(STORE_KEY, JSON.stringify(db));
  save();

  const money = n => '$' + Number(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const log = (serviceName, type, customerId, accountId, message) =>
    db.logs.push({ id: 'LG' + (db.logs.length + 1), serviceName, type, customerId, accountId, message, timestamp: isoLocal(new Date()) });

  const ok = data => ({ status: 200, data });
  const fail = (status, message) => ({ status, data: { message } });

  /* ---------- Routes (same paths and shapes as the real gateway) ---------- */

  function route(method, path, query, body) {
    const b = body || {};
    const findAccount = id => db.accounts.find(a => a.accountId === id);

    if (path === '/auth/login' && method === 'POST') {
      const email = String(b.email || '').toLowerCase();
      const isStaff = email.endsWith(STAFF_DOMAIN);
      if (!b.password || (!isStaff && !db.customers.some(c => c.email.toLowerCase() === email))) return fail(401, 'Invalid credentials');
      return ok({ token: 'demo-token', role: isStaff ? 'ADMIN' : 'CUSTOMER', email });
    }
    if (path === '/auth/register' && method === 'POST') {
      if (db.customers.some(c => c.email.toLowerCase() === String(b.email).toLowerCase())) return fail(400, 'Email is already taken!');
      const customer = { id: 'CU' + db.next.customer++, name: b.name, email: b.email, natId: b.natID, phone: b.phone, dob: b.dob, balance: 0 };
      db.customers.push(customer);
      log('Customer-Service', 'GENERAL', customer.id, null, `Customer ${customer.id} registered with email ${customer.email}`);
      return ok({ message: 'Customer created successfully' });
    }

    if (path === '/customer') {
      if (method === 'GET') return ok({ message: 'List of All Customers', data: db.customers });
      if (method === 'PUT') {
        const c = db.customers.find(x => x.id === b.id);
        if (!c) return fail(404, 'Customer not Found!');
        if (b.name) c.name = b.name;
        if (b.email) c.email = b.email;
        log('Customer-Service', 'GENERAL', c.id, null, `Profile updated for ${c.email}`);
        return ok({ message: 'Customer updated successfully' });
      }
      if (method === 'DELETE') {
        const c = db.customers.find(x => x.id === query.id);
        if (!c) return fail(404, 'Customer not found!');
        db.customers = db.customers.filter(x => x.id !== query.id);
        log('Customer-Service', 'GENERAL', c.id, null, `Customer ${c.id} was deleted`);
        return ok({ message: 'Customer Deleted!' });
      }
    }

    if (path === '/account') {
      if (method === 'GET') {
        const data = query.customerId ? db.accounts.filter(a => a.customerId === query.customerId) : db.accounts;
        return ok({ message: 'List of All Accounts', data });
      }
      if (method === 'DELETE') {
        const a = findAccount(query.id);
        if (!a) return fail(404, 'Account not found!');
        db.accounts = db.accounts.filter(x => x.accountId !== query.id);
        log('Account-Service', 'GENERAL', a.customerId, a.accountId, `Account ${a.accountId} deleted`);
        return ok({ message: 'Account Deleted successfully' });
      }
    }
    if ((path === '/account/credit' || path === '/account/debit') && method === 'POST') {
      const a = findAccount(b.accountId);
      if (!a) return fail(400, 'Account not found!');
      const credit = path.endsWith('credit');
      a.balance = Math.round((a.balance + (credit ? b.amount : -b.amount)) * 100) / 100;
      log('Account-Service', 'TRANSACTIONAL', a.customerId, a.accountId, `${credit ? 'Credit' : 'Debit'} of ${money(b.amount)} ${credit ? 'to' : 'from'} ${a.accountId}`);
      return ok({ message: credit ? 'Credit done successfully' : 'Debit done successfully' });
    }
    if (path === '/account/createAccount' && method === 'POST') {
      const c = db.customers.find(x => x.id === b.customerId);
      if (!c) return fail(400, 'Customer not found!');
      const a = { accountId: 'AC' + db.next.account++, customerId: c.id, accountNumber: 500 + db.accounts.length, type: b.accountType, balance: 0 };
      db.accounts.push(a);
      log('Account-Service', 'GENERAL', c.id, a.accountId, `${b.accountType.charAt(0) + b.accountType.slice(1).toLowerCase()} account ${a.accountId} opened`);
      return ok({ message: 'Account Created successfully' });
    }
    if (path === '/account/createCard' && method === 'POST') {
      const a = findAccount(b.accountId);
      if (!a) return fail(400, 'Account not found!');
      const digits = (b.cardType === 'MASTERCARD' ? '5' : '4') + Array.from({ length: 15 }, () => Math.floor(Math.random() * 10)).join('');
      db.cards.push({
        id: db.next.card++, customerId: a.customerId, accountId: a.accountId, cardType: b.cardType, cardNumber: digits,
        cvv: String(Math.floor(100 + Math.random() * 900)), pin: b.pin, expiryDate: dateOnly(daysFromNow(1825)), status: 'PENDING',
      });
      log('Card-Service', 'GENERAL', a.customerId, a.accountId, `${b.cardType === 'MASTERCARD' ? 'Mastercard' : 'Visa'} requested, pending approval`);
      return ok({ message: 'Card Created successfully' });
    }

    if (path === '/card') {
      if (method === 'GET') return ok(db.cards);
      if (method === 'DELETE') {
        const card = db.cards.find(x => String(x.id) === String(query.id));
        if (!card) return fail(404, 'Card not found');
        db.cards = db.cards.filter(x => x !== card);
        log('Card-Service', 'GENERAL', card.customerId, card.accountId, `Card ending ${card.cardNumber.slice(-4)} revoked`);
        return ok({ message: `Card with ID ${card.id} has been successfully revoked.` });
      }
    }
    if (path === '/card/status' && method === 'PATCH') {
      const card = db.cards.find(x => String(x.id) === String(query.id));
      if (!card) return fail(404, 'Card not found');
      card.status = query.status;
      log('Card-Service', 'GENERAL', card.customerId, card.accountId, `Card ending ${card.cardNumber.slice(-4)} set to ${query.status.toLowerCase()}`);
      return ok({ message: 'Status updated to ' + query.status });
    }

    if (path === '/loan' && method === 'GET') {
      const loans = db.loans.filter(l => l.customerId === query.customerId);
      return ok({ count: loans.length, totalDebt: loans.reduce((s, l) => s + l.remainingAmount, 0), loans });
    }
    if (path === '/loan/request' && method === 'POST') {
      const c = db.customers.find(x => x.id === b.customerId);
      if (!c) return fail(400, 'Customer not found!');
      const rates = { INSTANT: 0.02, HOUSING: 0.05, CAR: 0.1, COOL_GIRL: 0.2 };
      const loan = {
        loanId: db.next.loan++, customerId: c.id, loanType: b.loanType, principalAmount: b.amount,
        remainingAmount: Math.round(b.amount * (1 + rates[b.loanType]) * 100) / 100, installmentAmount: b.installmentAmount,
        interestRate: rates[b.loanType], nextInstallmentDate: dateOnly(daysFromNow(30)), status: 'ACTIVE',
      };
      db.loans.push(loan);
      const account = db.accounts.find(a => a.customerId === c.id);
      if (account) account.balance = Math.round((account.balance + b.amount) * 100) / 100;
      log('Loan-Service', 'TRANSACTIONAL', c.id, account ? account.accountId : null, `${money(b.amount)} loan ${loan.loanId} issued`);
      return ok(loan);
    }
    if (path === '/loan/pay' && method === 'POST') {
      const loan = db.loans.find(l => l.loanId === b.loanId);
      if (!loan) return fail(404, 'Loan not found!');
      if (loan.status === 'PAID') return fail(400, 'No active loans!');
      loan.remainingAmount = Math.max(0, Math.round((loan.remainingAmount - b.amount) * 100) / 100);
      if (loan.remainingAmount === 0) { loan.status = 'PAID'; loan.nextInstallmentDate = null; }
      else { loan.status = 'ACTIVE'; loan.nextInstallmentDate = dateOnly(daysFromNow(30)); }
      log('Loan-Service', 'TRANSACTIONAL', loan.customerId, null, `Installment of ${money(b.amount)} paid on loan ${loan.loanId}`);
      return ok({ message: 'Installment was paid!' });
    }
    if (path === '/loan/delay' && method === 'POST') {
      const loan = db.loans.find(l => l.loanId === b.loanId);
      if (!loan) return fail(404, 'Loan not found!');
      if (loan.status === 'PAID') return fail(400, 'Loan is already paid off, cannot delay!');
      loan.interestRate = Math.round((loan.interestRate + 0.01) * 100) / 100;
      loan.remainingAmount = Math.round(loan.remainingAmount * 1.01 * 100) / 100;
      if (loan.nextInstallmentDate) {
        const d = new Date(loan.nextInstallmentDate + 'T00:00:00');
        d.setDate(d.getDate() + 30);
        loan.nextInstallmentDate = dateOnly(d);
      }
      log('Loan-Service', 'TRANSACTIONAL', loan.customerId, null, `Installment on loan ${loan.loanId} delayed, interest raised`);
      return ok({ message: 'Payment delayed successfully. Interest rate increased.' });
    }

    if (path === '/logger' && method === 'GET') return ok(db.logs);

    return fail(404, `No demo route for ${method} ${path}`);
  }

  window.DEMO = {
    onPages,
    // Same signature the API client uses; resolves to { status, data } after a short, realistic delay.
    handle(method, path, query, body) {
      return new Promise(resolve => {
        setTimeout(() => {
          const result = route(method, path, query || {}, body ? JSON.parse(JSON.stringify(body)) : null);
          save();
          resolve(JSON.parse(JSON.stringify(result)));
        }, 180 + Math.random() * 220);
      });
    },
    reset() {
      sessionStorage.removeItem(STORE_KEY);
    },
    exit() {
      sessionStorage.removeItem('mbDemo');
      sessionStorage.removeItem(STORE_KEY);
    },
    accounts: { staff: 'staff' + STAFF_DOMAIN, customer: 'lina@example.com' },
  };
})();
