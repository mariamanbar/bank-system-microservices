$(document).ready(function () {
    const table = $('#customersDT').DataTable({
        autoWidth: false,
        dom: '<"datatable-header"fPl><"datatable-scroll"t><"datatable-footer"ip>',
        ajax: function (data, callback, settings) {
            loaders.blockPage();
            $.ajax({
                url: API_BASE + '/customer',
                type: 'GET',
                // ADD THIS HEADERS BLOCK:
                headers: {
                    'Authorization': 'Bearer ' + localStorage.getItem('token')
                },
                success: function (res) {
                    // 1. Get the current user's role and email
                    const role = localStorage.getItem('userRole');
                    const myEmail = localStorage.getItem('userEmail');

                    // 2. Extract the data array
                    let rawData = Array.isArray(res) ? res : (res.data || []);

                    // 3. APPLY THE FILTER: If it's a customer, only keep their row
                    if (role === 'CUSTOMER') {
                        rawData = rawData.filter(cust => cust.email === myEmail);
                    }

                    // 4. Return the filtered data to the table
                    callback({ data: rawData });
                },
                error: () => notifs.error("Fetch Error", "Access Denied or Service unreachable"),
                complete: () => loaders.unblockPage()
            });
        },
        columns: [
            { data: 'id' },
            { data: 'name' },
            { data: 'natId' },
            { data: 'phone' },
            { data: 'email' },
            { data: 'balance', render: (val) => '$' + parseFloat(val || 0).toFixed(2) },
            {
                data: null,
                className: "text-center",
                render: (data, type, row, meta) => {
                    // SWAP THE ACTION BUTTONS BASED ON ROLE
                    const role = localStorage.getItem('userRole');
                    
                    if (role === 'CUSTOMER') {
                        // Customers get a read-only badge
                        return `<span class="label label-success">Active</span>`;
                    } else {
                        // Admins get the Edit and Delete buttons
                        return `
                            <ul class="icons-list">
                                <li><a href="#" onclick="editCustomer(${meta.row})"><i class="icon-pencil7 text-primary"></i></a></li>
                                <li><a href="#" onclick="deleteCustomer('${row.id}')"><i class="icon-trash text-danger"></i></a></li>
                            </ul>`;
                    }
                }
            }
        ]
    });

    $('#customerForm').on('submit', function (e) {
        e.preventDefault();
        const id = $('#cust_id').val();
        const isNew = !id;

        let payload = {
            name: $('#cust_name').val(),
            email: $('#cust_email').val(),
            natID: $('#cust_natId').val(),
            dob: $('#cust_dob').val(),
            phone: $('#cust_phone').val()
        };

        // Gateway handles the sub-paths (/register) dynamically
        // New customers go through the security service so the password is hashed (same as the Sign Up page).
        let url = isNew ? API_BASE + '/auth/register' : API_BASE + '/customer';
        if (isNew) {
            payload.password = $('#cust_password').val();
        } else {
            payload.id = id;
            payload.balance = parseFloat($('#cust_id').data('current-balance')) || 0.0;
        }

        loaders.blockPage();
        $.ajax({
            url: url,
            type: isNew ? 'POST' : 'PUT',
            contentType: 'application/json',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            data: JSON.stringify(payload),
            success: function (res) {
                notifs.success("Success", res.message);
                $('#modal_customer').modal('hide');
                table.ajax.reload();
            },
            error: (jqXHR) => notifs.error("Failed", jqXHR.responseJSON?.message || "Check Logs"),
            complete: () => loaders.unblockPage()
        });
    });
});

function openRegisterModal() {
    $('#customerForm')[0].reset();
    $('#cust_id').val('');
    $('#modalTitle').text('Register New Customer');
    $('#passwordSection').show();
    $('#modal_customer').modal('show');
}

function editCustomer(rowIndex) {
    const c = $('#customersDT').DataTable().row(rowIndex).data();
    const { id, name, email, balance, natId, dob, phone } = c;
    $('#customerForm')[0].reset();
    $('#modalTitle').text('Update Customer Profile');
    $('#cust_id').val(id).data('current-balance', balance);
    $('#cust_name').val(name);
    $('#cust_email').val(email);
    $('#cust_natId').val(natId);
    $('#cust_dob').val(dob);
    $('#cust_phone').val(phone);
    $('#passwordSection').hide();
    $('#modal_customer').modal('show');
}

function deleteCustomer(id) {
    if (!confirm("Are you sure?")) return;
    loaders.blockPage();
    $.ajax({
        url: API_BASE + '/customer',
        type: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        data: { id: id },
        success: (res) => { notifs.success("Deleted", res.message); $('#customersDT').DataTable().ajax.reload(); },
        error: () => notifs.error("Delete Failed"),
        complete: () => loaders.unblockPage()
    });
}