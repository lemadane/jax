package com.lemadane.jax;

/** Test comparison shared by API and CLI round trips. */
public final class XmlRoundTripSnapshot {

  private XmlRoundTripSnapshot() {
  }

  // Compare every qualified element name, attribute and leaf value. Ignore formatting
  // and sibling order, which POJO property iteration does not promise to preserve.
  public static java.util.Map<String, String> xmlValues(String xml) throws Exception {
    var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var document = factory.newDocumentBuilder().parse(
        new org.xml.sax.InputSource(new java.io.StringReader(xml)));
    var values = new java.util.TreeMap<String, String>();
    collectXmlValues(document.getDocumentElement(), "", values);
    return values;
  }

  private static void collectXmlValues(org.w3c.dom.Element element, String path, java.util.Map<String, String> values) {
    path += "/" + element.getTagName();
    values.put(path, "element");
    for (int i = 0; i < element.getAttributes().getLength(); i++) {
      var attribute = element.getAttributes().item(i);
      values.put(path + "/@" + attribute.getNodeName(), attribute.getNodeValue());
    }
    var counts = new java.util.HashMap<String, Integer>();
    boolean hasChildren = false;
    for (int i = 0; i < element.getChildNodes().getLength(); i++) {
      if (element.getChildNodes().item(i) instanceof org.w3c.dom.Element child) {
        hasChildren = true;
        int index = counts.merge(child.getTagName(), 1, Integer::sum);
        collectXmlValues(child, path + "[" + child.getTagName() + ":" + index + "]", values);
      }
    }
    if (!hasChildren) {
      values.put(path + "/text()", element.getTextContent());
    }
  }

}
