// index.js

window.sn = Date.now();
var sharer = '';
var parent = '';

window.onload = function () {
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
            "orientation": (screen.orientation || {}).type || screen.mozOrientation || screen.msOrientation
        },
        "language": navigator.language,
        "userAgent": navigator.userAgent,
        "event": "View",
        "eventParam": {
            "sn": window.sn,
            "sharer": sharer,
            "parent": parent
        }
    };

    submit(data);

    resizePreview();
}

window.onresize = function () {
    setTimeout(function() {
        resizePreview();
    }, 1000);
}

function resizePreview() {
    var preview = document.querySelector('.preview');
    preview.style.height = document.body.clientHeight + 'px';
}

function download(url) {
    window.location.href = url;

    const data = {
        "domain": document.domain,
        "url": document.URL,
        "title": document.title,
        "screen": {
            "width": window.screen.width,
            "height": window.screen.height,
            "colorDepth": window.screen.colorDepth,
            "orientation": (screen.orientation || {}).type || screen.mozOrientation || screen.msOrientation
        },
        "language": navigator.language,
        "userAgent": navigator.userAgent,
        "event": "Extract",
        "eventTag": "button",
        "eventParam": {
            "sn": window.sn,
            "url": url,
            "sharer": sharer,
            "parent": parent
        }
    };

    submit(data);
}

function copy() {
    var el = document.getElementById('sharing_url');
    el.focus();
    el.select();
    document.execCommand('copy');

    showToast();
}

function showToast() {
    var toast = document.getElementById('toast');
    toast.style.display = 'block';
    toast.style.animation = 'toast-show-anim 0.5s';

    setTimeout(function () {
        hideToast();
    }, 3000);
}

function hideToast() {
    var toast = document.getElementById('toast');
    toast.style.animation = 'toast-hide-anim 0.5s';

    setTimeout(function () {
        toast.style.display = 'none';
    }, 500);
}

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
    }

    xhr.method = "POST";
    xhr.responseType = "json";
    xhr.open("POST", "/sharing/trace/browser", true);

    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(data));
}
