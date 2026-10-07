$(document).ready(function () {
    $('#registerForm').on('submit', function (e) {
        e.preventDefault();

        // 1. Prepare the JSON body exactly as Swagger expects
        const payload = {
            name: $('#reg_name').val(),
            email: $('#reg_email').val(),
            password: $('#reg_password').val(),
            dob: $('#reg_dob').val(),
            phone: $('#reg_phone').val(),
            natID: $('#reg_natId').val() // Check if backend uses 'natID' or 'natId'
        };

        // 2. Visual feedback
        $.blockUI({ 
            message: '<i class="icon-spinner4 spinner"></i>', 
            overlayCSS: { backgroundColor: '#1b2024', opacity: 0.8, cursor: 'wait' }, 
            css: { border: 0, color: '#fff', padding: 0, backgroundColor: 'transparent' } 
        });

        // 3. AJAX call to Gateway
        $.ajax({
            url: API_BASE + '/auth/register', // Ensure Gateway port 8084 is correct
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function (res) {
                new PNotify({
                    title: 'Registration Successful',
                    text: 'You can now log in with your new account.',
                    addclass: 'bg-success border-success',
                    type: 'success'
                });
                
                // Redirect to login after 2 seconds
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
            },
            error: function (xhr) {
                $.unblockUI();
                const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : "Registration failed. Please check your details.";
                new PNotify({
                    title: 'Error',
                    text: errorMsg,
                    addclass: 'bg-danger border-danger',
                    type: 'error'
                });
            },
            complete: function () {
                // Only unblock if not redirecting
                if (window.location.href.includes('register.html')) {
                    $.unblockUI();
                }
            }
        });
    });
});