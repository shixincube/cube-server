// index.js

window.onload = function () {
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
        "referrer": document.referrer,
        "language": navigator.language,
        "userAgent": navigator.userAgent
    };

    submit(data);
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
            alert('submit');
        }
    }

    xhr.method = "POST";
    xhr.responseType = "json";
    xhr.open("POST", "/sharing/trace", true);

    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(data));
}
