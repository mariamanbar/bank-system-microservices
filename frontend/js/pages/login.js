'use strict';

(function () {
  const { api, Session, withBusy } = App;
  const $ = id => document.getElementById(id);
  const params = new URLSearchParams(location.search);
  const note = $('loginNote');
  const error = $('loginError');

  if (params.get('reason') === 'expired') {
    note.textContent = 'Your session ended. Sign in again to continue.';
    note.hidden = false;
  }
  if (params.get('registered')) {
    note.textContent = 'Account created. Sign in with your new email and password.';
    note.hidden = false;
  }
  if (params.get('email')) {
    $('email').value = params.get('email');
    $('password').focus();
  }

  $('loginForm').addEventListener('submit', e => {
    e.preventDefault();
    const form = e.currentTarget;
    error.hidden = true;
    if (!form.reportValidity()) return;
    const email = $('email').value.trim();
    const password = $('password').value;

    withBusy($('loginBtn'), async () => {
      try {
        const res = await api('/auth/login', { method: 'POST', body: { email, password } });
        if (!res || !res.token) throw new Error("The server didn't return a session. Try again.");
        Session.start(res, email);
        location.href = 'index.html';
      } catch (err) {
        error.textContent = err.status === 0 || !err.status ? err.message : 'That email and password don\'t match. Check them and try again.';
        error.hidden = false;
        $('password').select();
      }
    });
  });
})();
