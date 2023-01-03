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

package cube.service.auth;

import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.entity.AuthDomain;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 授权域文件形式的数据库。
 * 仅用于测试，不可用于生产环境。
 */
public class AuthDomainFile {

    private File file;

    private List<AuthDomain> authDomainList;

    public AuthDomainFile(String path) {
        this.file = new File(path);
        this.authDomainList = new ArrayList<>();

        StringBuilder buf = new StringBuilder();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(this.file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
            }
        } catch (IOException e) {
            Logger.e(this.getClass(), "Read auth domain file", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        boolean needUpdate = false;
        long now = System.currentTimeMillis();

        try {
            JSONArray array = new JSONArray(buf.toString());
            for (int i = 0, size = array.length(); i < size; ++i) {
                JSONObject item = array.getJSONObject(i);
                AuthDomain authDomain = new AuthDomain(item.getString("domain"),
                        item.getString("appKey"), item.getJSONArray("tokens"), false);
                this.authDomainList.add(authDomain);

                // 判断是否有超期记录，有则删除
                Iterator<AuthToken> iter = authDomain.tokens.values().iterator();
                while (iter.hasNext()) {
                    AuthToken at = iter.next();
                    if (at.getExpiry() < now) {
                        needUpdate = true;
                        iter.remove();
                    }
                }
            }
        } catch (JSONException e) {
            Logger.e(this.getClass(), "Read auth domain file format error", e);
        }

        if (needUpdate) {
            (new Thread() {
                @Override
                public void run() {
                    update();
                }
            }).start();
        }

        buf = null;
    }

    /**
     * 返回被管理的域清单。
     *
     * @return
     */
    public List<AuthDomain> getAuthDomainList() {
        return this.authDomainList;
    }

    public AuthDomain queryDomain(String domainName, String appKey) {
        for (AuthDomain domain : this.authDomainList) {
            if (domain.domainName.equals(domainName) && domain.appKey.equals(appKey)) {
                return domain;
            }
        }

        return null;
    }

    public AuthToken queryToken(String code) {
        for (AuthDomain domain : this.authDomainList) {
            for (AuthToken token : domain.tokens.values()) {
                if (token.getCode().equals(code)) {
                    return token;
                }
            }
        }

        return null;
    }

    public synchronized void update() {
        FileOutputStream fos = null;

        try {
            JSONArray array = new JSONArray();
            for (AuthDomain domain : this.authDomainList) {
                JSONArray tokensArray = new JSONArray();
                for (AuthToken token : domain.tokens.values()) {
                    JSONObject tokenJson = token.toJSON();
                    tokenJson.remove("domain");
                    tokenJson.remove("appKey");
                    if (tokenJson.has("description")) {
                        tokenJson.remove("description");
                    }

                    // 写入令牌信息
                    tokensArray.put(tokenJson);
                }

                JSONObject domainJson = new JSONObject();
                domainJson.put("domain", domain.domainName);
                domainJson.put("appKey", domain.appKey);
                domainJson.put("tokens", tokensArray);

                // 写入域信息
                array.put(domainJson);
            }

            fos = new FileOutputStream(this.file);
            fos.write(array.toString(4).getBytes(Charset.forName("UTF-8")));
            fos.flush();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
