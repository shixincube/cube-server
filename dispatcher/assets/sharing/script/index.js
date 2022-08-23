// index.js

window.sn = Date.now();
var sharer = '';
var parent = '';
var token = '';

if (undefined === window.screen) {
    window.screen = {
        width: 0,
        height: 0,
        colorDepth: 0
    };
}

var screenOrientation = null;
if (undefined === screen.orientation) {
    screenOrientation = 'unknown';
}
else {
    screenOrientation = (undefined === screen.orientation.type) ? screen.orientation : screen.orientation.type;
}

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
            "orientation": screenOrientation
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

    initUI();
    resizePreview();
};

window.onresize = function () {
    setTimeout(function() {
        resizePreview();
    }, 1000);
}

function resizePreview() {
    var preview = document.querySelector('.preview');
    preview.style.height = (parseInt(document.body.clientHeight) - 43) + 'px';
}

function initUI() {
    var inputElArray = [];

    if (sharingTag.config.hasPassword) {
        var getPassword = function() {
            var values = [];
            for (var i = 0; i < 6; ++i) {
                var value = inputElArray[i].value;
                if (value.length == 0) {
                    return null;
                }

                values.push(value);
            }

            return values.join('');
        }

        var enableInput = function(enabled) {
            inputElArray.forEach(function(el) {
                el.disabled = !enabled;
            });
        }

        var resetInput = function() {
            inputElArray.forEach(function(el) {
                el.value = '';
            });
            inputElArray[0].focus();
        }

        var submitCheck = function(password) {
            showToast('正在校验访问码……', 10000);
            enableInput(false);

            // 提交密码
            checkPassword(password, function(valid) {
                enableInput(true);

                if (valid) {
                    hideToast();
                    showValidPage();
                }
                else {
                    showToast('访问码错误，请重新输入', 3000);
                    resetInput();
                }
            });
        }

        var keyUp = function(e) {
            var id = e.target.id;
            var value = e.target.value;
            if (value.length == 0) {
                return;
            }
            else if (value.length > 1) {
                value = value.substring(value.length - 1, value.length);
                e.target.value = value;
            }

            var sn = parseInt(id.split("_")[1]);

            if (sn == 6) {
                var password = getPassword();
                submitCheck(password);
            }
            else {
                var password = getPassword();
                if (null != password) {
                    submitCheck(password);
                    return;
                }

                // 焦点切换到下一个
                inputElArray[sn].focus();
            }
        }

        var el = document.querySelector('.password-input');
        el.style.visibility = 'visible';
        for (var i = 1; i <= 6; ++i) {
            var inputEl = document.getElementById('code_' + i);
            inputEl.onkeyup = keyUp;
            inputEl.value = '';
            inputElArray.push(inputEl);
        }

        inputElArray[0].focus();
    }
}

function showValidPage() {
    document.querySelector('.password-input').style.visibility = 'hidden';

    if (undefined !== sharingTag.previewList) {
        document.querySelector('.header').style.display = 'block';
        document.querySelector('.preview').style.display = 'block';
        document.querySelector('.main').style.display = 'none';
    }

    if (sharingTag.config.download) {
        document.querySelectorAll('.download-col').forEach(function(el) {
            el.style.display = 'inline-block';
        });
    }
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
            "orientation": screenOrientation
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
        if (token.length > 1) {
            url = url + '&token=' + token;
        }
        window.location.href = url;
    });
}

function qrcode() {
    var el = document.querySelector('.qrcode');
    if (el.style.display == 'block') {
        el.style.display = 'none';
    }
    else {
        el.style.display = 'block';
    }
}

function copy() {
    var el = document.getElementById('sharing_url');
    el.focus();
    el.select();
    document.execCommand('copy');

    showToast('分享链接已复制到剪贴板');

    const data = {
        "domain": document.domain,
        "url": document.URL,
        "title": document.title,
        "screen": {
            "width": window.screen.width,
            "height": window.screen.height,
            "colorDepth": window.screen.colorDepth,
            "orientation": screenOrientation
        },
        "language": navigator.language,
        "userAgent": navigator.userAgent,
        "event": "Share",
        "eventParam": {
            "sn": window.sn,
            "sharer": sharer,
            "parent": parent,
            "token": token
        }
    };

    submit(data);
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

function checkPassword(password, complete) {
    var data = {
        domain: sharingTag.domain,
        code: sharingTag.code,
        password: password
    };

    var xhr = null;
    if (window.XMLHttpRequest) {
        xhr = new XMLHttpRequest();
    }
    else {
        xhr = new ActiveXObject("Microsoft.XMLHTTP");
    }

    xhr.onreadystatechange = function() {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                complete(true)
            }
            else {
                complete(false);
            }
        }
    };

    xhr.method = "POST";
    xhr.responseType = "json";
    xhr.open("POST", "/sharing/check/", true);

    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(data));
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
