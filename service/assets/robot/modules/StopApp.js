
module.exports = function(appName) {

    if (app.openAppSetting(app.getPackageName(appName))) {
        var el = $.text(appName).findOne(5000);
        if (null != el) {
            el = $.textContains('结束').findOne(2000);
            if (null != el) {
                var location = el.bounds();
                click(location.centerX(), location.centerY());

                $.textContains('强制停止').findOne(2000);
                el = $.text('确定').findOnce();
                location = el.bounds();
                click(location.centerX(), location.centerY());
            }
            else {
                // 适配 VIVO
                el = $.textContains('强行停止').findOne(2000);
                var location = el.bounds();
                click(location.centerX(), location.centerY());

                sleep(2000);

                el = $.text('确定').findOne(2000);
                location = el.bounds();
                click(location.centerX(), location.centerY());
            }
        }
    }

}
