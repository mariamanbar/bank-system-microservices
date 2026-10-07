const LOGS_API = API_BASE + "/logger";

$(document).ready(function () {
    const table = $('#logsDT').DataTable({
        autoWidth: false,
        order: [[0, "desc"]], // Sort by timestamp descending
        dom: '<"datatable-header"fPl><"datatable-scroll"t><"datatable-footer"ip>',
        ajax: function (data, callback, settings) {
            loaders.blockPage();
            $.ajax({
                url: LOGS_API,
                type: 'GET',
                success: (res) => {
            let filteredData = res;
            if (localStorage.getItem('userRole') === 'CUSTOMER') {
                const myEmail = localStorage.getItem('userEmail');
                // Filter the logs to only show this customer's actions
                filteredData = res.filter(log => log.message.includes(myEmail) || log.customerId === localStorage.getItem('myId'));
            }
            callback({ data: filteredData });
        },
                
                error: () => {
                    notifs.error("Fetch Error", "Logs Service unreachable");
                    loaders.unblockPage();
                },

                complete: () => loaders.unblockPage()
            });
        },
        columns: [
            {
                data: 'timestamp',
                render: (val) => val ? val.replace('T', ' ').substring(0, 19) : 'N/A'
            },
            {
                data: 'serviceName',
                render: (val) => {
                    let color = 'bg-slate';
                    if (val === 'Loan-Service') color = 'bg-teal';
                    if (val === 'Account-Service') color = 'bg-indigo';
                    if (val === 'Customer-Service') color = 'bg-orange';
                    return `<span class="label ${color}">${val}</span>`;
                }
            },
            {
                data: 'type',
                render: (val) => `<b>${val}</b>`
            },
            { data: 'customerId', defaultContent: '<span class="text-muted">N/A</span>' },
            { data: 'accountId', defaultContent: '<span class="text-muted">N/A</span>' },
            {
                data: 'message',
                className: 'text-semibold text-grey-800'
            }
        ]
    });
});

function refreshLogs() {
    $('#logsDT').DataTable().ajax.reload();
}