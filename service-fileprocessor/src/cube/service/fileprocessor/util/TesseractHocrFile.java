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

package cube.service.fileprocessor.util;

import cell.util.Utils;
import cube.geometry.BoundingBox;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 *
 */
public class TesseractHocrFile {

    public TesseractHocrFile(File file) {
        File newFile = this.preproccess(file);
        if (null != newFile) {
            this.readXML(newFile);
            newFile.delete();
        }
    }

    private File preproccess(File file) {
        String tmpFile = file.getParent() + "/tmp_" + Utils.randomString(4) + ".xml";

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            writer = new BufferedWriter(new FileWriter(tmpFile));

            String line = null;
            boolean skip = false;
            while ((line = reader.readLine()) != null) {
                if (skip) {
                    skip = false;
                    continue;
                }

                if (line.startsWith("<!DOCTYPE")) {
                    skip = true;
                    continue;
                }

                writer.write(line);
                writer.write("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }

            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }

        return new File(tmpFile);
    }

    private void readXML(File file) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList nodeList = doc.getElementsByTagName("body");
            Element el = (Element) nodeList.item(0);

            nodeList = el.getElementsByTagName("div");
            for (int i = 0; i < nodeList.getLength(); ++i) {
                Element nodeEl = (Element) nodeList.item(i);
                String className = nodeEl.getAttribute("class");
                if (className.equals("ocr_page")) {
                    parsePage(nodeEl);
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Page parsePage(Element el) {
        Page page = new Page();

        String title = el.getAttribute("title");
        String[] params = title.split(";");
        for (String param : params) {
            param = param.trim();
            if (param.startsWith("bbox")) {
                String[] bboxParam = param.split(" ");
                if (bboxParam.length == 5) {
                    BoundingBox bbox = new BoundingBox(Integer.parseInt(bboxParam[1]),
                            Integer.parseInt(bboxParam[2]),
                            Integer.parseInt(bboxParam[3]),
                            Integer.parseInt(bboxParam[4]));
                    page.bbox = bbox;
                }
            }
        }

        NodeList nodeList = el.getChildNodes();

        return page;
    }

    private void parseArea(Page page, Element el) {

    }

    public class Page {

        protected BoundingBox bbox;

        protected Page() {
        }

    }

    public class Area {

    }

    public static void main(String[] args) {
        File file = new File("service/storage/tmp/x.html");
        TesseractHocrFile hocrFile = new TesseractHocrFile(file);
    }
}
