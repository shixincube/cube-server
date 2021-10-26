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

package cube.service;

import cell.core.talk.LiteralBase;
import cell.util.Base64;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.core.*;
import cube.storage.MySQLStorage;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 秘钥机器。
 */
public final class CipherMachine {

    private final static String CacheKeyPrefix = "CubeGlobalCipher_";

    private final static CipherMachine instance = new CipherMachine();

    private final StorageField[] fields = new StorageField[] {
            new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                    Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
            }),
            new StorageField("cipher", LiteralBase.STRING, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("year", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("month", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            }),
            new StorageField("date", LiteralBase.INT, new Constraint[] {
                    Constraint.NOT_NULL
            })
    };

    private final String table = "cube_cipher";

    private Cache generalCache;

    private Storage storage;

    private byte[] currentCipher;

    private long cipherBeginningTime;

    private long cipherEndingTime;

    private Timer timer;

    private CipherMachine() {
    }

    public static CipherMachine getInstance() {
        return CipherMachine.instance;
    }

    public void start(Cache generalCache) {
        this.generalCache = generalCache;

        String[] files = new String[] {
                "config/cipher_dev.properties",
                "config/cipher.properties",
                "cipher.properties"
        };

        Properties properties = null;

        for (String configFile : files) {
            try {
                properties = ConfigUtils.readProperties(configFile);
            } catch (IOException e) {
                // Nothing
            }

            if (null != properties) {
                break;
            }
        }

        if (null != properties) {
            JSONObject config = new JSONObject();
            config.put(MySQLStorage.CONFIG_HOST, properties.getProperty("storage.host"));
            config.put(MySQLStorage.CONFIG_PORT, Integer.parseInt(properties.getProperty("storage.port")));
            config.put(MySQLStorage.CONFIG_SCHEMA, properties.getProperty("storage.schema"));
            config.put(MySQLStorage.CONFIG_USER, properties.getProperty("storage.user"));
            config.put(MySQLStorage.CONFIG_PASSWORD, properties.getProperty("storage.password"));

            this.storage = StorageFactory.getInstance().createStorage(StorageType.MySQL, "CipherMachine", config);
            this.storage.open();

            this.execSelfChecking();

            // 启动定时器
            this.startTimer();
        }
    }

    public void stop() {
        if (null != this.storage) {
            this.storage.close();
            this.storage = null;
        }

        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        this.currentCipher = null;
    }

    public boolean isReady() {
        return (null != this.currentCipher);
    }

    /**
     * 获取当前有效的密钥。
     *
     * @return
     */
    public byte[] getCurrentCipher() {
        return this.currentCipher;
    }

    /**
     * 获取指定时间有效的密钥。
     *
     * @param timestamp
     * @return
     */
    public byte[] getCipher(long timestamp) {
        if (timestamp >= this.cipherBeginningTime && timestamp <= this.cipherEndingTime) {
            return this.currentCipher;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        // 尝试从缓存里获取
        String cacheKey = CacheKeyPrefix
                + calendar.get(Calendar.YEAR) + "_"
                + (calendar.get(Calendar.MONTH) + 1) + "_"
                + calendar.get(Calendar.DATE);
        CacheValue value = this.generalCache.get(new CacheKey(cacheKey));
        if (null != value) {
            byte[] cipher = null;
            try {
                cipher = Base64.decode(value.get().getString("cipher"));
            } catch (IOException e) {
                // Nothing
            }
            return cipher;
        }

        StringBuilder sql = new StringBuilder("SELECT `cipher` FROM ");
        sql.append(this.table);
        sql.append(" WHERE `year`=").append(calendar.get(Calendar.YEAR));
        sql.append(" AND `month`=").append(calendar.get(Calendar.MONTH) + 1);
        sql.append(" AND `date`=").append(calendar.get(Calendar.DATE));

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return null;
        }

        String cipherBase64 = result.get(0)[0].getString();

        byte[] cipher = null;
        try {
            cipher = Base64.decode(cipherBase64);
        } catch (IOException e) {
            // Nothing
        }

        // 写入缓存
        JSONObject json = new JSONObject();
        json.put("cipher", cipherBase64);
        this.generalCache.put(new CacheKey(cacheKey), new CacheValue(json));

        return cipher;
    }

    public byte[] encrypt(byte[] plaintext) {
        byte[] ciphertext = null;

        SecretKeySpec skeySpec = new SecretKeySpec(this.currentCipher, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            ciphertext = cipher.doFinal(plaintext);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return ciphertext;
    }

    public String encryptOutputBase64(String plaintext) {
        if (null == this.currentCipher) {
            return plaintext;
        }

        byte[] ciphertext = this.encrypt(plaintext.getBytes(Charset.forName("UTf-8")));
        if (null == ciphertext) {
            return null;
        }

        return Base64.encodeBytes(ciphertext);
    }

    public byte[] decrypt(byte[] ciphertext) {
        byte[] plaintext = null;

        SecretKeySpec skeySpec = new SecretKeySpec(this.currentCipher, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            plaintext = cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return plaintext;
    }

    public String decryptForBase64(String base64) {
        if (null == this.currentCipher) {
            return base64;
        }

        String output = null;

        try {
            byte[] ciphertext = Base64.decode(base64);
            byte[] plaintext = decrypt(ciphertext);
            output = new String(plaintext, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return output;
    }

    private byte[] loadCipher() {
        Calendar calendar = Calendar.getInstance();

        StringBuilder sql = new StringBuilder("SELECT `cipher` FROM ");
        sql.append(this.table);
        sql.append(" WHERE `year`=").append(calendar.get(Calendar.YEAR));
        sql.append(" AND `month`=").append(calendar.get(Calendar.MONTH) + 1);
        sql.append(" AND `date`=").append(calendar.get(Calendar.DATE));

        List<StorageField[]> result = this.storage.executeQuery(sql.toString());
        if (result.isEmpty()) {
            return null;
        }

        String keyString = result.get(0)[0].getString();
        try {
            return Base64.decode(keyString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] saveRandomCipher() {
        byte[] cipher = new byte[16];
        for (int i = 0; i < 16; ++i) {
            cipher[i] = (byte) Utils.randomInt(0, 127);
        }

        String string = Base64.encodeBytes(cipher);

        Calendar calendar = Calendar.getInstance();

        boolean result = this.storage.executeInsert(this.table, new StorageField[] {
                new StorageField("cipher", LiteralBase.STRING, string),
                new StorageField("year", LiteralBase.INT, calendar.get(Calendar.YEAR)),
                new StorageField("month", LiteralBase.INT, calendar.get(Calendar.MONTH) + 1),
                new StorageField("date", LiteralBase.INT, calendar.get(Calendar.DATE))
        });

        if (result) {
            return cipher;
        }
        else {
            return null;
        }
    }

    private void execSelfChecking() {
        if (!this.storage.exist(this.table)) {
            boolean ret = this.storage.executeCreate(this.table, this.fields);
            if (!ret) {
                // 未能成功连接数据库
                Logger.w(this.getClass(), "Can NOT connect cipher database");
                return;
            }
        }

        final Calendar calendar = Calendar.getInstance();
        String cacheKey = CacheKeyPrefix
                + calendar.get(Calendar.YEAR) + "_"
                + (calendar.get(Calendar.MONTH) + 1) + "_"
                + calendar.get(Calendar.DATE);

        // 使用缓存的全局事务来设置全局缓存
        this.generalCache.execute(new CacheKey(cacheKey), new CacheTransaction() {
            @Override
            public void perform(TransactionContext context) {
                CacheValue value = context.get();
                if (null == value) {
                    // 没有值，写入值
                    currentCipher = loadCipher();
                    if (null == currentCipher) {
                        currentCipher = saveRandomCipher();
                    }

                    JSONObject json = new JSONObject();
                    json.put("cipher", Base64.encodeBytes(currentCipher));

                    context.put(new CacheValue(json));
                }
                else {
                    JSONObject json = value.get();
                    try {
                        currentCipher = Base64.decode(json.getString("cipher"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        this.cipherBeginningTime = calendar.getTimeInMillis();
        this.cipherEndingTime = this.cipherBeginningTime + 24 * 60 * 60 * 1000;
    }

    private void startTimer() {
        // 每天凌晨 0 点执行，随机时长 5 分钟
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, Utils.randomInt(0, 4));
        cal.set(Calendar.SECOND, Utils.randomInt(0, 59));

        Date date = cal.getTime();
        if (date.before(new Date())) {
            // 执行时间已过，调整至一天后
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();
        }

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                execSelfChecking();
            }
        }, date, 24 * 60 * 60 * 1000);
    }

    private void removePreviousCache(Calendar calendar) {
        // 上一天
        calendar.add(Calendar.DAY_OF_YEAR, -1);

        String cacheKey = CacheKeyPrefix
                + calendar.get(Calendar.YEAR) + "_"
                + (calendar.get(Calendar.MONTH) + 1) + "_"
                + calendar.get(Calendar.DATE);

        this.generalCache.remove(new CacheKey(cacheKey));
    }

    private void test() {
        (new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println(this.getClass().getName() + "# test start");

                String string = "This is a test string. China No.1";

                String base64 = encryptOutputBase64(string);

                System.out.println(this.getClass().getName() + "# base64: " + base64);

                String raw = decryptForBase64(base64);

                System.out.println(this.getClass().getName() + "# raw: " + raw);

                System.out.println(this.getClass().getName() + "# test stop");
            }
        }).start();
    }
}
