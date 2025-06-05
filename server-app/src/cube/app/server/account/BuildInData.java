/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
        // Password: shixincube

        /* 2025-6-5 作废
        Account account = new Account(50001001, AuthConsts.DEFAULT_DOMAIN, "cube1", "13199887766",
                "c7af98d321febe62e04d45e8806852e0",
                "李国诚", "avatar01", 0);
        account.region = "北京";
        account.department = "产品中心";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001002, AuthConsts.DEFAULT_DOMAIN, "cube2", "13288776655",
                "c7af98d321febe62e04d45e8806852e0",
                "王沛珊", "avatar13", 0);
        account.region = "武汉";
        account.department = "媒介部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001003, AuthConsts.DEFAULT_DOMAIN, "cube3", "13377665544",
                "c7af98d321febe62e04d45e8806852e0",
                "郝思雁", "avatar15", 0);
        account.region = "上海";
        account.department = "公关部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001004, AuthConsts.DEFAULT_DOMAIN, "cube4", "13466554433",
                "c7af98d321febe62e04d45e8806852e0",
                "高海光", "avatar09", 0);
        account.region = "成都";
        account.department = "技术部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);

        account = new Account(50001005, AuthConsts.DEFAULT_DOMAIN, "cube5", "13555443322",
                "c7af98d321febe62e04d45e8806852e0",
                "张明宇", "avatar12", 0);
        account.region = "广州";
        account.department = "设计部";
        account.registration = System.currentTimeMillis();
        this.accountList.add(account);
        */
    }
}
