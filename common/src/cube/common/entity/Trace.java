/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.common.entity;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 痕迹。
 */
public class Trace implements JSONable {

    private Contact contact;

    private long contactId = 0;

    /**
     * 子列表。
     */
    private List<Trace> children;

    public Trace(Contact contact) {
        this.contact = contact;
    }

    public Trace(long contactId) {
        this.contactId = contactId;
    }

    public Trace(JSONObject json) {
        if (json.has("contact")) {
            this.contact = new Contact(json.getJSONObject("contact"));
        }
        else {
            this.contactId = json.has("contactId") ? json.getLong("contactId") : 0;
        }
    }

    public long getContactId() {
        if (null != this.contact) {
            return this.contact.getId();
        }
        else {
            return this.contactId;
        }
    }

    public void addChildren(List<Trace> traces) {
        synchronized (this) {
            if (null == this.children) {
                this.children = new ArrayList<>();
            }

            this.children.addAll(traces);
        }
    }

    public List<Trace> getChildren() {
        return this.children;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (null != this.contact) {
            json.put("contact", this.contact.toBasicJSON());
        }
        else {
            json.put("contactId", this.contactId);
        }
        json.put("string", this.toString());
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    @Override
    public String toString() {
        String string = null;

        long id = this.getContactId();

        // 1. 从 49-113 选取1个随机数字作为加数（最大值不能超过 113，防止相加之后数值超过两位数）
        // 2. 将 ID 转为字符串，将每位数字依次解析为 int 型
        // 3. 使用随机加数依次将每位数字相加，得到 int 数组
        // 4. 将数组每个 int 值拼接为字符串，如果 int 值范围为 65-90，97-122 则视为 ASC 码，转为对应的字符
        // 5. 将随机加数添加到结尾，不足3位的前补 0
        // 例：ID 为 11240809 可转为
        //    SSTVRZR91082
        //    llmokskt107
        //    5252535551595160051
        //    91919294ZbZc090

        int randDelta = Utils.randomInt(49, 113);
        String idString = Long.toString(id);

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < idString.length(); ++i) {
            int value = Integer.parseInt(String.valueOf(idString.charAt(i)));
            // 与随机加数相加
            value += randDelta;
            // 拼接
            if ((value >= 65 && value <= 90) || (value >= 97 && value <= 122)) {
                buf.append(String.valueOf((char)value));
            }
            else {
                buf.append(Integer.toString(value));
            }
        }

        if (randDelta < 100) {
            buf.append("0");
        }
        buf.append(Integer.toString(randDelta));

        string = buf.toString();

        return string;
    }

    public static long parseString(String string) throws Exception {
        // 取后两位加数
        String dstr = string.substring(string.length() - 3);
        int delta = Integer.parseInt(dstr);

        // 逐一还原每位数据
        List<String> plainList = new ArrayList<>();
        String plain = string.substring(0, string.length() - 3);
        for (int i = 0; i < plain.length(); ++i) {
            char c = plain.charAt(i);
            int value = (int) c;
            if ((value >= 65 && value <= 90) || (value >= 97 && value <= 122)) {
                plainList.add(String.valueOf(c));
            }
            else {
                plainList.add(String.valueOf(c) + String.valueOf(plain.charAt(i + 1)));
                ++i;
            }
        }

        // 逐一将每位减去加数
        StringBuilder buf = new StringBuilder();
        for (String text : plainList) {
            if (text.length() == 1) {
                int asc = (int) text.charAt(0);
                buf.append(Integer.toString(asc - delta));
            }
            else {
                int num = Integer.parseInt(text);
                buf.append(Integer.toString(num - delta));
            }
        }

        return Long.parseLong(buf.toString());
    }

//    public static void main(String[] args) {
//        Contact contact = new Contact(11240809, "domain");
//        Trace trace = new Trace(contact);
//        try {
//            for (int i = 0; i < 10; ++i) {
//                String s = trace.toString();
//                System.out.print(s);
//                System.out.print(" - ");
//                System.out.println(Trace.parseString(s));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
