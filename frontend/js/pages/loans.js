const LOAN_API = API_BASE + "/loan";
const CUST_API = API_BASE + "/customer";

function fetchLoanSummary(explicitId) {
    // 1. Get the ID either from the argument or the input field
    const custId = explicitId || $('#search_cust_id').val();
    
    if (!custId) return notifs.error("Input Required", "Enter a Customer ID");

    loaders.blockPage();
    $.ajax({
        url: `${LOAN_API}?customerId=${custId}`,
        type: 'GET',
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
        success: (res) => {
            // Update Cards
            $('#stat_count').text(res.count || 0);
            $('#stat_total').text('$' + parseFloat(res.totalDebt || 0).toLocaleString());

            // Clear and Fill Table
            const tbody = $('#loan_details_body');
            tbody.empty();

            if (res.loans && res.loans.length > 0) {
                res.loans.forEach(loan => {
                    const statusLabel = loan.status === 'ACTIVE' ? 'label-success' : 'label-default';
                    
                    const row = `
                        <tr>
                            <td><span class="text-semibold">#${loan.loanId}</span></td>
                            <td><i class="icon-info22 text-size-mini position-left"></i> ${loan.loanType}</td>
                            <td>$${loan.principalAmount.toLocaleString()}</td>
                            <td class="text-danger-600 text-semibold">$${loan.remainingAmount.toLocaleString()}</td>
                            <td>${loan.nextInstallmentDate || 'N/A'}</td>
                            <td><span class="label ${statusLabel}">${loan.status}</span></td>
                        </tr>
                    `;
                    tbody.append(row);
                });
            } else {
                tbody.append('<tr><td colspan="6" class="text-center text-muted">No active loans found for this customer.</td></tr>');
            }

            $('#summary_container').fadeIn();
        },
        error: () => notifs.error("Not Found", "No data for this customer"),
        complete: () => loaders.unblockPage()
    });
}

$(document).ready(function () {
    const role = localStorage.getItem('userRole');
    const myEmail = localStorage.getItem('userEmail');

    // AUTO-LOAD FOR CUSTOMER
    if (role === 'CUSTOMER') {
        // Hide the search bar for customers to keep it clean
        $('.breadcrumb-line').hide(); 

        $.ajax({
            url: CUST_API,
            type: 'GET',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
            success: (custRes) => {
                const customers = Array.isArray(custRes) ? custRes : (custRes.data || []);
                const myCustomer = customers.find(c => c.email === myEmail);
                if (myCustomer) {
                    fetchLoanSummary(myCustomer.id);
                }
            }
        });
    }

    // 1. Issue New Loan
    $('#loanRequestForm').on('submit', function (e) {
        e.preventDefault();
        const payload = {
            customerId: $('#req_cust_id').val().trim(), // Added trim()
            amount: parseFloat($('#req_amount').val()),
            installmentAmount: parseFloat($('#req_inst_amount').val()),
            loanType: $('#req_type').val(),
            timestamp: new Date().toISOString()
        };

        loaders.blockPage();
        $.ajax({
            url: `${LOAN_API}/request`,
            type: 'POST',
            contentType: 'application/json',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
            data: JSON.stringify(payload),
            success: (res) => {
                notifs.success("Success", "Loan issued and disbursed to account.");
                $('#modal_request_loan').modal('hide');
                fetchLoanSummary(payload.customerId);
            },
            error: (err) => notifs.error("Request Failed", err.responseJSON?.message),
            complete: () => loaders.unblockPage()
        });
    });

    // 2. Pay or Delay Installment
    $('#loanActionForm').on('submit', function (e) {
        e.preventDefault();
        const mode = $('#action_mode').val();
        const payload = { loanId: parseInt($('#action_loan_id').val()) };
        
        if (mode === 'pay') {
            payload.amount = parseFloat($('#action_amount').val());
        }

        loaders.blockPage();
        $.ajax({
            url: `${LOAN_API}/${mode}`,
            type: 'POST',
            contentType: 'application/json',
            headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') },
            data: JSON.stringify(payload),
            success: (res) => {
                notifs.success("Success", res.message);
                $('#modal_loan_action').modal('hide');
                // Reload summary after action
                const currentId = role === 'CUSTOMER' ? null : $('#search_cust_id').val();
                fetchLoanSummary(currentId);
            },
            error: (err) => notifs.error("Action Failed", err.responseJSON?.message),
            complete: () => loaders.unblockPage()
        });
    });
});

// Modal UI Helpers
function openRequestModal() { 
    $('#loanRequestForm')[0].reset(); 
    $('#modal_request_loan').modal('show'); 
}

function openPayModal() {
    $('#loanActionForm')[0].reset();
    $('#action_mode').val('pay');
    $('#actionTitle').text('Pay Installment');
    $('#amount_field').show();
    $('#actionHeader').attr('class', 'modal-header bg-primary');
    $('#modal_loan_action').modal('show');
}

function openDelayModal() {
    $('#loanActionForm')[0].reset();
    $('#action_mode').val('delay');
    $('#actionTitle').text('Delay Installment (Apply Penalty)');
    $('#amount_field').hide();
    $('#actionHeader').attr('class', 'modal-header bg-warning');
    $('#modal_loan_action').modal('show');
}