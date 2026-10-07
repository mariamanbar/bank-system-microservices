const CARD_API = API_BASE + "/card";
const ACCT_API = API_BASE + "/account";
const CUST_API = API_BASE + "/customer";

$(document).ready(function () {
    const role = localStorage.getItem('userRole');
    const myEmail = localStorage.getItem('userEmail');

    const table = $('#cardsDT').DataTable({
        autoWidth: false,
        dom: '<"datatable-header"fPl><"datatable-scroll"t><"datatable-footer"ip>',
        ajax: function (data, callback, settings) {
            loaders.blockPage();

            // 1. Fetch Customer Record to get ID
            $.ajax({
                url: CUST_API,
                type: 'GET',
                headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                success: (custRes) => {
                    const customers = Array.isArray(custRes) ? custRes : (custRes.data || []);
                    const myCustomer = customers.find(c => c.email === myEmail);

                    // 2. Fetch Cards
                    $.ajax({
                        url: CARD_API,
                        type: 'GET',
                        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                        success: (cardRes) => {
                            let allCards = Array.isArray(cardRes) ? cardRes : (cardRes.data || []);
                            
                            // 3. Filter: If Customer, only show cards matching their ID
                            if (role === 'CUSTOMER' && myCustomer) {
                                allCards = allCards.filter(card => card.customerId === myCustomer.id);
                            }

                            callback({ data: allCards });
                        },
                        error: () => notifs.error("Fetch Error", "Card Service unreachable"),
                        complete: () => loaders.unblockPage()
                    });
                },
                error: () => {
                    notifs.error("Fetch Error", "Customer Service unreachable");
                    loaders.unblockPage();
                }
            });
        },
        columns: [
            { data: 'id' },
            { data: 'customerId' },
            { data: 'accountId' },
            {
                data: 'cardNumber',
                render: (val) => val ? val.replace(/\W/gi, '').replace(/(.{4})/g, '$1 ') : 'N/A'
            },
            { data: 'cardType' },
            {
                data: 'cvv',
                render: (val) => `<span class="text-muted">***</span> <small>(${val})</small>`
            },
            { data: 'expiryDate' },
            {
                data: 'status',
                render: (val) => {
                    const badges = {
                        'ACTIVE': 'label-success',
                        'PENDING': 'label-warning',
                        'DECLINED': 'label-danger'
                    };
                    let labelClass = badges[val] || 'label-default';
                    return `<span class="label ${labelClass}">${val}</span>`;
                }
            },
            {
                data: null,
                className: "text-center",
                render: (data, type, row) => {
                    if (role === 'CUSTOMER') {
                        // Customers can only view their PIN
                        return `
                            <ul class="icons-list">
                                <li><a href="#" onclick="viewPin('${row.pin}')" title="View PIN"><i class="icon-eye text-primary"></i></a></li>
                            </ul>`;
                    } else {
                        // Admins get the full management menu
                        return `
                            <ul class="icons-list">
                                <li class="dropdown">
                                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="icon-menu9"></i></a>
                                    <ul class="dropdown-menu dropdown-menu-right">
                                        <li class="dropdown-header">Manage Status</li>
                                        <li><a href="#" onclick="updateStatus(${row.id}, 'ACTIVE')"><i class="icon-checkmark4 text-success"></i> Activate</a></li>
                                        <li><a href="#" onclick="updateStatus(${row.id}, 'PENDING')"><i class="icon-history text-warning"></i> Set Pending</a></li>
                                        <li><a href="#" onclick="updateStatus(${row.id}, 'DECLINED')"><i class="icon-cross2 text-danger"></i> Decline</a></li>
                                        <li class="divider"></li>
                                        <li><a href="#" onclick="viewPin('${row.pin}')"><i class="icon-eye"></i> View PIN</a></li>
                                        <li><a href="#" onclick="deleteCard('${row.id}')"><i class="icon-trash text-danger"></i> Revoke Card</a></li>
                                    </ul>
                                </li>
                            </ul>`;
                    }
                }
            }
        ]
    });

    // Form Submission: Issue New Card
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
                notifs.success("Success", "Card request issued successfully!");
                $('#modal_issue_card').modal('hide');
                setTimeout(() => table.ajax.reload(), 1000);
            },
            error: (jqXHR) => notifs.error("Failed", jqXHR.responseJSON?.message || "Check Logs"),
            complete: () => loaders.unblockPage()
        });
    });
});

// --- Helper Functions ---

function openIssueCardModal() {
    $('#cardForm')[0].reset();
    $('#modal_issue_card').modal('show');
}

function viewPin(pin) {
    notifs.info("Card PIN", "Secure access requested. The PIN is: **" + pin + "**");
}

function updateStatus(cardId, newStatus) {
    loaders.blockPage();
    $.ajax({
        url: `${CARD_API}/status`, 
        type: 'PATCH',
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
        data: { id: cardId, status: newStatus },
        success: (res) => {
            notifs.success("Status Updated", "Card " + cardId + " is now " + newStatus);
            $('#cardsDT').DataTable().ajax.reload(null, false);
        },
        error: (jqXHR) => notifs.error("Update Failed", jqXHR.responseJSON?.message || "Error"),
        complete: () => loaders.unblockPage()
    });
}

function deleteCard(id) {
    if (!confirm("Are you sure you want to revoke card ID: " + id + "?")) return;
    loaders.blockPage();
    $.ajax({
        url: CARD_API + "?id=" + id,
        type: 'DELETE',
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
        success: (res) => {
            notifs.success("Deleted", "Card revoked successfully.");
            $('#cardsDT').DataTable().ajax.reload();
        },
        error: () => notifs.error("Delete Failed"),
        complete: () => loaders.unblockPage()
    });
}