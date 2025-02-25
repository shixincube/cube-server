/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.JSONable;
import cube.vision.BoundingBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 结构文件。
 */
public class OCRFile implements JSONable {

    protected List<Page> pages = new ArrayList<>();

    public OCRFile() {
    }

    public OCRFile(List<String> text) {
        Part part = new Part();
        for (String lineText : text) {
            Line line = new Line();

            String[] words = lineText.split(" ");
            for (String w : words) {
                line.words.add(new Word(w));
            }

            part.lines.add(line);
        }

        Area area = new Area();
        area.parts.add(part);

        Page page = new Page();
        page.areas.add(area);

        this.pages.add(page);
    }

    public OCRFile(JSONObject json) {
        JSONArray pageArray = json.getJSONArray("pages");
        for (int i = 0; i < pageArray.length(); ++i) {
            this.pages.add(new Page(pageArray.getJSONObject(i)));
        }
    }

    public OCRFile(File file) {
        FlexibleByteBuffer buf = new FlexibleByteBuffer(4096);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            int length = 0;
            byte[] bytes = new byte[512];
            while ((length = fis.read(bytes)) > 0) {
                buf.put(bytes, 0, length);
            }
            buf.flip();

            JSONObject json = new JSONObject(new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8));
            JSONArray pageArray = json.getJSONArray("pages");
            for (int i = 0; i < pageArray.length(); ++i) {
                this.pages.add(new Page(pageArray.getJSONObject(i)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#OCRFile", e);
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public List<Page> getPages() {
        return this.pages;
    }

    public List<String> toText() {
        List<String> text = new ArrayList<>();
        for (Page page : this.pages) {
            for (Area area : page.areas) {
                for (Part part : area.parts) {
                    for (String line : part.toText()) {
                        text.add(line);
                    }
                }
            }
        }
        return text;
    }

    public void outputFile(FileOutputStream stream) throws IOException {
        JSONObject json = this.toJSON();
        String jsonString = json.toString(4);
        try {
            stream.write(jsonString.getBytes(Charset.forName("UTF-8")));
            stream.flush();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        return this.toCompactJSON();
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();

        JSONArray pageArray = new JSONArray();
        for (Page page : this.pages) {
            pageArray.put(page.toJSON());
        }
        json.put("pages", pageArray);

        return json;
    }


    /**
     * 页。
     */
    public class Page {
        protected BoundingBox bbox = new BoundingBox();

        protected List<Area> areas = new ArrayList<>();

        protected Page() {
        }

        protected Page(JSONObject json) {
            this.bbox = new BoundingBox(json.getJSONObject("bbox"));
            JSONArray array = json.getJSONArray("areas");
            for (int i = 0; i < array.length(); ++i) {
                this.areas.add(new Area(array.getJSONObject(i)));
            }
        }

        public BoundingBox getBoundingBox() {
            return this.bbox;
        }

        public List<Area> getAreas() {
            return this.areas;
        }

        public String toText() {
            StringBuilder buf = new StringBuilder();
            for (Area area : this.areas) {
                buf.append(area.toText());
            }
            return buf.toString();
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bbox", this.bbox.toJSON());

            JSONArray array = new JSONArray();
            for (Area area : this.areas) {
                array.put(area.toJSON());
            }
            json.put("areas", array);

            return json;
        }
    }

    /**
     * 识别区域。
     */
    public class Area {
        protected BoundingBox bbox = new BoundingBox();

        protected List<Part> parts = new ArrayList<>();

        protected Area() {
        }

        protected Area(JSONObject json) {
            this.bbox = new BoundingBox(json.getJSONObject("bbox"));
            JSONArray array = json.getJSONArray("parts");
            for (int i = 0; i < array.length(); ++i) {
                this.parts.add(new Part(array.getJSONObject(i)));
            }
        }

        public BoundingBox getBoundingBox() {
            return this.bbox;
        }

        public List<Part> getParts() {
            return this.parts;
        }

        public String toText() {
            StringBuilder buf = new StringBuilder();
            for (Part part : this.parts) {
                for (String line : part.toText()) {
                    buf.append(line);
                    buf.append("\n");
                }
            }
            return buf.toString();
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bbox", this.bbox.toJSON());

            JSONArray array = new JSONArray();
            for (Part part : this.parts) {
                array.put(part.toJSON());
            }
            json.put("parts", array);

            return json;
        }
    }

    /**
     * 区域部分。
     */
    public class Part {
        protected BoundingBox bbox = new BoundingBox();

        protected String language;

        protected List<Line> lines = new ArrayList<>();

        protected Part() {
        }

        protected Part(JSONObject json) {
            this.bbox = new BoundingBox(json.getJSONObject("bbox"));
            this.language = json.getString("language");
            JSONArray array = json.getJSONArray("lines");
            for (int i = 0; i < array.length(); ++i) {
                this.lines.add(new Line(array.getJSONObject(i)));
            }
        }

        public BoundingBox getBoundingBox() {
            return this.bbox;
        }

        public String getLanguage() {
            return this.language;
        }

        public List<Line> getLines() {
            return this.lines;
        }

        public List<String> toText() {
            List<String> result = new ArrayList<>(this.lines.size());
            for (Line line : this.lines) {
                result.add(line.toText());
            }
            return result;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bbox", this.bbox.toJSON());
            json.put("language", this.language);

            JSONArray array = new JSONArray();
            for (Line line : this.lines) {
                array.put(line.toJSON());
            }
            json.put("lines", array);

            return json;
        }
    }

    public class Line {
        protected BoundingBox bbox = new BoundingBox();

        protected List<Word> words = new ArrayList<>();

        protected Line() {
        }

        protected Line(JSONObject json) {
            this.bbox = new BoundingBox(json.getJSONObject("bbox"));
            JSONArray array = json.getJSONArray("words");
            for (int i = 0; i < array.length(); ++i) {
                this.words.add(new Word(array.getJSONObject(i)));
            }
        }

        public BoundingBox getBoundingBox() {
            return this.bbox;
        }

        public List<Word> getWords() {
            return this.words;
        }

        public String toText() {
            StringBuilder buf = new StringBuilder();
            for (Word word : this.words) {
                buf.append(word.word);
            }
            return buf.toString();
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bbox", this.bbox.toJSON());

            JSONArray array = new JSONArray();
            for (Word word : this.words) {
                array.put(word.toJSON());
            }
            json.put("words", array);

            return json;
        }

        public Line createLine(List<Word> wordList) {
            Line line = new Line();
            int x = Integer.MAX_VALUE;
            int y = Integer.MAX_VALUE;
            int boundaryX = 0;
            int boundaryY = 0;
            for (Word word : wordList) {
                line.words.add(word);

                BoundingBox bbox = word.bbox;
                if (bbox.x < x) {
                    x = bbox.x;
                }
                if (bbox.y < y) {
                    y = bbox.y;
                }
                if (bbox.x + bbox.width > boundaryX) {
                    boundaryX = bbox.x + bbox.width;
                }
                if (bbox.y + bbox.height > boundaryY) {
                    boundaryY = bbox.y + bbox.height;
                }
            }

            line.bbox = new BoundingBox(x, y, boundaryX - x, boundaryY - y);
            return line;
        }

        public Line createLine(Word word) {
            Line line = new Line();
            line.words.add(word);
            line.bbox = word.bbox;
            return line;
        }
    }

    public class Word {
        protected BoundingBox bbox = new BoundingBox();

        protected String word;

        protected Word(String word) {
            this.word = word;
        }

        protected Word(JSONObject json) {
            this.bbox = new BoundingBox(json.getJSONObject("bbox"));
            this.word = json.getString("word");
        }

        public BoundingBox getBoundingBox() {
            return this.bbox;
        }

        public String getWord() {
            return this.word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bbox", this.bbox.toJSON());
            json.put("word", this.word);
            return json;
        }
    }
}
