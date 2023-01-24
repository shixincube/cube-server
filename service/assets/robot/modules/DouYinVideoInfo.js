
module.exports = {
    // 作者 - id : title
    // 描述 - id : desc
    // 点赞 - android.widget.LinearLayout | desc 未点赞，喜欢1.8w，按钮
    // 评论 - android.widget.LinearLayout | desc 评论2482，按钮
    // 收藏 - android.widget.LinearLayout | desc 未选中，收藏920，按钮
    // 分享 - android.widget.LinearLayout | desc 分享1.0万，按钮
    // 相关搜索 - id : content

    // 分享按钮位置
    shareButtonLocation: null,

    getInfo: function() {
        var result = {
            "author": "",
            "desc": "",
            "like": "",
            "comment": "",
            "collect": "",
            "share": "",
            "relevantContent": "",
            "link": ""
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

        var location = null;

        regex = "分享\\S*，按钮";
        el = $.descMatches(regex).findOne(1000);
        if (null != el) {
            var desc = el.desc();
            var tmp = desc.split('，')[0];
            result.share = tmp.substring(2);

            if (null == this.shareButtonLocation) {
                location = el.bounds();
                this.shareButtonLocation = location;
            }
            else {
                location = this.shareButtonLocation;
            }
        }

        el = $.id('content').findOnce();
        if (null != el) {
            result.relevantContent = el.text();
        }

        // 视频链接地址
        if (null != location) {
            var x = location.centerX();
            var y = location.centerY();
            if (x > 0 && y > 0) {
                sleep(1000);

                // 点击"分享"按钮，弹出菜单
                click(x, y);

                sleep(1000);

                el = $.text('分享到').findOne(2000);
                if (null == el) {
                    log('点击"分享"按钮后，未检测到菜单');
                    return result;
                }

                el = $.text('复制链接').findOnce();
                if (null == el) {
                    log('未检测到"复制链接"');
                    return result;
                }

                location = el.bounds();
                x = location.centerX();
                y = location.centerY() - 20;
                // 点击复制链接图标
                if (x > 0 && y > 0) {
                    click(x, y);
                    // 等待链接复制
                    sleep(2000);
                    // 获取剪贴板内容
                    result.link = getClip();
                    if (result.link.length == 0) {
                        sleep(1000);
                        result.link = getClip();
                    }
                }
                else {
                    log('点击"复制链接"时未获取到按钮的正确 bounds');
                }
            }
        }

        return result;
    }
}
