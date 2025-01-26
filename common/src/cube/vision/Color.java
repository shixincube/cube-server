/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.vision;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 颜色描述。
 */
public class Color {

    private int red;

    private int green;

    private int blue;

    private int color = -1;

    public Color() {
    }

    public Color(int red, int green, int blue) {
        this.red = Math.min(red, 255);
        this.green = Math.min(green, 255);
        this.blue = Math.min(blue, 255);
    }

    public Color(String desc) {
        if (desc.startsWith("#")) {
            this.red = Integer.parseInt(desc.substring(1, 3), 16);
            this.green = Integer.parseInt(desc.substring(3, 5), 16);
            this.blue = Integer.parseInt(desc.substring(5, 7), 16);
        }
        else if (desc.startsWith("rgb")) {
            Pattern pattern = Pattern.compile("\\(([^}])*\\)");
            Matcher m = pattern.matcher(desc);
            if (m.find()) {
                String str = m.group(0);
                str = str.replaceAll("\\(", "");
                str = str.replaceAll("\\)", "");
                str = str.trim();
                String[] values = str.split(",");
                try {
                    this.red = Integer.parseInt(values[0].trim());
                    this.green = Integer.parseInt(values[1].trim());
                    this.blue = Integer.parseInt(values[2].trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else if (desc.startsWith("rgba")) {
            Pattern pattern = Pattern.compile("\\(([^}])*\\)");
            Matcher m = pattern.matcher(desc);
            if (m.find()) {
                String str = m.group(0);
                str = str.replaceAll("\\(", "");
                str = str.replaceAll("\\)", "");
                str = str.trim();
                String[] values = str.split(",");
                try {
                    this.red = Integer.parseInt(values[0].trim());
                    this.green = Integer.parseInt(values[1].trim());
                    this.blue = Integer.parseInt(values[2].trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Color(JSONObject json) {
        this.red = json.getInt("r");
        this.green = json.getInt("g");
        this.blue = json.getInt("b");
    }

    public int red() {
        return this.red;
    }

    public int green() {
        return this.green;
    }

    public int blue() {
        return this.blue;
    }

    public int color() {
        if (this.color == -1) {
            int r = (this.red << 16) & 0x00FF0000;
            int g = (this.green << 8) & 0x0000FF00;
            int b = this.blue & 0x000000FF;
            this.color = (r | g | b);
        }

        return this.color;
    }

    public String formatRGB() {
        StringBuilder buf = new StringBuilder("rgb(");
        buf.append(this.red);
        buf.append(",");
        buf.append(this.green);
        buf.append(",");
        buf.append(this.blue);
        buf.append(")");
        return buf.toString();
    }

    public String formatHex() {
        StringBuilder buf = new StringBuilder("#");
        String redHex = Integer.toHexString(this.red);
        buf.append(redHex.length() == 1 ? "0" + redHex : redHex);
        String greenHex = Integer.toHexString(this.green);
        buf.append(greenHex.length() == 1 ? "0" + greenHex : greenHex);
        String blueHex = Integer.toHexString(this.blue);
        buf.append(blueHex.length() == 1 ? "0" + blueHex : blueHex);
        return buf.toString();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("r", this.red);
        json.put("g", this.green);
        json.put("b", this.blue);
        return json;
    }
}
