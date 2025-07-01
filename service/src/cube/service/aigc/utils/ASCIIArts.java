/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.utils;

import cell.util.Utils;
import cell.util.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

public class ASCIIArts {

    private long fileTimestamp;
    private JSONObject data;

    private final static ASCIIArts instance = new ASCIIArts();

    private ASCIIArts() {
        this.load();
    }

    private void load() {
        try {
            File file = new File("assets/ASCIIArt.json");
            if (file.exists() && file.lastModified() != this.fileTimestamp) {
                this.fileTimestamp = file.lastModified();
                byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                this.data = new JSONObject(new String(data, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#load", e);
        }
    }

    public static ASCIIArt randomArt() {
        ASCIIArts.instance.load();
        if (null == ASCIIArts.instance.data) {
            return null;
        }

        int pos = Utils.randomInt(0, ASCIIArts.instance.data.keySet().size() - 1);
        Iterator<String> iter = ASCIIArts.instance.data.keySet().iterator();
        String key = null;
        int index = 0;
        while (iter.hasNext()) {
            String data = iter.next();
            if (index == pos) {
                key = data;
                break;
            }
            ++index;
        }
        JSONArray array = ASCIIArts.instance.data.getJSONArray(key);
        String art = array.getString(Utils.randomInt(0, array.length() - 1));
        return new ASCIIArt(key, art);
    }

    public static class ASCIIArt {
        public String type;
        public String data;

        public ASCIIArt(String type, String data) {
            this.type = type;
            this.data = data;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("type", this.type);
            json.put("data", this.data);
            return json;
        }
    }


    public static void main(String[] args) {
        System.out.println(ASCIIArts.randomArt().data);
    }
}
