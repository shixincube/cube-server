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

    public static void main(String[] args) {
        File file = new File("service/storage/tmp/x.html");
        TesseractHocrFile hocrFile = new TesseractHocrFile(file);
    }
}
