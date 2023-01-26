
module.exports = {

    getAccount: function() {
        var account = {
            name: '',
            wid: ''
        };



        return account;
    },

    getMessageList: function() {
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
                    this.extractContent(message, contentView.child(1)); // RelativeLayout
                }
                else if (node.childCount() == 2) {
                    // 带时间戳的消息内容
                    var dateView = node.child(0);   // TextView
                    dateDesc = dateView.text();
                    message.date = dateDesc;

                    /*
                    D/GlobalConsole: 21:13:50.939 [D] XJW self: 癸卯年，好
                    D/GlobalConsole: 21:13:51.031 [D] XJW other: 徐江威 - (textview: 2)
                    */

                    var contentView = node.child(1);    // LinearLayout
                    if (contentView.childCount() == 3) {
                        // 本人的消息
                        var conNode = contentView.child(1).child(0).child(0).child(0);
                        if (conNode.className() == 'android.widget.TextView') {
                            // 文本消息
                            message.content = conNode.text();
                            // log('XJW self: ' + message.content);
                        }
                    }
                    else if (contentView.childCount() == 2) {
                        // 其他人的消息
                        var msgLayout = contentView.child(1);   // LinearLayout
                        message.sender = msgLayout.child(0).text();

                        // 尝试获取消息内容
                        this.extractContent(message, msgLayout.child(1));
                    }
                }

                list.push(message);
            }
        }

        return list;
    },

    extractContent: function(message, parentLayout) {
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
            }
        }
    }
}
