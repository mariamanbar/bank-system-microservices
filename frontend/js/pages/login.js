$(document).ready(function () {

    $('#loginForm').on('submit', function (e) {
        e.preventDefault();

        const loginData = {
            email: $('#email').val(),
            password: $('#password').val()
        };

        loaders.blockPage();

        $.ajax({
            // Points to Gateway (8084) -> Security Service (8087)
            url: API_BASE + '/auth/login',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(loginData),
            success: function (res) {
                if (res.token) {
                    // 1. Save the JWT token in the browser's storage
                    localStorage.setItem('token', res.token);
                    localStorage.setItem('userEmail', loginData.email);
                    localStorage.setItem('userRole', res.role); // Save "ADMIN" or "CUSTOMER"

                    notifs.success("Login Successful", "Redirecting to dashboard...");
                    
                    localStorage.setItem('token', res.token);
        localStorage.setItem('userEmail', loginData.email);
        
        // If your Login API returns the user's name, save it!
        // If not, you can just use the email prefix for now
        const nameFromEmail = loginData.email.split('@')[0]; 
        localStorage.setItem('userName', nameFromEmail);

                    // 2. Redirect to your main page after 0.5 second
                    setTimeout(() => {
                        window.location.href = 'index.html'; 
                    }, 500);
                } else {
                    notifs.error("Login Failed", "No token received from server.");
                }
            },
            error: function (xhr) {
                const errorMsg = xhr.responseJSON ? xhr.responseJSON.message : "Invalid email or password";
                notifs.error("Access Denied", errorMsg);
            },
            complete: function () {
                loaders.unblockPage();
            }
        });
    });
});