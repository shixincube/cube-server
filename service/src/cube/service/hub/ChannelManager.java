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

package cube.service.hub;

import cell.core.talk.LiteralBase;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.hub.Product;
import cube.hub.dao.ChannelCode;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * 授权管理器。
 */
public class ChannelManager {

    private final StorageField[] channelCodeFields = new StorageField[]{
            new StorageField("id", LiteralBase.LONG, new Constraint[]{
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            // 授权码
            new StorageField("code", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 创建时间戳
            new StorageField("creation", LiteralBase.LONG, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 到期时间戳
            new StorageField("expiration", LiteralBase.LONG, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 产品
            new StorageField("product", LiteralBase.STRING, new Constraint[]{
                    Constraint.NOT_NULL
            }),
            // 状态
            new StorageField("state", LiteralBase.INT, new Constraint[]{
                    Constraint.NOT_NULL
            })
    };

    private final String channelCodeTable = "hub_channel_code";

    private Storage storage;

    public ChannelManager(JSONObject config) {
        this.storage = StorageFactory.getInstance().createStorage(StorageType.MySQL, "HubAuth", config);
    }

    public void start() {
        this.storage.open();
        this.execSelfChecking();
    }

    public void stop() {
        this.storage.close();
    }

    public ChannelCode getChannelCode(String code) {
        List<StorageField[]> result = this.storage.executeQuery(this.channelCodeTable, this.channelCodeFields, new Conditional[] {
                Conditional.createEqualTo("code", code)
        });
        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));
        return new ChannelCode(map.get("code").getString(), map.get("creation").getLong(),
                map.get("expiration").getLong(), Product.parse(map.get("product").getString()),
                map.get("state").getInt());
    }

    public ChannelCode createChannelCode(Product product, long expiredDuration) {
        long now = System.currentTimeMillis();
        ChannelCode channelCode = new ChannelCode(Utils.randomString(32), now,
                now + expiredDuration, product, ChannelCode.ENABLED);
        this.storage.executeInsert(this.channelCodeTable, new StorageField[] {
                new StorageField("code", channelCode.code),
                new StorageField("creation", channelCode.creation),
                new StorageField("expiration", channelCode.expiration),
                new StorageField("product", channelCode.product.name),
                new StorageField("state", channelCode.state),
        });
        return channelCode;
    }

    private void execSelfChecking() {
        if (!this.storage.exist(this.channelCodeTable)) {
            // 不存在，建新表
            if (this.storage.executeCreate(this.channelCodeTable, this.channelCodeFields)) {
                Logger.i(this.getClass(), "Created table '" + this.channelCodeTable + "' successfully");
            }
        }
    }
}
