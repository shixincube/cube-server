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

package cube.app.server.account;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置数据。
 */
public final class BuildInData {

    public final List<Account> accountList = new ArrayList<>();

    public BuildInData() {
        Account account = new Account(50001001, "cube1", "13199887766",
                "c7af98d321febe62e04d45e8806852e0",
                "李国诚", "avatar01", 0);
        account.region = "北京";
        account.department = "产品中心";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001002, "cube2", "13288776655",
                "c7af98d321febe62e04d45e8806852e0",
                "王沛珊", "avatar13", 0);
        account.region = "武汉";
        account.department = "媒介部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001003, "cube3", "13377665544",
                "c7af98d321febe62e04d45e8806852e0",
                "郝思雁", "avatar15", 0);
        account.region = "上海";
        account.department = "公关部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001004, "cube4", "13466554433",
                "c7af98d321febe62e04d45e8806852e0",
                "高海光", "avatar09", 0);
        account.region = "成都";
        account.department = "技术部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001005, "cube5", "13555443322",
                "c7af98d321febe62e04d45e8806852e0",
                "张明宇", "avatar12", 0);
        account.region = "广州";
        account.department = "设计部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);
    }
}
