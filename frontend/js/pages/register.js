'use strict';

(function () {
  const { api, withBusy } = App;
  const $ = id => document.getElementById(id);
  const error = $('registerError');

  $('registerForm').addEventListener('submit', e => {
    e.preventDefault();
    const form = e.currentTarget;
    error.hidden = true;
    if (!form.reportValidity()) return;
    const email = $('regEmail').value.trim();

    withBusy($('registerBtn'), async () => {
      try {
        await api('/auth/register', {
          method: 'POST',
          body: {
            name: $('regName').value.trim(),
            email,
            password: $('regPassword').value,
            dob: $('regDob').value,
            phone: $('regPhone').value.trim(),
            natID: $('regNatId').value.trim(),
          },
        });
        location.href = 'login.html?registered=1&email=' + encodeURIComponent(email);
      } catch (err) {
        error.textContent = err.message;
        error.hidden = false;
        error.scrollIntoView({ block: 'nearest' });
      }
    });
  });
})();
