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

package cube.file;

import cube.common.JSONable;
import cube.geometry.BoundingBox;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public List<Page> getPages() {
        return this.pages;
    }

    public List<String> toText() {
        List<String> text = new ArrayList<>();
        for (Page page : this.pages) {
            for (Area area : page.areas) {
                for (Part part : area.parts) {
                    text.add(part.toText());
                }
            }
        }
        return text;
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
                buf.append(part.toText());
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

        public String toText() {
            StringBuilder buf = new StringBuilder();
            for (Line line : this.lines) {
                buf.append(line.toText());
            }
            return buf.toString();
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

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("bbox", this.bbox.toJSON());
            json.put("word", this.word);
            return json;
        }
    }
}
