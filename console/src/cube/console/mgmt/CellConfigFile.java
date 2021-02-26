/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.console.mgmt;

import cell.util.log.Logger;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cell 配置文件信息。
 */
public class CellConfigFile {

    private String fullPath;

    private AccessPoint accessPoint;

    private AccessPoint wsAccessPoint;

    private AccessPoint wssAccessPoint;

    private List<CelletConfig> celletList;

    public CellConfigFile(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getFullPath() {
        return this.fullPath;
    }

    public AccessPoint getAccessPoint() {
        return this.accessPoint;
    }

    public AccessPoint getWSAccessPoint() {
        return this.wsAccessPoint;
    }

    public AccessPoint getWSSAccessPoint() {
        return this.wssAccessPoint;
    }

    public List<CelletConfig> getCelletConfigList() {
        return this.celletList;
    }

    public void refresh() {
        this.parse();
    }

    private void parse() {
        Document document = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(this.fullPath);
        } catch (ParserConfigurationException e) {
            Logger.w(this.getClass(), "#parse", e);
        } catch (SAXException e) {
            Logger.w(this.getClass(), "#parse", e);
        } catch (IOException e) {
            Logger.w(this.getClass(), "#parse", e);
        }

        if (null == document) {
            return;
        }

        NodeList nodeList = document.getElementsByTagName("server");
        Element node = (Element) nodeList.item(0);
        String host = node.getElementsByTagName("host").item(0).getTextContent();
        int port = Integer.parseInt(node.getElementsByTagName("port").item(0).getTextContent());
        int maxConn = Integer.parseInt(node.getElementsByTagName("max-connection").item(0).getTextContent());
        this.accessPoint = new AccessPoint(host, port, maxConn);

        nodeList = document.getElementsByTagName("ws-server");
        node = (Element) nodeList.item(0);
        nodeList = node.getElementsByTagName("host");
        if (null != nodeList && nodeList.getLength() > 0) {
            host = nodeList.item(0).getTextContent();
            port = Integer.parseInt(node.getElementsByTagName("port").item(0).getTextContent());
            maxConn = Integer.parseInt(node.getElementsByTagName("max-connection").item(0).getTextContent());
            this.wsAccessPoint = new AccessPoint(host, port, maxConn);
        }

        nodeList = document.getElementsByTagName("wss-server");
        node = (Element) nodeList.item(0);
        nodeList = node.getElementsByTagName("host");
        if (null != nodeList && nodeList.getLength() > 0) {
            host = nodeList.item(0).getTextContent();
            port = Integer.parseInt(node.getElementsByTagName("port").item(0).getTextContent());
            maxConn = Integer.parseInt(node.getElementsByTagName("max-connection").item(0).getTextContent());
            this.wssAccessPoint = new AccessPoint(host, port, maxConn);
        }

        nodeList = document.getElementsByTagName("cellet");
        if (nodeList.getLength() > 0) {
            this.celletList = new ArrayList<>();

            for (int i = 0; i < nodeList.getLength(); ++i) {
                Element celletNode = (Element) nodeList.item(i);

                CelletConfig cc = new CelletConfig();

                String[] ports = celletNode.getAttribute("port").split(",");
                for (String strPort : ports) {
                    cc.addPort(Integer.parseInt(strPort));
                }

                String jarFile = celletNode.getAttribute("jar");
                if (null != jarFile && jarFile.length() > 0) {
                    cc.jarFilePath = jarFile;
                }

                NodeList classList = celletNode.getElementsByTagName("class");
                if (classList.getLength() > 0) {
                    for (int n = 0; n < classList.getLength(); ++n) {
                        Node classNode = classList.item(n);
                        cc.addClass(classNode.getTextContent().trim());
                    }
                }

                this.celletList.add(cc);
            }
        }
    }

    public class CelletConfig implements JSONable {

        private List<Integer> ports = new ArrayList<>();

        private List<String> classes = new ArrayList<>();

        private String jarFilePath = null;

        private File jarFile = null;

        public CelletConfig() {
        }

        public List<Integer> getPorts() {
            return this.ports;
        }

        public List<String> getClasses() {
            return this.classes;
        }

        public String getJarFilePath() {
            return this.jarFilePath;
        }

        public void setJarFile(File file) {
            this.jarFile = file;
        }

        public void addPort(int port) {
            if (this.ports.contains(port)) {
                return;
            }

            this.ports.add(port);
        }

        public void addClass(String clazz) {
            if (this.classes.contains(clazz)) {
                return;
            }

            this.classes.add(clazz);
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();

            JSONArray portArray = new JSONArray();
            for (Integer port : this.ports) {
                portArray.put(port.intValue());
            }
            json.put("ports", portArray);

            if (null != this.jarFilePath) {
                JSONObject jarFileJson = new JSONObject();
                jarFileJson.put("path", this.jarFilePath);
                if (null != this.jarFile) {
                    jarFileJson.put("name", this.jarFile.getName());
                    jarFileJson.put("size", this.jarFile.length());
                    jarFileJson.put("lastModified", this.jarFile.lastModified());
                }

                json.put("jar", jarFileJson);
            }

            JSONArray classArray = new JSONArray();
            for (String clazz : this.classes) {
                classArray.put(clazz);
            }
            json.put("classes", classArray);

            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
