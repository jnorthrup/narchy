package spacegraph.geo;

import jcog.list.FasterList;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import spacegraph.geo.data.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class OsmReader {

    final Osm osm;

    private String filename;

    OsmReader(String filename) {
        setFilename(filename);
        osm = new Osm();
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    Osm parse() throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder documentBuilder = factory.newDocumentBuilder();


        InputStream fis = new FileInputStream(filename);
        if (filename.endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        }
        Document document = documentBuilder.parse(fis);


        osm.clear();
        Node root = document.getDocumentElement();

        Collection<Element> relationElements = new FasterList<>();

        NodeList childNodes = root.getChildNodes();
        for (int i=0; i<childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);

            switch (childNode.getNodeName()) {
                case "bounds": {
                    osm.bounds = new OsmBounds((Element) childNode);
                    break;
                }
                case "node": {
                    Element childElement = (Element) childNode;
                    String id = childElement.getAttribute("id");

                    HashMap<String, String> osmTags = new HashMap<>();
                    NodeList nodeChildren = childNode.getChildNodes();
                    int nnc = nodeChildren.getLength();
                    for (int j = 0; j < nnc; j++) {
                        Node nodeChild = nodeChildren.item(j);
                        if ("tag".equals(nodeChild.getNodeName())) {
                            Element nodeChildElement = (Element) nodeChild;
                            String k = nodeChildElement.getAttribute("k");
                            String v = nodeChildElement.getAttribute("v");
                            osmTags.put(k, v);
                        }
                    }
                    osm.nodes.add(new OsmNode(id, new GeoCoordinate(childElement), osmTags));

                    break;
                }
                case "way": {
                    Element childElement = (Element) childNode;
                    String id = childElement.getAttribute("id");

                    List<OsmNode> refOsmNodes = new FasterList<>();
                    HashMap<String, String> osmTags = new HashMap<>();

                    NodeList wayChildren = childNode.getChildNodes();
                    for (int j = 0; j < wayChildren.getLength(); j++) {
                        Node wayChild = wayChildren.item(j);
                        String node = wayChild.getNodeName();
                        if ("nd".equals(node)) {
                            Element wayChildElement = (Element) wayChild;
                            String refId = wayChildElement.getAttribute("ref");
                            OsmNode refOsmNode = OsmNode.getOsmNodeById(osm.nodes, refId);
                            refOsmNodes.add(refOsmNode);
                        } else if ("tag".equals(node)) {
                            Element nodeChildElement = (Element) wayChild;
                            String k = nodeChildElement.getAttribute("k");
                            String v = nodeChildElement.getAttribute("v");
                            osmTags.put(k, v);
                        }
                    }

                    osm.ways.add(new OsmWay(id, refOsmNodes, osmTags));

                    break;
                }
                case "relation":
                    Element childElement = (Element) childNode;
                    String id = childElement.getAttribute("id");

                    NodeList relationChildren = childElement.getChildNodes();
                    HashMap<String, String> osmTags = new HashMap<>(relationChildren.getLength());
                    for (int j = 0; j < relationChildren.getLength(); j++) {
                        Node relationChild = relationChildren.item(j);
                        if ("tag".equals(relationChild.getNodeName())) {
                            Element nodeChildElement = (Element) relationChild;
                            String k = nodeChildElement.getAttribute("k");
                            String v = nodeChildElement.getAttribute("v");
                            osmTags.put(k, v);
                        }
                    }

                    osm.relations.add(new OsmRelation(id, null, osmTags));
                    relationElements.add(childElement);
                    break;
            }
        }

        // Relation 2nd pass
        for (Element relationElement: relationElements) {
            String id = relationElement.getAttribute("id");

            OsmRelation osmRelation = OsmRelation.getOsmRelationById(osm.relations, id);
            List<OsmElement> osmMembers = new FasterList<>();

            Map<String, String> tags = osmRelation.tags;
            String highway = tags.get("highway");
            String natural = tags.get("natural");
            String building = tags.get("building");
            String building_part = tags.get("building:part");
            String landuse = tags.get("landuse");

            NodeList relationChildren = relationElement.getChildNodes();
            for (int j = 0; j < relationChildren.getLength(); j++) {
                Node relationChild = relationChildren.item(j);
                if ("member".equals(relationChild.getNodeName())) {
                    Element relationChildElement = (Element) relationChild;
                    String type = relationChildElement.getAttribute("type");
                    String ref = relationChildElement.getAttribute("ref");
                    String role = relationChildElement.getAttribute("role");
                    OsmElement member = null;
                    switch (type) {
                        case "node":
                            member = OsmNode.getOsmNodeById(osm.nodes, ref);
                            break;
                        case "way":
                            member = OsmWay.getOsmWayById(osm.ways, ref);
                            if (member != null) {
                                if (highway != null) {
                                    member.tag("highway", highway);
                                }
                                if (natural != null) {
                                    member.tag("natural", natural);
                                }
                                if (building != null) {
                                    member.tag("building", building);
                                }
                                if (building_part != null) {
                                    member.tag("building:part", building_part);
                                }
                                if (landuse != null) {
                                    member.tag("landuse", landuse);
                                }
                            }
                            break;
                        case "relation":
                            member = OsmRelation.getOsmRelationById(osm.relations, ref);
                            break;
                    }
                    if (member != null) {
                        osmMembers.add(member);
                    }
                }
            }

            osmRelation.addChildren(osmMembers);
        }

        // Relation 3rd pass: merge multipolygon
        for (Element relationElement: relationElements) {
            String id = relationElement.getAttribute("id");

            OsmRelation osmRelation = OsmRelation.getOsmRelationById(osm.relations, id);

            Map<String, String> tags = osmRelation.tags;
            String type = tags.get("type");

            if (!"multipolygon".equals(type))
                continue;

            List<? extends OsmElement> oc = osmRelation.children();
            int s = oc.size();
            for (int i = 0; i< s; i++) {
                OsmElement e1 = oc.get(i);

                if (e1 == null || e1.getClass() != OsmWay.class)
                    continue;

                OsmWay w1 = (OsmWay)e1;
                if (w1.isClosed())
                    continue;

                repeat: {
                    ListIterator<? extends OsmElement> ii = oc.listIterator(i);
                    while (ii.hasNext()) {
                        OsmElement e2 = ii.next();
                        if (e2 == null || e2.getClass() != OsmWay.class)
                            continue;

                        OsmWay w2 = (OsmWay)e2;

                        if (w1.isFollowedBy(w2)) {
                            w1.addOsmWay(w2);
                            ii.remove();
                            break repeat; // loop again
                        }
                    }
                }
            }
        }

        return osm;
    }

    public static void printOsm(Osm osm) {
        printOsmNodes(osm.nodes);
        printOsmWays(osm.ways);
        printOsmRelations(osm.relations);
    }

    static void printTags(Map<String, String> tags, int indent) {
        for (Map.Entry<String, String> stringStringEntry : tags.entrySet()) {
            String v = stringStringEntry.getValue();
            printIndent(indent);
            System.out.printf("%s=%s\n", stringStringEntry.getKey(), v);
        }
    }

    static void printOsmNodes(List<OsmNode> osmNodes) {
        printOsmNodes(osmNodes, 0);
    }

    static void printOsmNodes(Iterable<OsmNode> osmNodes, int indent) {
        for (OsmNode osmNode: osmNodes) {
            printOsmNode(osmNode, indent);
        }
    }

    static void printOsmNode(OsmNode osmNode, int indent) {
        GeoCoordinate geoCoordinate = osmNode.getGeoCoordinate();
        printIndent(indent);
        System.out.printf("<node id=%s, lat=%f, lon=%f>\n", osmNode.id, geoCoordinate.latitude, geoCoordinate.longitude);
        printTags(osmNode.tags, indent + 1);
    }

    static void printOsmWays(List<OsmWay> osmWays) {
        printOsmWays(osmWays, 0);
    }

    static void printOsmWays(Iterable<OsmWay> osmWays, int indent) {
        for (OsmWay osmWay: osmWays) {
            printOsmWay(osmWay, indent);
        }
    }

    static void printOsmWay(OsmWay osmWay, int indent) {
        printIndent(indent);
        System.out.printf("<way id=%s>\n", osmWay.id);
        printOsmNodes(osmWay.getOsmNodes(), indent + 1);
        printTags(osmWay.tags, indent + 1);
    }

    static void printOsmRelations(List<OsmRelation> osmRelations) {
        printOsmRelations(osmRelations, 0);
    }

    static void printOsmRelations(Iterable<OsmRelation> osmRelations, int indent) {
        for (OsmRelation osmRelation: osmRelations) {
            printOsmRelation(osmRelation, indent);
        }
    }

    static void printOsmRelation(OsmRelation osmRelation, int indent) {
        printIndent(indent);
        System.out.printf("<relation id=%s>\n", osmRelation.id);
        printOsmElements(osmRelation.children(), indent + 1);
        printTags(osmRelation.tags, indent + 1);
    }

    static void printOsmElements(Iterable<? extends OsmElement> osmElements, int indent) {
        for (OsmElement osmElement: osmElements) {
            if (osmElement.getClass() == OsmNode.class) {
                printOsmNode((OsmNode)osmElement, indent);
            } else if (osmElement.getClass() == OsmWay.class) {
                printOsmWay((OsmWay) osmElement, indent);
            } else if (osmElement.getClass() == OsmRelation.class) {
                printOsmRelation((OsmRelation) osmElement, indent);
            }
        }
    }

    static void printIndent(int indent) {
        for (int i=0; i<indent; i++) {
            System.out.print("  ");
        }
    }

    void printNode(int indent, Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }

        for (int i=0; i<indent; i++) {
            System.out.print("  ");
        }
        System.out.print(node.getNodeName());

        NamedNodeMap nodeMap = node.getAttributes();
        for (int i=0; i<nodeMap.getLength(); i++) {
            Node attr = nodeMap.item(i);
            System.out.print(", " + attr.getNodeName() + '=' + attr.getNodeValue());
        }
        System.out.println();

        NodeList childNodes = node.getChildNodes();
        int l = childNodes.getLength();
        for (int i = 0; i< l; i++) {
            printNode(indent+1, childNodes.item(i));
        }
    }
}
