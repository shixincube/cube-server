
module.exports = {
    // 作者 - id : title
    // 描述 - id : desc
    // 点赞 - android.widget.LinearLayout | desc 未点赞，喜欢1.8w，按钮
    // 评论 - android.widget.LinearLayout | desc 评论2482，按钮
    // 收藏 - android.widget.LinearLayout | desc 未选中，收藏920，按钮
    // 分享 - android.widget.LinearLayout | desc 分享1.0万，按钮
    // 相关搜索 - id : content

    getInfo: function() {
        var result = {
            "author": "",
            "desc": "",
            "like": "",
            "comment": "",
            "collect": "",
            "share": "",
            "relevantContent": ""
        };

        var el = $.id('title').findOnce();
        if (null != el) {
            result.author = el.text().substring(1);
        }

        el = $.id('desc').findOnce();
        if (null != el) {
            result.desc = el.text();
        }

        var regex = "\\S喜欢\\S*，按钮";
        el = $.descMatches(regex).findOne(1000);
        if (null != el) {
            var desc = el.desc();
            var tmp = desc.split('，')[1];
            result.like = tmp.substring(2);
        }

        regex = "评论\\S*，按钮";
        el = $.descMatches(regex).findOne(1000);
        if (null != el) {
            var desc = el.desc();
            var tmp = desc.split('，')[0];
            result.comment = tmp.substring(2);
        }
        else {
            // 评论抢首评，按钮
        }

        regex = "\\S收藏\\S*，按钮";
        el = $.descMatches(regex).findOne(1000);
        if (null != el) {
            var desc = el.desc();
            var tmp = desc.split('，')[1];
            result.collect = tmp.substring(2);
        }

        regex = "分享\\S*，按钮";
        el = $.descMatches(regex).findOne(1000);
        if (null != el) {
            var desc = el.desc();
            var tmp = desc.split('，')[0];
            result.share = tmp.substring(2);
        }

        el = $.id('content').findOnce();
        if (null != el) {
            result.relevantContent = el.text();
        }

        return result;
    },

    submitInfo: function() {
        var info = this.getInfo();
        if (info.author.length === 0) {
            return;
        }

        var reportContent = info;
        reportContent["app"] = 'com.ss.android.ugc.aweme';
        reportContent["appName"] = '抖音';

        // 提交数据
        report.submit('com.ss.android.ugc.aweme', 'DailyAuto', 'VideoInfo', reportContent);
    }
}
