// Apply saved theme immediately to prevent flash of wrong theme
(function() {
    var t = localStorage.getItem('nh-admin-theme');
    if (t === 'light') document.documentElement.setAttribute('data-theme', 'light');
})();
