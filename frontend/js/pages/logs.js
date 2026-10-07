'use strict';

(function () {
  if (!App.boot()) return;
  const { api, listOf, getMyCustomer, Session, esc, fmt, withBusy } = App;
  const mine = Session.isCustomer;
  const $ = id => document.getElementById(id);

  // Each service gets a consistent colour tag.
  const tagClass = {};
  const tagFor = name => {
    if (!name) return App.EMPTY;
    if (!(name in tagClass)) tagClass[name] = 't' + ((Object.keys(tagClass).length % 4) + 1);
    return `<span class="tag ${tagClass[name]}">${esc(String(name).replace(/-/g, ' '))}</span>`;
  };

  const table = new DataTable($('logsTable'), {
    pageSize: 25,
    search: $('logSearch'),
    countEl: $('logCount'),
    countLabel: ['entry', 'entries'],
    emptyTitle: 'No activity yet',
    emptyText: 'Actions across the system will show up here.',
    sort: { key: 'timestamp', dir: 'desc' },
    columns: [
      { key: 'timestamp', label: 'Time', searchable: false, render: r => `<span class="num">${fmt.dateTime(r.timestamp)}</span>` },
      { key: 'serviceName', label: 'Service', render: r => tagFor(r.serviceName) },
      { key: 'type', label: 'Event', render: r => r.type ? `<span class="cell-primary">${esc(fmt.label(r.type))}</span>` : App.EMPTY },
      { key: 'customerId', label: 'Customer ID', render: r => fmt.id(r.customerId) },
      { key: 'accountId', label: 'Account ID', render: r => fmt.id(r.accountId) },
      { key: 'message', label: 'Message', sortable: false },
    ],
  });

  const serviceFilter = $('serviceFilter');
  serviceFilter.addEventListener('change', () => {
    const s = serviceFilter.value;
    table.setFilter(s ? r => r.serviceName === s : null);
  });

  async function load() {
    table.setLoading();
    try {
      let logs = listOf(await api('/logger'));
      if (mine) {
        const me = await getMyCustomer().catch(() => null);
        logs = logs.filter(l => (l.message || '').includes(Session.email) || (me && l.customerId === me.id));
      }
      // Rebuild the service filter from what's in the data.
      const current = serviceFilter.value;
      const services = [...new Set(logs.map(l => l.serviceName).filter(Boolean))].sort();
      serviceFilter.innerHTML = '<option value="">All services</option>' +
        services.map(s => `<option value="${esc(s)}"${s === current ? ' selected' : ''}>${esc(s.replace(/-/g, ' '))}</option>`).join('');
      services.forEach(tagFor);
      table.setData(logs);
    } catch (err) {
      table.setError(err.message, load);
    }
  }

  $('refreshBtn').addEventListener('click', e => withBusy(e.currentTarget, load));

  load();
})();
