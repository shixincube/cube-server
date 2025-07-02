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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ASCIIArts {

    private long fileTimestamp;
    private List<ASCIIArt> asciiArtList = new ArrayList<>();

    private final static ASCIIArts instance = new ASCIIArts();

    private ASCIIArts() {
        this.load();
    }

    private void load() {
        try {
            File file = new File("assets/ASCIIArt.json");
            if (file.exists() && file.lastModified() != this.fileTimestamp) {
                this.asciiArtList.clear();
                this.fileTimestamp = file.lastModified();
                byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                JSONObject json = new JSONObject(new String(data, StandardCharsets.UTF_8));

                Iterator<String> iter = json.keys();
                while (iter.hasNext()) {
                    String type = iter.next();
                    JSONArray array = json.getJSONArray(type);
                    for (int i = 0; i < array.length(); ++i) {
                        String artData = array.getString(i);
                        ASCIIArt art = new ASCIIArt(type, artData);
                        this.asciiArtList.add(art);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#load", e);
        }
    }

    public static ASCIIArt randomArt() {
        ASCIIArts.instance.load();
        if (ASCIIArts.instance.asciiArtList.isEmpty()) {
            return null;
        }

        return ASCIIArts.instance.asciiArtList.get(
                Utils.randomInt(0, ASCIIArts.instance.asciiArtList.size() - 1));
    }

    public static List<ASCIIArt> listArts() {
        ASCIIArts.instance.load();
        return new ArrayList<>(ASCIIArts.instance.asciiArtList);
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
        for (int i = 0; i < 5; ++i) {
            System.out.println(ASCIIArts.randomArt().data);
            System.out.println("----------------------------------------");
        }

//        List<ASCIIArt> list = ASCIIArts.listArts();
//        int index = 0;
//        for (ASCIIArt art : list) {
//            System.out.println(index);
//            System.out.println(art.data);
//            System.out.println("----------------------------------------");
//            ++index;
//        }
    }
}
