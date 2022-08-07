// index.js

window.sn = Date.now();
var sharer = '';
var parent = '';
var token = '';

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
        else if (pair[0] == 't') {
            token = pair[1];
        }
    }

    // 尝试读取 Cookie
    var cookie = readCookie('CubeAppToken');
    if (null != cookie && cookie.length >= 32) {
        token = cookie;
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
            "parent": parent,
            "token": token
        }
    };

    submit(data);

    resizePreview();
};

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
    if (sharingTag.config.traceDownload) {
        if (token.length < 1) {
            // 需要登录
            showToast('您需要登录之后才能下载该文件<br/>3秒后跳转到登录界面');
            setTimeout(function() {
                var jump = encodeURI(document.URL);
                var href = appLoginURL + ((appLoginURL.indexOf('?') > 0) ? '&' : '?') + "jump=" + jump;
                window.location.href = href;
            }, 3200);
            return;
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
        "event": "Extract",
        "eventTag": "download",
        "eventParam": {
            "sn": window.sn,
            "url": url,
            "sharer": sharer,
            "parent": parent,
            "token": token
        }
    };

    submit(data, function() {
        window.location.href = url;
    });
}

function copy() {
    var el = document.getElementById('sharing_url');
    el.focus();
    el.select();
    document.execCommand('copy');

    showToast('分享链接已复制到剪贴板');
}

var toastTimer  = 0;

function showToast(text, closeDelay) {
    if (toastTimer > 0) {
        clearTimeout(toastTimer);
    }

    var toast = document.getElementById('toast');
    toast.style.display = 'table';
    toast.style.animation = 'toast-show-anim 0.5s';

    if (undefined !== text) {
        var textEl = document.querySelector('.toast-text');
        textEl.innerHTML = text;
    }

    toastTimer = setTimeout(function () {
        toastTimer = 0;
        hideToast();
    }, (undefined !== closeDelay) ? closeDelay : 3000);
}

function hideToast() {
    var toast = document.getElementById('toast');
    toast.style.animation = 'toast-hide-anim 0.5s';

    toastTimer = setTimeout(function () {
        toastTimer = 0;
        toast.style.display = 'none';
    }, 500);
}

function submit(data, complete) {
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
            if (undefined !== complete) {
                complete();
            }
        }
    };

    xhr.method = "POST";
    xhr.responseType = "json";
    xhr.open("POST", "/sharing/trace/browser", true);

    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(data));
}

function readCookie(cname) {
    var name = cname + "=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var ca = decodedCookie.split(';');
    for (var i = 0; i <ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return null;
}
