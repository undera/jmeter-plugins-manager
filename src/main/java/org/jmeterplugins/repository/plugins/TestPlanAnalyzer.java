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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestPlanAnalyzer {

    private static final Logger log = LoggingManager.getLoggerForClass();


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

    private NodeList getNodeListWithClassNames(String path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path);
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
