/*
 * A small sortable, searchable, paginated table.
 * Replaces the DataTables plugin (and its jQuery dependency).
 *
 * columns: [{ key, label, render(row), value(row), align: 'right', sortable, searchable }]
 * Action buttons inside cells use data-action="name"; handle them with table.onAction().
 */
'use strict';

class DataTable {
  constructor(table, options) {
    this.table = table;
    this.columns = options.columns;
    this.pageSize = options.pageSize || 10;
    this.emptyTitle = options.emptyTitle || 'Nothing here yet';
    this.emptyText = options.emptyText || '';
    this.sort = options.sort || null;           // { key, dir: 'asc' | 'desc' }
    this.countEl = options.countEl || null;
    this.countLabel = options.countLabel || ['item', 'items'];
    this.rows = [];
    this.query = '';
    this.filter = null;
    this.page = 0;
    this.handlers = [];

    this.thead = table.tHead || table.createTHead();
    this.tbody = table.tBodies[0] || table.createTBody();

    this.pager = document.createElement('div');
    this.pager.className = 'pager';
    this.pager.hidden = true;
    (table.closest('.table-wrap') || table).after(this.pager);

    if (options.search) {
      options.search.addEventListener('input', e => {
        this.query = e.target.value.trim().toLowerCase();
        this.page = 0;
        this.render();
      });
    }

    this.thead.addEventListener('click', e => {
      const btn = e.target.closest('[data-sort]');
      if (!btn) return;
      const key = btn.dataset.sort;
      const dir = this.sort && this.sort.key === key && this.sort.dir === 'asc' ? 'desc' : 'asc';
      this.sort = { key, dir };
      this.renderHead();
      this.render();
    });

    this.pager.addEventListener('click', e => {
      const btn = e.target.closest('[data-page]');
      if (!btn) return;
      this.page += Number(btn.dataset.page);
      this.render();
    });

    const dispatch = e => {
      const el = e.target.closest('[data-action]');
      if (!el || (e.type === 'click' && el.tagName === 'SELECT')) return;
      const tr = el.closest('tr[data-index]');
      if (!tr) return;
      e.preventDefault();
      const row = this.rows[Number(tr.dataset.index)];
      this.handlers.forEach(h => h(el.dataset.action, row, el));
    };
    this.tbody.addEventListener('click', dispatch);
    this.tbody.addEventListener('change', dispatch);

    this.renderHead();
  }

  onAction(handler) { this.handlers.push(handler); return this; }

  setFilter(fn) { this.filter = fn; this.page = 0; this.render(); }

  value(row, col) { return col.value ? col.value(row) : row[col.key]; }

  renderHead() {
    const cells = this.columns.map(col => {
      const cls = col.align === 'right' ? ' class="right"' : (col.key === 'actions' ? ' class="actions"' : '');
      if (!col.label) return `<th${cls}><span class="visually-hidden">Actions</span></th>`;
      if (col.sortable === false || col.key === 'actions') return `<th${cls}>${App.esc(col.label)}</th>`;
      const active = this.sort && this.sort.key === col.key;
      const aria = active ? ` aria-sort="${this.sort.dir === 'asc' ? 'ascending' : 'descending'}"` : '';
      const mark = active ? (this.sort.dir === 'asc' ? '▲' : '▼') : '';
      return `<th${cls}${aria}><button type="button" class="th-sort" data-sort="${col.key}">${App.esc(col.label)}<span class="mark" aria-hidden="true">${mark}</span></button></th>`;
    });
    this.thead.innerHTML = `<tr>${cells.join('')}</tr>`;
  }

  setLoading() {
    const cols = this.columns.length;
    const row = `<tr class="skeleton">${'<td><span></span></td>'.repeat(cols)}</tr>`;
    this.tbody.innerHTML = row.repeat(4);
    this.pager.hidden = true;
    if (this.countEl) this.countEl.textContent = 'Loading…';
  }

  setError(message, retry) {
    this.tbody.innerHTML = `<tr class="table-state"><td colspan="${this.columns.length}">
      <strong>Couldn't load this list</strong>${App.esc(message)}
      ${retry ? '<div><button type="button" class="btn btn-secondary" data-retry>Try again</button></div>' : ''}
    </td></tr>`;
    if (retry) this.tbody.querySelector('[data-retry]').addEventListener('click', retry);
    this.pager.hidden = true;
    if (this.countEl) this.countEl.textContent = '';
  }

  setData(rows) {
    this.rows = rows || [];
    this.page = 0;
    this.render();
  }

  visibleRows() {
    let list = this.rows.map((row, index) => ({ row, index }));
    if (this.filter) list = list.filter(({ row }) => this.filter(row));
    if (this.query) {
      const cols = this.columns.filter(c => c.searchable !== false && c.key !== 'actions');
      list = list.filter(({ row }) => cols.some(c => {
        const v = c.search ? c.search(row) : this.value(row, c);
        return v !== null && v !== undefined && String(v).toLowerCase().includes(this.query);
      }));
    }
    if (this.sort) {
      const col = this.columns.find(c => c.key === this.sort.key);
      if (col) {
        const dir = this.sort.dir === 'asc' ? 1 : -1;
        list.sort((a, b) => {
          const x = this.value(a.row, col);
          const y = this.value(b.row, col);
          if (x === y) return 0;
          if (x === null || x === undefined || x === '') return 1;
          if (y === null || y === undefined || y === '') return -1;
          if (typeof x === 'number' && typeof y === 'number') return (x - y) * dir;
          return String(x).localeCompare(String(y), undefined, { numeric: true, sensitivity: 'base' }) * dir;
        });
      }
    }
    return list;
  }

  render() {
    const list = this.visibleRows();
    const total = list.length;
    const pages = Math.max(1, Math.ceil(total / this.pageSize));
    this.page = Math.min(Math.max(0, this.page), pages - 1);
    const start = this.page * this.pageSize;
    const slice = list.slice(start, start + this.pageSize);

    if (this.countEl) {
      const [one, many] = this.countLabel;
      this.countEl.textContent = this.rows.length === total
        ? `${total} ${total === 1 ? one : many}`
        : `${total} of ${this.rows.length} ${many}`;
    }

    if (!total) {
      const searching = this.query || this.filter;
      this.tbody.innerHTML = `<tr class="table-state"><td colspan="${this.columns.length}">
        <strong>${searching ? 'No matches' : App.esc(this.emptyTitle)}</strong>
        ${searching ? 'Try a different search.' : App.esc(this.emptyText)}
      </td></tr>`;
      this.pager.hidden = true;
      return;
    }

    this.tbody.innerHTML = slice.map(({ row, index }) => {
      const cells = this.columns.map(col => {
        const cls = col.align === 'right' ? ' class="right"' : (col.key === 'actions' ? ' class="actions"' : '');
        const html = col.render ? col.render(row) : (this.value(row, col) ?? '') === '' ? App.EMPTY : App.esc(this.value(row, col));
        return `<td${cls}>${html}</td>`;
      });
      return `<tr data-index="${index}">${cells.join('')}</tr>`;
    }).join('');

    this.pager.hidden = total <= this.pageSize;
    if (!this.pager.hidden) {
      this.pager.innerHTML = `<span>${start + 1}–${Math.min(start + this.pageSize, total)} of ${total}</span>
        <div>
          <button type="button" class="btn btn-secondary" data-page="-1"${this.page === 0 ? ' disabled' : ''}>Previous</button>
          <button type="button" class="btn btn-secondary" data-page="1"${this.page >= pages - 1 ? ' disabled' : ''}>Next</button>
        </div>`;
    }
  }
}

window.DataTable = DataTable;
