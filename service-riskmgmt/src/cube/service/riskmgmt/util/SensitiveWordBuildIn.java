/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.riskmgmt.util;

import cube.service.riskmgmt.SensitiveWord;

import java.util.ArrayList;

/**
 * 内置的敏感词数据。
 */
public class SensitiveWordBuildIn {

    public final SensitiveWord[] sensitiveWords = new SensitiveWord[] {
            new SensitiveWord("毛泽东", SensitiveWord.SensitiveWordType.Unallowed),
            new SensitiveWord("周恩来", SensitiveWord.SensitiveWordType.Unallowed),
            new SensitiveWord("朱德", SensitiveWord.SensitiveWordType.Unallowed),
            new SensitiveWord("刘少奇", SensitiveWord.SensitiveWordType.Unallowed),
            new SensitiveWord("邓小平", SensitiveWord.SensitiveWordType.Unallowed),
            new SensitiveWord("共贪党", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("发抡功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法轮功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法 轮 功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法*功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法.轮.功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法L功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法lun功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法轮大法", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法轮佛法", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法轮功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法十轮十功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法西斯", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("珐.輪功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("氵去车仑工力", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("falun", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("Falundafa", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("fa轮", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("Flg", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("摩门教", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("三水法轮", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法轮", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("轮法功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("三去车仑", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("氵去车仑", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("发论工", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法x功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法o功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法0功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法一轮一功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("车仑工力", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法lun", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("fa轮", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("法lg", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("fl功", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("falungong", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("暴干", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("暴奸", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("暴乳", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("爆乳", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("暴淫", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("屄", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("被操", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("被插", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("被干", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插暴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("操逼", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("操黑", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("操烂", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("肏你", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("肏死", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("操死", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("操我", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("厕奴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插比", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插b", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插逼", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插进", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插你", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插我", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("插阴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("潮吹", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("潮喷", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("吃精", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("扌由插", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("大力抽送", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("多人轮", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("肥逼", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("粉穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("干死你", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("干穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("肛交", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("龟头", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("国产av", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("黑逼", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("后庭", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("后穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("换妻俱乐部", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("鸡吧", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("鸡巴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("鸡奸", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("妓女", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("集体淫", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("奸情", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("脚交", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("精液", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("巨屌", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("菊花洞", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("菊门", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("巨奶", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("菊穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("开苞", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("口爆", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("口交", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("口射", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("口淫", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("狂操", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("狂插", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("浪逼", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("浪妇", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("浪叫", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("浪女", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("流淫", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("凌辱", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("露b", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("乱交", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("乱伦", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("轮暴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("轮操", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("美逼", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("美穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("迷奸", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("密穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("蜜穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("蜜液", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("母奸", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("奶子", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("男奴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("内射", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("嫩逼", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("嫩女", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("嫩穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("喷精", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("A片", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("fuck", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("博彩", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("BOCAI", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("巨乳", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("巨穴", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("巨根", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("浪B", SensitiveWord.SensitiveWordType.Degraded),
            new SensitiveWord("澳门博彩", SensitiveWord.SensitiveWordType.Illegal),
            new SensitiveWord("澳门赌场", SensitiveWord.SensitiveWordType.Illegal)
    };

    public SensitiveWordBuildIn() {
    }
}
