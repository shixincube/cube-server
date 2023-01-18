
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
                // 适配 VIVO/华为
                el = $.textContains('强行停止').findOne(2000);
                var location = el.bounds();
                click(location.centerX(), location.centerY());

                sleep(2000);

                el = $.text('确定').findOne(1000);    // VIVO是"确定"按钮
                if (null == el) {
                    //el = $.text('强行停止').findOne(1000);  // 华为是"强行停止"按钮
                    var c = $.className('Button').find();
                    if (c.nonEmpty()) {
                        for (var i = 0; i < c.size(); ++i) {
                            var child = c.get(i);
                            if (child.text() == '强行停止') {
                                el = child;
                                break;
                            }
                        }
                    }
                }

                if (null != el) {
                    location = el.bounds();
                    click(location.centerX(), location.centerY());
                }
            }
        }
    }

}
