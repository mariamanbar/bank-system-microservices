const ACCT_API = API_BASE + "/account";
// We need to call the Customer API first to get the Customer ID from their email
const CUST_API = API_BASE + "/customer";

$(document).ready(function () {
    const role = localStorage.getItem('userRole');
    const myEmail = localStorage.getItem('userEmail');

    const table = $('#accountsDT').DataTable({
        autoWidth: false,
        dom: '<"datatable-header"fPl><"datatable-scroll"t><"datatable-footer"ip>',
        ajax: function (data, callback, settings) {
            loaders.blockPage();
            
            // 1. IF ADMIN: Just fetch all accounts normally
            if (role !== 'CUSTOMER') {
                $.ajax({
                    url: ACCT_API,
                    type: 'GET',
                    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                    success: (res) => {
                        callback({ data: Array.isArray(res) ? res : (res.data || []) });
                    },
                    error: () => notifs.error("Fetch Error", "Account service unreachable"),
                    complete: () => loaders.unblockPage()
                });
                return;
            }

            // 2. IF CUSTOMER: We must first find their Customer ID using their email
            $.ajax({
                url: CUST_API,
                type: 'GET',
                headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                success: (custRes) => {
                    const customers = Array.isArray(custRes) ? custRes : (custRes.data || []);
                    const myCustomerRecord = customers.find(c => c.email === myEmail);

                    if (!myCustomerRecord) {
                        loaders.unblockPage();
                        return callback({ data: [] }); // No customer found, show empty table
                    }

                    const myCustomerId = myCustomerRecord.id;

                    // 3. Now fetch the accounts and filter by that Customer ID
                    $.ajax({
                        url: ACCT_API,
                        type: 'GET',
                        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                        success: (accRes) => {
                            const allAccounts = Array.isArray(accRes) ? accRes : (accRes.data || []);
                            const myAccounts = allAccounts.filter(acc => acc.customerId === myCustomerId);
                            
                            callback({ data: myAccounts });
                        },
                        error: () => notifs.error("Fetch Error", "Account service unreachable"),
                        complete: () => loaders.unblockPage()
                    });
                },
                error: () => {
                    notifs.error("Fetch Error", "Customer service unreachable");
                    loaders.unblockPage();
                    callback({ data: [] });
                }
            });
        },
        columns: [
            { data: 'accountId' },
            { data: 'customerId' },
            { data: 'type' },
            {
                data: 'balance',
                render: function (val) {
                    return '$' + parseFloat(val || 0).toFixed(2);
                }
            },
            {
                data: null,
                className: "text-center",
                render: function (data, type, row) {
                    // SWAP THE ACTION BUTTONS BASED ON ROLE
                    if (role === 'CUSTOMER') {
                        // Customers get Transact and Request Card, but NOT Delete
                        return `
                            <ul class="icons-list">
                                <li>
                                    <a href="#" onclick="openTransModal('${row.accountId}', 'credit')" title="Deposit">
                                        <i class="icon-plus2 text-success"></i>
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="openTransModal('${row.accountId}', 'debit')" title="Withdraw">
                                        <i class="icon-minus2 text-warning"></i>
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="openCardModal('${row.accountId}')" title="Request Card">
                                        <i class="icon-credit-card text-primary"></i>
                                    </a>
                                </li>
                            </ul>`;
                    } else {
                        // Admins get everything, including Delete
                        return `
                            <ul class="icons-list">
                                <li>
                                    <a href="#" onclick="openTransModal('${row.accountId}', 'credit')" title="Credit Account">
                                        <i class="icon-plus2 text-success"></i>
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="openTransModal('${row.accountId}', 'debit')" title="Debit Account">
                                        <i class="icon-minus2 text-warning"></i>
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="openCardModal('${row.accountId}')" title="Issue Card">
                                        <i class="icon-credit-card text-primary"></i>
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="deleteAccount('${row.accountId}')" title="Delete Account">
                                        <i class="icon-trash text-danger"></i>
                                    </a>
                                </li>
                            </ul>`;
                    }
                }
            }
        ]
    });

    // Create Account
    $('#createAccountForm').on('submit', function (e) {
        e.preventDefault();

        const payload = {
            customerId: $('#acc_cust_id').val(),
            accountType: $('#acc_type').val()
        };

        loaders.blockPage();
        $.ajax({
            url: ACCT_API + "/createAccount",
            type: 'POST',
            contentType: 'application/json',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
            data: JSON.stringify(payload),
            success: function (res) {
                notifs.success("Success", res.message);
                $('#modal_create_account').modal('hide');
                table.ajax.reload();
            },
            error: function (err) {
                notifs.error("Failed", err.responseJSON && err.responseJSON.message);
            },
            complete: function () {
                loaders.unblockPage();
            }
        });
    });

    // Credit / Debit
    $('#transForm').on('submit', function (e) {
        e.preventDefault();

        const action = $('#trans_action_type').val();
        const payload = {
            accountId: $('#trans_acc_id').val(),
            amount: parseFloat($('#trans_amount').val())
        };

        loaders.blockPage();
        $.ajax({
            url: ACCT_API + "/" + action,
            type: 'POST',
            contentType: 'application/json',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
            data: JSON.stringify(payload),
            success: function (res) {
                notifs.success("Success", res.message);
                $('#modal_transaction').modal('hide');
                table.ajax.reload();
            },
            error: function (err) {
                notifs.error("Transaction Failed", err.responseJSON && err.responseJSON.message);
            },
            complete: function () {
                loaders.unblockPage();
            }
        });
    });

    // Card Creation
    $('#cardForm').on('submit', function (e) {
        e.preventDefault();

        const payload = {
            accountId: $('#card_acc_id').val(),
            pin: parseInt($('#card_pin').val()),
            cardType: $('#card_type').val()
        };

        loaders.blockPage();
        $.ajax({
            url: ACCT_API + "/createCard",
            type: 'POST',
            contentType: 'application/json',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
            data: JSON.stringify(payload),
            success: function (res) {
                notifs.success("Success", res.message);
                $('#modal_card').modal('hide');
            },
            error: function () {
                notifs.error("Card Request Failed");
            },
            complete: function () {
                loaders.unblockPage();
            }
        });
    });
});

// Helpers
function openCreateAccountModal() {
    $('#createAccountForm')[0].reset();
    $('#modal_create_account').modal('show');
}

function openTransModal(id, action) {
    $('#transForm')[0].reset();
    $('#trans_acc_id').val(id);
    $('#trans_action_type').val(action);
    $('#transTitle').text(action.toUpperCase() + " Operation");
    $('#modal_transaction').modal('show');
}

function openCardModal(id) {
    $('#cardForm')[0].reset();
    $('#card_acc_id').val(id);
    $('#modal_card').modal('show');
}

function deleteAccount(id) {
    if (!confirm("Delete account " + id + "?")) return;

    loaders.blockPage();
    $.ajax({
        url: ACCT_API,
        type: 'DELETE',
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
        data: { id: id },
        success: function (res) {
            notifs.success("Deleted", res.message);
            $('#accountsDT').DataTable().ajax.reload();
        },
        error: function () {
            notifs.error("Delete Failed");
        },
        complete: function () {
            loaders.unblockPage();
        }
    });
}