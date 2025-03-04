/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file;

import cell.util.Utils;
import cube.vision.BoundingBox;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 *
 */
public class TesseractHocrFile extends OCRFile {

    private String filename;

    public TesseractHocrFile(File file) {
        super();

        this.filename = file.getName();

        File newFile = this.preproccess(file);
        if (null != newFile) {
            this.readXML(newFile);
            newFile.delete();
        }
    }

    public TesseractHocrFile(JSONObject json) {
        super(json);

        if (json.has("filename")) {
            this.filename = json.getString("filename");
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("filename", this.filename);
        return json;
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
                    Page page = parsePage(nodeEl);
                    if (null != page) {
                        this.pages.add(page);
                    }
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
                break;
            }
        }

        NodeList nodeList = el.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                Element child = (Element) node;
                String className = child.getAttribute("class");
                if (className.equals("ocr_carea")) {
                    this.parseArea(page, child);
                }
            }
        }

        return page;
    }

    private Area parseArea(Page page, Element el) {
        Area area = new Area();

        String title = el.getAttribute("title");
        String[] params = title.trim().split(" ");
        BoundingBox bbox = new BoundingBox(Integer.parseInt(params[1]),
                Integer.parseInt(params[2]),
                Integer.parseInt(params[3]),
                Integer.parseInt(params[4]));
        area.bbox = bbox;
        page.areas.add(area);

        NodeList nodeList = el.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                Element child = (Element) node;
                String className = child.getAttribute("class");
                if (className.equals("ocr_par")) {
                    this.parsePart(area, child);
                }
            }
        }

        return area;
    }

    private Part parsePart(Area area, Element el) {
        Part part = new Part();

        part.language = el.getAttribute("lang");

        String title = el.getAttribute("title");
        String[] params = title.trim().split(" ");
        BoundingBox bbox = new BoundingBox(Integer.parseInt(params[1]),
                Integer.parseInt(params[2]),
                Integer.parseInt(params[3]),
                Integer.parseInt(params[4]));
        part.bbox = bbox;
        area.parts.add(part);

        NodeList nodeList = el.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                Element child = (Element) node;
                String className = child.getAttribute("class");
                if (className.equals("ocr_line")) {
                    this.parseLine(part, child);
                }
            }
        }

        return part;
    }

    private Line parseLine(Part part, Element el) {
        Line line = new Line();

        String title = el.getAttribute("title");
        String[] params = title.trim().split(";");
        for (String param : params) {
            param = param.trim();
            if (param.startsWith("bbox")) {
                String[] bbParams = param.split(" ");
                if (bbParams.length == 5) {
                    BoundingBox bbox = new BoundingBox(Integer.parseInt(bbParams[1]),
                            Integer.parseInt(bbParams[2]),
                            Integer.parseInt(bbParams[3]),
                            Integer.parseInt(bbParams[4]));
                    line.bbox = bbox;
                }

                break;
            }
        }

        part.lines.add(line);

        NodeList nodeList = el.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                Element child = (Element) node;
                String className = child.getAttribute("class");
                if (className.equals("ocrx_word")) {
                    parseWord(line, child);
                }
            }
        }

        return line;
    }

    private Word parseWord(Line line, Element el) {
        Word word = new Word(el.getTextContent());

        String title = el.getAttribute("title");
        String[] params = title.split(";");
        for (String param : params) {
            param = param.trim();
            if (param.startsWith("bbox")) {
                String[] bbParams = param.split(" ");
                BoundingBox bbox = new BoundingBox(Integer.parseInt(bbParams[1]),
                        Integer.parseInt(bbParams[2]),
                        Integer.parseInt(bbParams[3]),
                        Integer.parseInt(bbParams[4]));
                word.bbox = bbox;

                break;
            }
        }

        line.words.add(word);

        return word;
    }
}
