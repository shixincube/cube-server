// dashboard.js

(function ($) {
    'use strict'

    var console = new Console();
    $.console = console;

    console.getServers(function(data) {
        $('#dispatcher-box').find('h3').text(data.dispatchers.length);
        $('#service-box').find('h3').text(data.services.length);
    });
})(jQuery);
