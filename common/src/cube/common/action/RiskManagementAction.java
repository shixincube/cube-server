/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * 风控模块动作。
 */
public enum RiskManagementAction {

    /**
     * 获取联系人行为列表。
     */
    ListContactBehaviors("listContactBehaviors"),

    /**
     * 获取联系人风险掩码。
     */
    GetContactRisk("getContactRisk"),

    /**
     * 修改联系人风险掩码。
     */
    ModifyContactRisk("modifyContactRisk"),

    ;

    public final String name;

    RiskManagementAction(String name) {
        this.name = name;
    }
}
