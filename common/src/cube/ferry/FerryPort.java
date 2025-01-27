/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

/**
 * Port 动作描述。
 * 主要用于 Ferry 和 Tenet 两个 Action 的数据操作。
 */
public class FerryPort {

    public final static String TransferIntoMember = "TransferIntoMember";

    public final static String TransferOutMember = "TransferOutMember";

    public final static String ResetLicence = "ResetLicence";

    public final static String WriteMessage = "WriteMessage";

    public final static String UpdateMessage = "UpdateMessage";

    public final static String DeleteMessage = "DeleteMessage";

    public final static String BurnMessage = "BurnMessage";

    public final static String SaveFile = "SaveFile";

    public final static String Cleanup = "Cleanup";

    private FerryPort() {
    }
}
