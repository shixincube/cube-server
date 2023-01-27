// 微信消息监视器

const ignoreList = require('WeiXinIgnoreList');
const messageTool = require('WeiXinMessageTool');

if (!launchApp('微信')) {
    console.log('没有安装微信');
    exit();
}

const dataReport = {
    account: {
        name: '',
        wid: ''
    },
    conversations: []
};

var tabChat = $.text('微信').findOne(5000);
var tabContacts = $.text('通讯录').findOne(2000);
var tabDiscover = $.text('发现').findOne(2000);
var tabProfile = $.text('我').findOnce();

const ignoreConversations = ignoreList.getIgnoreConversations();

if (null != tabChat && null != tabContacts && null != tabDiscover && null != tabProfile) {
    // 修正标签按钮元素
    // 定位到底部的标签 View
    var coll = $.className('android.view.ViewGroup').find();
    var con = coll.get(1);
    con = con.child(0).child(0).child(0).child(1);  // RelativeLayout
    con = con.child(0); // LinearLayout
    tabChat = con.child(0);
    tabContacts = con.child(1);
    tabDiscover = con.child(2);
    tabProfile = con.child(3);

    // 已启动
    var location = null;

    // 读取账号数据
    location = tabProfile.bounds();
    click(location.centerX(), location.centerY());
    sleep(1000);
    dataReport.account = messageTool.getAccount();

    // 读取消息列表
    location = tabChat.bounds();
    click(location.centerX(), location.centerY());
    sleep(1000);
    const listView = $.className('android.widget.ListView').findOnce();
    for (var i = 0; i < listView.childCount(); ++i) {
        var child = listView.child(i);
        if (child.className() == 'android.widget.LinearLayout') {
            if (child.childCount() > 0) {
                // 获取子布局
                var node = child.child(0);
                if (node.childCount() == 2) {
                    // console.log('TEST: ' + node.child(0).className() + " - " + node.child(1).className());

                    // 判断是有新消息
                    var avatarNode = node.child(0);
                    if (avatarNode.childCount() != 2) {
                        // 没有提示气泡
                        // XJW ：android.widget.RelativeLayout
                        // XJW ：android.widget.RelativeLayout
                        //log('XJW ：' + avatarNode.className());
                        //continue;
                    }

                    node = node.child(1);   // 第二列，消息列表的聊天标题列
                    location = node.bounds();
                    var x = location.centerX();
                    var y = location.centerY();
                    node = node.child(0).child(0).child(0);
                    var name = node.text();
                    if (ignoreConversations.indexOf(name) >= 0) {
                        // 跳过忽略的会话
                        continue;
                    }

                    var data = {
                        "conversation": name,
                        "messages": []
                    };

                    // 进入会话
                    click(x, y);

                    $.desc('返回').findOne(3000);

                    var messageList = messageTool.getMessageList();
                    if (null != messageList) {
                        data.messages = messageList;
                    }

                    dataReport.conversations.push(data);

                    back();

                    sleep(2000);
                }
            }
        }
    }

    report.submit('CubeWeiXinMonitor', 'Result', dataReport.account.name, dataReport);
}
else {
    log('微信未登录');
}

//require('StopApp')('微信');

sleep(1000);

//launchApp('Roboengine');
back();
