/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.app.server.notice;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.app.server.util.AbstractStorage;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;
import cube.storage.StorageFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 通知存储器。
 */
public class NoticeStorage extends AbstractStorage {

    public final static String TABLE_NOTICE = "notice";

    private final StorageField[] noticeFields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTO_INCREMENT
            }),
            new StorageField("id", LiteralBase.LONG, new Constraint[] {
                    Constraint.UNIQUE
            }),
            new StorageField("domain", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("title", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("content", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("content_url", LiteralBase.STRING, new Constraint[] {
                    Constraint.DEFAULT_NULL
            }),
            new StorageField("type", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("creation", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("expires", LiteralBase.LONG, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    public NoticeStorage(Properties properties) {
        super("NoticeStorage", properties);
    }

    public void open() {
        this.storage.open();

        if (!this.storage.exist(TABLE_NOTICE)) {
            this.storage.executeCreate(TABLE_NOTICE, this.noticeFields);
        }

        Logger.i(this.getClass(), "Open");
    }

    public void close() {
        this.storage.close();

        Logger.i(this.getClass(), "Close");
    }

    public void write(Notice notice) {
        this.storage.executeInsert(TABLE_NOTICE, new StorageField[] {
                new StorageField("id", notice.getId()),
                new StorageField("domain", notice.getDomain()),
                new StorageField("title", notice.getTitle()),
                new StorageField("content", notice.getContent()),
                new StorageField("content_url", notice.getContentURL()),
                new StorageField("type", notice.getType()),
                new StorageField("creation", notice.getCreation()),
                new StorageField("expires", notice.getExpires()),
        });
    }

    /**
     * 返回有效的通知。
     *
     * @return
     */
    public List<Notice> readNotices(String domainName) {
        List<Notice> noticeList = new ArrayList<>();

        long now = System.currentTimeMillis();
        List<StorageField[]> result = this.storage.executeQuery(TABLE_NOTICE, this.noticeFields, new Conditional[] {
                Conditional.createGreaterThan(new StorageField("expires", now)),
                Conditional.createAnd(),
                Conditional.createEqualTo("domain", domainName)
        });

        if (result.isEmpty()) {
            return noticeList;
        }

        for (StorageField[] fields : result) {
            Map<String, StorageField> map = StorageFields.get(fields);

            Notice notice = new Notice(map.get("id").getLong(), map.get("domain").getString(),
                    map.get("title").getString(), map.get("content").getString(), map.get("type").getInt(),
                    map.get("creation").getLong(), map.get("expires").getLong());
            if (!map.get("content_url").isNullValue()) {
                notice.setContentURL(map.get("content_url").getString());
            }

            noticeList.add(notice);
        }

        return noticeList;
    }
}
