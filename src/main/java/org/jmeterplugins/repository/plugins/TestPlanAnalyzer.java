package org.jmeterplugins.repository.plugins;

import org.apache.jmeter.save.SaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestPlanAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(TestPlanAnalyzer.class);
    private static final byte[] XML_VERSION = "version=\"1.1\"".getBytes();

    /**
     * @param path fo jmx file with test plan
     * @return set of classes, that does not exists or empty list if there are none
     */
    public Set<String> analyze(String path) {
        log.debug("Analyze test plan: " + path);
        final NodeList nodeList = getNodeListWithClassNames(path);

        if (nodeList != null) {
            final Set<String> nonExistentClasses = new HashSet<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap attributes = node.getAttributes();
                checkAttributeAndAdd(attributes, "guiclass", nonExistentClasses);
                checkAttributeAndAdd(attributes, "testclass", nonExistentClasses);

                if (node.getNodeName().equals("BackendListener")) {
                    addBackendListenerImplClass(node, nonExistentClasses);
                }
            }
            return nonExistentClasses;
        }
        return Collections.emptySet();
    }

    private void checkAttributeAndAdd(NamedNodeMap attributes, final String attributeName, final Set<String> nonExistentClasses) {
        Node node = attributes.getNamedItem(attributeName);
        if (node != null && !isClassExists(node.getTextContent())) {
            nonExistentClasses.add(node.getTextContent());
        }
    }

    // BackendListener has implementation class stored in a stringProp with name="classname"
    private void addBackendListenerImplClass(Node node, final Set<String> nonExistentClasses) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("stringProp")) {
               Node name = child.getAttributes().getNamedItem("name");
               if (name != null && name.getTextContent().equals("classname") && !isClassExists(child.getTextContent())) {
                   nonExistentClasses.add(child.getTextContent());
                }
            }
        }
    }

    private static byte[] readBytesFromFile(String filePath) {
        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);
        } catch (IOException e) {
            log.warn("Failed read jmx file", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.warn("Failed close jmx file input stream", e);
                }
            }
        }

        return bytesArray;
    }

    private byte[] overrideXmlVersion(byte[] bytes) {
        final int headerLength = 100;
        if (bytes != null && bytes.length > headerLength) {
            final byte[] line = new byte[headerLength];
            System.arraycopy(bytes, 0, line, 0, headerLength);
            String header = new String(line);
            int index = header.indexOf("version=");
            if (index != -1) {
                System.arraycopy(XML_VERSION, 0, bytes, index, XML_VERSION.length);
            } else {
                log.debug("Did not find XML version in test plan");
            }
        }
        return bytes;
    }


    private NodeList getNodeListWithClassNames(String path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            byte[] bytes = overrideXmlVersion(readBytesFromFile(path));
            Document doc = (bytes == null) ? builder.parse(path) : builder.parse(new ByteArrayInputStream(bytes));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//*[@guiclass|@testclass]");
            return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        } catch (ParserConfigurationException pex) {
            log.warn("Cannot set the required parser config", pex);
            return null;
        } catch (Exception ex) {
            log.warn("Cannot parse file: " + path, ex);
            return null;
        }
    }

    public static boolean isClassExists(String className) {
        try {
            Class.forName(SaveService.aliasToClass(className));
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }
}
