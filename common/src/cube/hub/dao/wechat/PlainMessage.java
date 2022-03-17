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

package cube.hub.dao.wechat;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.JSONable;
import cube.common.entity.Contact;
import cube.hub.dao.Meta;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

/**
 * 平滑消息。
 */
public class PlainMessage extends Meta {

    private long id;

    private int datePrecision;
    private long date;

    private String text;

    private File file;
    private String fileMD5;

    private Contact sender;

    public PlainMessage() {
        super(Utils.generateSerialNumber());
        this.datePrecision = DatePrecision.Unknown;
        this.date = this.timestamp;
    }

    public PlainMessage(String text) {
        super(Utils.generateSerialNumber());
        this.datePrecision = DatePrecision.Unknown;
        this.date = System.currentTimeMillis();
        this.text = text;
    }

    public PlainMessage(JSONObject json) {
        super(json);
        this.datePrecision = json.getInt("precision");
        this.date = json.getLong("date");
        this.sender = new Contact(json.getJSONObject("sender"));

        if (json.has("text")) {
            this.text = json.getString("text");
        }

        if (json.has("file")) {
            this.file = new File(json.getString("fullPath"));
            this.fileMD5 = json.getString("fileMD5");
        }
    }

    public String getText() {
        return this.text;
    }

    public File getFile() {
        return this.file;
    }

    public String getFileMD5() {
        return this.fileMD5;
    }

    public void setFile(File file) {
        this.file = file;
        this.fileMD5 = md5(file);
    }

    public void setSender(Contact sender) {
        this.sender = sender;
    }

    public Contact getSender() {
        return this.sender;
    }

    public void updateDate(String dateText) {
        if (null == dateText) {
            return;
        }

        if (dateText.contains("昨") || dateText.contains("天")) {
            this.datePrecision = DatePrecision.Day;
            this.date = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        }
        else if (dateText.indexOf("时期") == 0 || dateText.indexOf("星期") == 0 || dateText.contains("期")) {
            Calendar calendar = Calendar.getInstance();
            if (dateText.contains("一")) {
                calendar.set(Calendar.DAY_OF_WEEK, 2);
            }
            else if (dateText.contains("二")) {
                calendar.set(Calendar.DAY_OF_WEEK, 3);
            }
            else if (dateText.contains("三")) {
                calendar.set(Calendar.DAY_OF_WEEK, 4);
            }
            else if (dateText.contains("四")) {
                calendar.set(Calendar.DAY_OF_WEEK, 5);
            }
            else if (dateText.contains("五")) {
                calendar.set(Calendar.DAY_OF_WEEK, 6);
            }
            else if (dateText.contains("六")) {
                calendar.set(Calendar.DAY_OF_WEEK, 7);
            }

            this.datePrecision = DatePrecision.Day;
            this.date = calendar.getTimeInMillis();
        }
        else {
            String[] segments = dateText.split("/");
            if (segments.length == 3) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, Integer.parseInt("20" + segments[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(segments[1]) - 1);
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(segments[2]));
                this.datePrecision = DatePrecision.Day;
                this.date = calendar.getTimeInMillis();
            }
            else {
                segments = dateText.split(":");
                if (segments.length == 2) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(segments[0]));
                    calendar.set(Calendar.MINUTE, Integer.parseInt(segments[1]));
                    this.datePrecision = DatePrecision.Minute;
                    this.date = calendar.getTimeInMillis();
                }
                else {
                    Logger.e(this.getClass(), "Error date format : " + dateText);
                }
            }
        }
    }

    public long getDate() {
        return this.date;
    }

    public int getDatePrecision() {
        return this.datePrecision;
    }

    public String getDateDescription() {
        if (DatePrecision.Unknown == this.datePrecision) {
            return "";
        }
        else if (DatePrecision.Minute == this.datePrecision) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(this.date);
            StringBuilder buf = new StringBuilder();
            buf.append(calendar.get(Calendar.YEAR));
            buf.append("-");
            buf.append(calendar.get(Calendar.MONTH) + 1);
            buf.append("-");
            buf.append(calendar.get(Calendar.DAY_OF_MONTH));
            buf.append(" ");
            buf.append(calendar.get(Calendar.HOUR_OF_DAY));
            buf.append(":");
            buf.append(calendar.get(Calendar.MINUTE));
            return buf.toString();
        }
        else {
            long delta = System.currentTimeMillis() - this.date;
            if (delta > 24 * 60 * 60 * 1000 && delta < 2 * 24 * 60 * 60 * 1000) {
                return "昨天";
            }
            else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(this.date);
                StringBuilder buf = new StringBuilder();
                buf.append(calendar.get(Calendar.YEAR));
                buf.append("-");
                buf.append(calendar.get(Calendar.MONTH) + 1);
                buf.append("-");
                buf.append(calendar.get(Calendar.DAY_OF_MONTH));
                return buf.toString();
            }
        }
    }

    public boolean isTextType() {
        return (null != this.text);
    }

    public boolean isImageType() {
        if (null == this.file) {
            return false;
        }

        return FileUtils.isImageType(FileUtils.extractFileExtensionType(this.file.getName()));
    }

    /**
     * 比较消息的数据内容是否相同。
     *
     * @param other
     * @return
     */
    public boolean equalsContent(PlainMessage other) {
        if (this.sender.getName().equals(other.sender.getName())) {
            if (null != this.text && null != other.text) {
                return this.text.equals(other.text);
            }

            if (null != this.file && null != other.file) {
                return this.fileMD5.equalsIgnoreCase(other.fileMD5);
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        buf.append(getDateDescription());
        buf.append("] [");
        buf.append(this.sender.getName());
        buf.append("] : ");
        if (null != this.text) {
            buf.append("(TEXT) ");
            buf.append(this.text);
        }
        else if (null != this.file) {
            if (isImageType()) {
                buf.append("(IMAGE) ");
                buf.append(this.file.getName());
            }
            else {
                buf.append("(FILE) ");
                buf.append(this.file.getName());
            }
        }
        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("precision", this.datePrecision);
        json.put("date", this.date);

        if (null != this.text) {
            json.put("text", this.text);
        }

        if (null != this.file) {
            json.put("file", this.file.getName());
            json.put("fullPath", this.file.getAbsolutePath());
            json.put("fileMD5", this.fileMD5);
        }

        json.put("sender", this.sender.toJSON());

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    

    private String md5(File file) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[4096];
            int length = 0;
            while ((length = fis.read(bytes)) > 0) {
                md5.update(bytes, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }

        byte[] hashMD5 = md5.digest();
        return FileUtils.bytesToHexString(hashMD5);
    }
}
