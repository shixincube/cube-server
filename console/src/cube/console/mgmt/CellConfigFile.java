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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
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
        if (null != nodeList) {
            host = nodeList.item(0).getTextContent();
            port = Integer.parseInt(node.getElementsByTagName("port").item(0).getTextContent());
            maxConn = Integer.parseInt(node.getElementsByTagName("max-connection").item(0).getTextContent());
            this.wsAccessPoint = new AccessPoint(host, port, maxConn);
        }

        nodeList = document.getElementsByTagName("wss-server");
        node = (Element) nodeList.item(0);
        nodeList = node.getElementsByTagName("host");
        if (null != nodeList) {
            host = nodeList.item(0).getTextContent();
            port = Integer.parseInt(node.getElementsByTagName("port").item(0).getTextContent());
            maxConn = Integer.parseInt(node.getElementsByTagName("max-connection").item(0).getTextContent());
            this.wssAccessPoint = new AccessPoint(host, port, maxConn);
        }


    }

    public class CelletConfig {

    }
}
