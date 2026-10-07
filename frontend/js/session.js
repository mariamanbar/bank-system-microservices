/*
 * Session handling for every signed-in page:
 * attaches the JWT to requests, redirects to login when signed out,
 * adjusts the menu for customers, and provides logout().
 */

// 1. GLOBAL AJAX SETUP (Runs before any AJAX request)

// Automatically attaches the JWT token to every request
$.ajaxSetup({
    beforeSend: function(xhr) {
        const token = localStorage.getItem('token');
        if (token) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + token);
        }
    }
});

/* ---------------------------------------------------------------------------- */
// 2. MAIN PAGE LOAD LOGIC
$(document).ready(function() {
    const token = localStorage.getItem('token');
    const userEmail = localStorage.getItem('userEmail');
    const role = localStorage.getItem('userRole'); 
    
    const path = window.location.pathname;
    const isAuthPage = path.includes('login.html') || path.includes('register.html');

    // --- A. SECURITY CHECK ---
    // Redirect to login if no token (and not already on login/register page)
    if (!token && !isAuthPage) {
        window.location.replace('login.html');
        return; // Stop execution
    }

    // --- B. UI PROFILE UPDATE ---
    if (userEmail && !isAuthPage) {
        // Sets the name to 'mariam' from 'mariam@gmail.com'
        const displayName = localStorage.getItem('userName') || userEmail.split('@')[0];
        
        // Update your custom ID, and also target the Limitless theme's default class
        $('#userNameDisplay').text(displayName);
        $('.media-heading').text(displayName); 
    }

    // --- C. ROLE-BASED DASHBOARD TRANSFORMATION ---
    if (role === 'CUSTOMER' && !isAuthPage) {
        console.log("Applying Customer Theme...");

        // 1. Rename Sidebar Links (Change text inside the <span> tags)
        $('.navigation-main a[href="index.html"] span').text('My Profile'); 
        $('.navigation-main a[href="accounts.html"] span').text('My Accounts');
        $('.navigation-main a[href="cards.html"] span').text('My Cards');
        $('.navigation-main a[href="loans.html"] span').text('My Loans');

        // 2. Hide Admin-Only Sidebar Sections
        $('.navigation-header:contains("Monitoring")').hide();
        $('a:contains("Transaction Logs")').closest('li').hide();

        // 3. Hide Global Admin Buttons
        $('.btn:contains("Register New Customer")').hide();
        $('.btn:contains("Open New Account")').hide();
        
        // 4. Update Page Header Titles
        $('.panel-title, .page-title h4').each(function() {
            let text = $(this).text();
            $(this).text(text.replace('Directory', 'Details').replace('System', 'Overview'));
        });

    } else if (role === 'ADMIN' && !isAuthPage) {
        console.log("Running in Admin Mode");
        // Everything stays visible
    }
});

/* ---------------------------------------------------------------------------- */
// 3. LOGOUT FUNCTION
function logout() {
    console.log("Logging out...");
    
    // Clear storage completely
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
    localStorage.removeItem('userRole');
    
    // Use replace to prevent the "Back" button from re-triggering sessions
    window.location.replace('login.html');
}
