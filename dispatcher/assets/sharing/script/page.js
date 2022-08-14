// page.js

if (undefined === window.screen) {
    window.screen = {
        width: 0,
        height: 0,
        colorDepth: 0
    };
}

window.onload = function () {
    var sharer = '';
    var parent = '';

    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i = 0; i < vars.length; ++i) {
        var pair = vars[i].split("=");
        if (pair[0] == 's') {
            sharer = pair[1];
        }
        else if (pair[0] == 'p') {
            parent = pair[1];
        }
    }

    const data = {
        "domain": document.domain,
        "url": document.URL,
        "title": document.title,
        "screen": {
            "width": window.screen.width,
            "height": window.screen.height,
            "colorDepth": window.screen.colorDepth,
            "orientation": (screen.orientation || { type: 'unknown' }).type || screen.mozOrientation || screen.msOrientation
        },
        "language": navigator.language,
        "userAgent": navigator.userAgent,
        "event": window.traceEvent,
        "eventParam": {
            "sharer": sharer,
            "parent": parent
        }
    };

    submit(data);
};

function submit(data) {
    var xhr = null;
    if (window.XMLHttpRequest) {
        // code for IE7+, Firefox, Chrome, Opera, Safari
        xhr = new XMLHttpRequest();
    }
    else {
        // code for IE6, IE5
        xhr = new ActiveXObject("Microsoft.XMLHTTP");
    }

    xhr.onreadystatechange = function() {
        if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
            // 完成
        }
    };

    xhr.method = "POST";
    xhr.responseType = "json";
    xhr.open("POST", "/sharing/trace/browser", true);

    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(data));
}
