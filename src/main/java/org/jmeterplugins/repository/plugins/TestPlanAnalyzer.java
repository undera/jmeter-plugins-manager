package org.jmeterplugins.repository.plugins;

import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestPlanAnalyzer {

    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final byte[] XML_HEADER = "<?xml version=\"1.1".getBytes();

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
        if (bytes != null && bytes.length > XML_HEADER.length) {
            System.arraycopy(XML_HEADER, 0, bytes, 0, XML_HEADER.length);
        }
        return bytes;
    }



    private NodeList getNodeListWithClassNames(String path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            byte[] bytes = overrideXmlVersion(readBytesFromFile(path));
            Document doc = (bytes == null) ? builder.parse(path) : builder.parse(new ByteArrayInputStream(bytes));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//*[@guiclass|@testclass]");
            return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
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
