/*
 * Shared by every page: the API gateway address, notifications and the loading overlay.
 * (These used to be copy-pasted at the top of each page script.)
 */

// All requests go through the API gateway.
const API_BASE = 'http://localhost:8084';

const notifs = {
    error: (title, message) => {
        new PNotify({ title: title || 'Error', text: message || 'Failed.', addclass: 'bg-danger border-danger', type: 'error' });
    },
    success: (title, message) => {
        new PNotify({ title: title || 'Success', text: message || 'Completed.', addclass: 'bg-success border-success', type: 'success' });
    },
    info: (title, message) => {
        new PNotify({ title: title || 'Info', text: message || '', addclass: 'bg-info border-info' });
    }
};

const loaders = {
    blockPage: () => $.blockUI({
        message: '<i class="icon-spinner4 spinner"></i>',
        overlayCSS: { backgroundColor: '#1b2024', opacity: 0.8, cursor: 'wait' },
        css: { border: 0, color: '#fff', padding: 0, backgroundColor: 'transparent' }
    }),
    unblockPage: () => $.unblockUI()
};
