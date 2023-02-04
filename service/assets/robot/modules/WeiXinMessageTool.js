
module.exports = {

    getAccount: function() {
        var account = {
            name: '',
            wid: ''
        };

        var coll = $.className('android.widget.ListView').find();
        if (coll.nonEmpty()) {
            // 定位到顶部的 ListView
            var el = coll.get(2);
            // 定位到账号名
            var con = el.child(0).child(1).child(0).child(1); // LinearLayout
            account.name = con.child(0).child(0).text();
            account.wid = con.child(1).child(0).text();
            account.wid = account.wid.substring(4);
        }

        return account;
    },

    /**
     * 获取消息列表。
     * @param accountName 当前微信账号。
     * @param conversationName 消息会话名称。
     * @param handleFile 是否上传文件。
     * @returns {null|[]}
     */
    getMessageList: function(accountName, conversationName, handleFile) {
        var listView = $.className('androidx.recyclerview.widget.RecyclerView').findOne(3000);
        if (null == listView) {
            return null;
        }

        if (null != $.desc('设置').findOnce()) {
            // 公众号
            return null;
        }

        var list = [];
        var dateDesc = '';
        var node = null;

        var cx = Math.floor(device.width * 0.5);
        var cy = Math.floor(device.height * 0.5);
        var halfHeight = cy;
        var y1 = cy + (halfHeight - 160);
        var y2 = cy - (halfHeight - 200);
        var swipeCount = 0;
        var findDateDesc = false;

        // 滑动到有时间标签的位置
        while (swipeCount < 10) {
            for (var i = 0; i < listView.childCount(); ++i) {
                node = listView.child(i);
                if (node.className() == 'android.widget.RelativeLayout') {
                    if (node.childCount() == 2) {
                        findDateDesc = true;
                        break;
                    }
                }
            }

            if (!findDateDesc) {
                // 滑动
                //log('XJW: ' + cx + ' - ' + y2 + ' -> ' + y1);
                swipe(cx, y2, cx, y1, 500);
                sleep(1000);
                ++swipeCount;

                listView = $.className('androidx.recyclerview.widget.RecyclerView').findOnce();
            }
            else {
                break;
            }
        }

        while (swipeCount >= 0 && findDateDesc) {
            for (var i = 0; i < listView.childCount(); ++i) {
                node = listView.child(i);
                if (node.className() == 'android.widget.RelativeLayout') {
                    var message = {
                        "time": Date.now(),
                        "date": "",
                        "sender": "",
                        "content": "",
                        "subtitle": ""
                    };

                    if (node.childCount() == 1) {
                        message.date = dateDesc;
                        // 消息内容
                        var contentView = node.child(0).child(1);   // LinearLayout
                        // 发送人
                        message.sender = contentView.child(0).text();
                        // 消息内容
                        this.extractContent(message, contentView.child(1), handleFile,
                            conversationName, message.sender); // RelativeLayout or LinearLayout
                    }
                    else if (node.childCount() == 2) {
                        // 带时间戳的消息内容
                        var dateView = node.child(0);   // TextView
                        dateDesc = dateView.text();
                        message.date = dateDesc;

                        var contentView = node.child(1);    // LinearLayout
                        if (contentView.childCount() == 3) {
                            // 本人的消息
                            var conNode = contentView.child(1).child(0).child(0).child(0);
                            if (conNode.className() == 'android.widget.TextView') {
                                // 文本消息
                                message.content = conNode.text();
                            }
                        }
                        else if (contentView.childCount() == 2) {
                            // 其他人的消息
                            var msgLayout = contentView.child(1);   // LinearLayout
                            message.sender = msgLayout.child(0).text();

                            // 尝试获取消息内容
                            this.extractContent(message, msgLayout.child(1), handleFile,
                                conversationName, message.sender);
                        }
                    }

                    list.push(message);
                }
            }

            if (swipeCount > 0) {
                swipe(cx, y1, cx, y2, 500);
                sleep(1000);

                listView = $.className('androidx.recyclerview.widget.RecyclerView').findOne(2000);
            }

            --swipeCount;
        }

        return list;
    },

    extractContent: function(message, parentLayout, handleFile, conversationName, senderName) {
        if (parentLayout.className() == 'android.widget.RelativeLayout') {
            var node = parentLayout.child(0);
            if (node.className() == 'android.widget.TextView') {
                message.content = node.text();
            }
        }
        else if (parentLayout.className() == 'android.widget.LinearLayout') {
            var node = parentLayout.child(0).child(0);
            if (node.className() == 'android.widget.FrameLayout') {
                var col = node.find($.className('android.widget.TextView'));
                if (col.size() == 2) {
                    message.content = col.get(0).text();
                    message.subtitle = col.get(1).text();
                }

                if (handleFile) {
                    var filename = this.processFile(conversationName, senderName, node);
                    if (null != filename) {
                        message.filename = filename;
                    }
                }
            }
        }
        else if (parentLayout.className() == 'android.widget.FrameLayout') {
            var node = parentLayout.child(1);
            if (node.desc() == '图片') {
                message.content = '[图片]';
            }
        }
    },

    /**
     *
     * @param conversationName
     * @param senderName
     * @param node
     * @returns {string} 返回上传的文件名。
     */
    processFile: function(conversationName, senderName, node) {
        // 点击打开文件
        var location = node.bounds();
        click(location.centerX(), location.centerY());
        sleep(3000);    // 等待文件加载

        var el = $.desc('更多').findOne(3000);
        if (null == el) {
            return null;
        }

        // 弹出更多操作菜单
        location = el.bounds();
        click(location.centerX(), location.centerY());
        sleep(1000);

        el = $.text('保存').findOne(1000);
        if (null == el) {
            return null;
        }

        var filename = null;

        // 点击保存
        location = el.bounds();
        click(location.centerX(), location.centerY());

        // 等待保存
        sleep(3000);

        var path = files.getSdcardPath() + '/Download/WeiXin/';
        if (files.exists(path)) {
            // 找到最新的一个文件，也就是时间戳值最大的文件
            var curFilePath = null;
            var curLast = 0;
            var array = files.listDir(path);
            array.forEach(function(item) {
                if (files.isFile(path + item)) {
                    var last = files.lastModified(path + item);
                    if (last > curLast) {
                        curLast = last;
                        curFilePath = path + item;
                    }
                }
            });

            if (null != curFilePath) {
                // 提交文件
                filename = report.submitFile('CubeWeiXinMonitor', conversationName, senderName, curFilePath);
            }
        }

        // 回到消息列表界面
        back();
        sleep(2000);

        return filename;
    }
}
