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

import cell.util.Utils;
import cell.util.log.LogLevel;
import cell.util.log.Logger;
import cube.common.JSONable;
import cube.util.FileUtils;
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
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Cell 配置文件信息。
 */
public class CellConfigFile {

    private String fullPath;

    private Document document;

    private AccessPoint accessPoint;

    private AccessPoint wsAccessPoint;

    private AccessPoint wssAccessPoint;

    private SSLConfig sslConfig;

    private LogLevel logLevel;

    private List<CelletConfig> celletList;

    public CellConfigFile(String fullPath) {
        this.fullPath = fullPath;
        this.logLevel = LogLevel.INFO;
    }

    public String getFullPath() {
        return this.fullPath;
    }

    public AccessPoint getAccessPoint() {
        return this.accessPoint;
    }

    public void setAccessPoint(AccessPoint accessPoint) {
        this.accessPoint = accessPoint;

        NodeList nodeList = this.document.getElementsByTagName("server");
        Element node = (Element) nodeList.item(0);
        node.getElementsByTagName("host").item(0).setTextContent(accessPoint.getHost());
        node.getElementsByTagName("port").item(0).setTextContent(Integer.toString(accessPoint.getPort()));
        node.getElementsByTagName("max-connection").item(0).setTextContent(Integer.toString(accessPoint.maxConnection));
    }

    public AccessPoint getWSAccessPoint() {
        return this.wsAccessPoint;
    }

    public void setWSAccessPoint(AccessPoint accessPoint) {
        this.wsAccessPoint = accessPoint;

        NodeList nodeList = this.document.getElementsByTagName("ws-server");
        Element node = (Element) nodeList.item(0);
        if (node.getChildNodes().getLength() > 0) {
            node.getElementsByTagName("host").item(0).setTextContent(accessPoint.getHost());
            node.getElementsByTagName("port").item(0).setTextContent(Integer.toString(accessPoint.getPort()));
            node.getElementsByTagName("max-connection").item(0).setTextContent(Integer.toString(accessPoint.maxConnection));
        }
        else {
            Element newNode = this.document.createElement("host");
            newNode.setTextContent(accessPoint.getHost());
            node.appendChild(newNode);
            newNode = this.document.createElement("port");
            newNode.setTextContent(Integer.toString(accessPoint.getPort()));
            node.appendChild(newNode);
            newNode = this.document.createElement("max-connection");
            newNode.setTextContent(Integer.toString(accessPoint.maxConnection));
            node.appendChild(newNode);
        }
    }

    public AccessPoint getWSSAccessPoint() {
        return this.wssAccessPoint;
    }

    public void setWSSAccessPoint(AccessPoint accessPoint) {
        this.wssAccessPoint = accessPoint;

        NodeList nodeList = this.document.getElementsByTagName("wss-server");
        Element node = (Element) nodeList.item(0);
        if (node.getChildNodes().getLength() > 0) {
            node.getElementsByTagName("host").item(0).setTextContent(accessPoint.getHost());
            node.getElementsByTagName("port").item(0).setTextContent(Integer.toString(accessPoint.getPort()));
            node.getElementsByTagName("max-connection").item(0).setTextContent(Integer.toString(accessPoint.maxConnection));
        }
        else {
            Element newNode = this.document.createElement("host");
            newNode.setTextContent(accessPoint.getHost());
            node.appendChild(newNode);
            newNode = this.document.createElement("port");
            newNode.setTextContent(Integer.toString(accessPoint.getPort()));
            node.appendChild(newNode);
            newNode = this.document.createElement("max-connection");
            newNode.setTextContent(Integer.toString(accessPoint.maxConnection));
            node.appendChild(newNode);
        }
    }

    public SSLConfig getSslConfig() {
        return this.sslConfig;
    }

    public LogLevel getLogLevel() {
        return this.logLevel;
    }

    public String getLogLevelAsString() {
        switch (this.logLevel) {
            case DEBUG:
                return "DEBUG";
            case INFO:
                return "INFO";
            case WARNING:
                return "WARNING";
            case ERROR:
                return "ERROR";
            default:
                return "INFO";
        }
    }

    public void setLogLevel(String logLevelString) {
        if (logLevelString.equalsIgnoreCase("DEBUG")) {
            this.logLevel = LogLevel.DEBUG;
        }
        else if (logLevelString.equalsIgnoreCase("INFO")) {
            this.logLevel = LogLevel.INFO;
        }
        else if (logLevelString.equalsIgnoreCase("WARNING")) {
            this.logLevel = LogLevel.WARNING;
        }
        else if (logLevelString.equalsIgnoreCase("ERROR")) {
            this.logLevel = LogLevel.ERROR;
        }
        else {
            return;
        }

        NodeList nodeList = this.document.getElementsByTagName("log");
        NodeList level = ((Element) nodeList.item(0)).getElementsByTagName("level");
        level.item(0).setTextContent(logLevelString);
    }

    public List<CelletConfig> getCelletConfigList() {
        return this.celletList;
    }

    public void load() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.document = builder.parse(this.fullPath);
        } catch (ParserConfigurationException e) {
            Logger.w(this.getClass(), "#parse", e);
        } catch (SAXException e) {
            Logger.w(this.getClass(), "#parse", e);
        } catch (IOException e) {
            Logger.w(this.getClass(), "#parse", e);
        }

        if (null == this.document) {
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

        nodeList = document.getElementsByTagName("ssl");
        if (nodeList.getLength() > 0) {
            Element el = (Element) nodeList.item(0);
            NodeList keystore = el.getElementsByTagName("keystore");
            if (keystore.getLength() > 0) {
                this.sslConfig = new SSLConfig(keystore.item(0).getTextContent().trim(),
                        el.getElementsByTagName("store-password").item(0).getTextContent(),
                        el.getElementsByTagName("manager-password").item(0).getTextContent());
            }
        }

        nodeList = document.getElementsByTagName("log");
        if (nodeList.getLength() > 0) {
            NodeList level = ((Element) nodeList.item(0)).getElementsByTagName("level");
            String logLevelString = level.item(0).getTextContent();
            if (logLevelString.equalsIgnoreCase("DEBUG")) {
                this.logLevel = LogLevel.DEBUG;
            }
            else if (logLevelString.equalsIgnoreCase("INFO")) {
                this.logLevel = LogLevel.INFO;
            }
            else if (logLevelString.equalsIgnoreCase("WARNING")) {
                this.logLevel = LogLevel.WARNING;
            }
            else if (logLevelString.equalsIgnoreCase("ERROR")) {
                this.logLevel = LogLevel.ERROR;
            }
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

    public void save() {
        if (null == this.document) {
            return;
        }

        this.backup();

        FileWriter fw = null;
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            fw = new FileWriter(new File(this.fullPath));

            StreamResult sr = new StreamResult(fw);
            DOMSource source = new DOMSource(this.document);
            transformer.transform(source, sr);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fw) {
                try {
                    fw.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void backup() {
        File file = new File(this.fullPath);
        String filename = FileUtils.extractFileName(file.getName());

        String backupPath = file.getParent() + "/backup";
        File bp = new File(backupPath);
        if (!bp.exists()) {
            bp.mkdirs();
        }

        Path source = Paths.get(this.fullPath);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Path target = Paths.get(backupPath, filename + "_" + dateFormat.format(new Date()) + ".xml");

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * SSL 配置。
     */
    public class SSLConfig implements JSONable {

        public final String keystore;

        public final String storePassword;

        public final String managerPassword;

        public SSLConfig(String keystore, String storePassword, String managerPassword) {
            this.keystore = keystore;
            this.storePassword = storePassword;
            this.managerPassword = managerPassword;
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("keystore", this.keystore);
            json.put("storePassword", this.storePassword);
            json.put("managerPassword", this.managerPassword);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return toJSON();
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
