package spacegraph.util.geo.osm;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import jcog.data.list.FasterList;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import spacegraph.video.Draw;
import spacegraph.video.OsmSpace;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_POINTS;
import static java.lang.Double.parseDouble;
import static jcog.Texts.l;
import static jcog.Texts.n4;

/**
 * Created by unkei on 2017/04/25.
 */
public class Osm {

    /** lon,lat coordinates */
    public RectFloat geoBounds;

    public final LongObjectHashMap<OsmNode> nodes;
    private final LongObjectHashMap<OsmRelation> relations;
    public final LongObjectHashMap<OsmWay> ways;

    public boolean ready = false;
    public String id;

    public Osm() {
        nodes = new LongObjectHashMap();
        ways = new LongObjectHashMap();
        relations = new LongObjectHashMap();
    }

    public void load(String filename) throws SAXException, IOException, ParserConfigurationException {
        InputStream fis = new FileInputStream(filename);

        if (filename.endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        }

        load(fis);
    }

    public static URL url(String apiURL, double lonMin, double latMin, double lonMax, double latMax)  {
        try {
            return new URL(apiURL + "/api/0.6/map?bbox=" +
                    n4(lonMin) + ',' + n4(latMin) + ',' + n4(lonMax) + ',' + n4(latMax) );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(String apiURL, double lonMin, double latMin, double lonMax, double latMax) throws SAXException, IOException, ParserConfigurationException {
        load(url(apiURL, lonMin, latMin, lonMax, latMax).openStream());
    }

    public void load(InputStream fis) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();


        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setCoalescing(true);




        DocumentBuilder documentBuilder = factory.newDocumentBuilder();


        Document document = documentBuilder.parse(fis);



        Osm osm = this;

        Collection<Element> relationElements = new FasterList<>();

        NodeList childNodes = document.getDocumentElement().getChildNodes();
        int cl = childNodes.getLength();
        for (int i = 0; i < cl; i++) {
            Node childNode = childNodes.item(i);

            switch (childNode.getNodeName()) {
                case "bounds": {
                    Element e = ((Element) childNode);
                    assert(osm.geoBounds == null);
                    osm.geoBounds = RectFloat.XYXY(
                            (float) parseDouble(e.getAttribute("minlon")),
                            (float) parseDouble(e.getAttribute("minlat")),
                            (float) parseDouble(e.getAttribute("maxlon")),
                            (float) parseDouble(e.getAttribute("maxlat"))
                    );
                    break;
                }
                case "node": {
                    Element childElement = (Element) childNode;
                    long id = l(childElement.getAttribute("id"));

                    Map<String, String> osmTags = null;
                    NodeList nodeChildren = ((Element) childNode).getElementsByTagName("tag") /*childNode.getChildNodes()*/;
                    int nnc = nodeChildren.getLength();
                    for (int j = 0; j < nnc; j++) {
                        Node nodeChild = nodeChildren.item(j);
                        /*if ("tag".equals(nodeChild.getNodeName()))*/
                        {
                            Element ne = (Element) nodeChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap(1);
                            osmTags.put(ne.getAttribute("k"), ne.getAttribute("v"));
                        }
                    }


                    OsmNode oo = new OsmNode(id, new GeoVec3(childElement), osmTags);
                    osm.nodes.put(id, oo);


                    break;
                }
                case "way": {
                    Element childElement = (Element) childNode;
                    long id = l(childElement.getAttribute("id"));

                    List<OsmNode> refOsmNodes = new FasterList<>();
                    Map<String, String> osmTags = null;

                    NodeList wayChildren = childNode.getChildNodes();
                    int l = wayChildren.getLength();
                    for (int j = 0; j < l; j++) {
                        Node wayChild = wayChildren.item(j);
                        String node = wayChild.getNodeName();
                        if ("nd".equals(node)) {
                            Element wayChildElement = (Element) wayChild;
                            refOsmNodes.add(
                                    osm.nodes.get(l(wayChildElement.getAttribute("ref")))
                            );
                        } else if ("tag".equals(node)) {
                            Element nodeChildElement = (Element) wayChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap<>(1);
                            osmTags.put(nodeChildElement.getAttribute("k"), nodeChildElement.getAttribute("v"));
                        }
                    }

                    OsmWay ow = new OsmWay(id, refOsmNodes, osmTags);
                    osm.ways.put(id, ow);


                    break;
                }
                case "relation":
                    Element childElement = (Element) childNode;
                    long id = l(childElement.getAttribute("id"));

                    NodeList relationChildren = childElement.getElementsByTagName("tag");
                    Map<String, String> osmTags = null;
                    int l = relationChildren.getLength();
                    for (int j = 0; j < l; j++) {
                        Node relationChild = relationChildren.item(j);
                        /*if ("tag".equals(relationChild.getNodeName()))*/
                        {
                            Element e = (Element) relationChild;
                            if (osmTags == null)
                                osmTags = new UnifiedMap<>(1);
                            osmTags.put(e.getAttribute("k"), e.getAttribute("v"));
                        }
                    }

                    OsmRelation or = new OsmRelation(id, null, osmTags);
                    osm.relations.put(id, or);
                    relationElements.add(childElement);
                    break;
            }
        }


        
        for (Element relationElement : relationElements) {
            long id = l(relationElement.getAttribute("id"));

            OsmRelation osmRelation = osm.relations.get(id);

            Map<String, String> tags = osmRelation.tags;
            String highway, natural, building, building_part, landuse;
            if (tags.isEmpty()) {
                highway = natural = building = building_part = landuse = null;
            } else {
                highway = tags.get("highway");
                natural = tags.get("natural");
                building = tags.get("building");
                building_part = tags.get("building:part");
                landuse = tags.get("landuse");
            }

            NodeList relationChildren = relationElement.getElementsByTagName("member"); 
            List<OsmElement> osmMembers = null;
            int l = relationChildren.getLength();
            for (int j = 0; j < l; j++) {
                Node relationChild = relationChildren.item(j);
                /*if ("member".equals(relationChild.getNodeName()))*/
                {
                    Element r = (Element) relationChild;
                    String type = r.getAttribute("type");
                    long ref = l(r.getAttribute("ref"));

                    OsmElement member = null;
                    switch (type) {
                        case "node":
                            member = osm.nodes.get(ref);
                            break;
                        case "way":
                            member = osm.ways.get(ref);
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
                            member = osm.relations.get(ref);
                            break;
                    }
                    if (member != null) {
                        if (osmMembers == null)
                            osmMembers = new FasterList(1);
                        osmMembers.add(member);
                    }
                }
            }

            if (osmMembers != null && !osmMembers.isEmpty())
                osmRelation.addChildren(osmMembers);
        }

        
        
        for (Element relationElement : relationElements) {
            long id = l(relationElement.getAttribute("id"));

            OsmRelation osmRelation = osm.relations.get(id);

            Map<String, String> tags = osmRelation.tags;
            String type = tags.get("type");

            if (!"multipolygon".equals(type))
                continue;

            List<? extends OsmElement> oc = osmRelation.children();
            int s = oc.size();
            for (int i = 0; i < s; i++) {
                OsmElement e1 = oc.get(i);

                if (e1 == null || e1.getClass() != OsmWay.class)
                    continue;

                OsmWay w1 = (OsmWay) e1;
                if (w1.isClosed())
                    continue;

                repeat:
                {
                    ListIterator<? extends OsmElement> ii = oc.listIterator(i);
                    while (ii.hasNext()) {
                        OsmElement e2 = ii.next();
                        if (e2 == null || e2.getClass() != OsmWay.class)
                            continue;

                        OsmWay w2 = (OsmWay) e2;

                        if (w1.isFollowedBy(w2)) {
                            w1.addOsmWay(w2);
                            ii.remove();
                            
                        }
                    }
                }
            }
        }

    }

    public void clear() {
        nodes.clear();
        ways.clear();
        relations.clear();
    }

    public void print() {
        printOsmNodes(nodes);
        printOsmWays(ways);
        printOsmRelations(relations);
    }

    private static void printTags(Map<String, String> tags, int indent) {
        for (Map.Entry<String, String> stringStringEntry : tags.entrySet()) {
            String v = stringStringEntry.getValue();
            printIndent(indent);
            System.out.printf("%s=%s\n", stringStringEntry.getKey(), v);
        }
    }

    private static void printOsmNodes(Iterable<OsmNode> osmNodes) {
        printOsmNodes(osmNodes, 0);
    }

    private static void printOsmNodes(Iterable<OsmNode> osmNodes, int indent) {
        for (OsmNode osmNode : osmNodes) {
            printOsmNode(osmNode, indent);
        }
    }

    private static void printOsmNode(OsmNode osmNode, int indent) {
        GeoVec3 pos = osmNode.pos;
        printIndent(indent);
        System.out.printf("<node id=%s, lat=%f, lon=%f>\n", osmNode.id, pos.getLatitude(), pos.getLongitude());
        printTags(osmNode.tags, indent + 1);
    }

    private static void printOsmWays(Iterable<OsmWay> osmWays) {
        printOsmWays(osmWays, 0);
    }

    private static void printOsmWays(Iterable<OsmWay> osmWays, int indent) {
        for (OsmWay osmWay : osmWays) {
            printOsmWay(osmWay, indent);
        }
    }

    private static void printOsmWay(OsmWay osmWay, int indent) {
        printIndent(indent);
        System.out.printf("<way id=%s>\n", osmWay.id);
        printOsmNodes(osmWay.getOsmNodes(), indent + 1);
        printTags(osmWay.tags, indent + 1);
    }

    private static void printOsmRelations(Iterable<OsmRelation> osmRelations) {
        printOsmRelations(osmRelations, 0);
    }

    private static void printOsmRelations(Iterable<OsmRelation> osmRelations, int indent) {
        for (OsmRelation osmRelation : osmRelations) {
            printOsmRelation(osmRelation, indent);
        }
    }

    private static void printOsmRelation(OsmRelation osmRelation, int indent) {
        printIndent(indent);
        System.out.printf("<relation id=%s>\n", osmRelation.id);
        printOsmElements(osmRelation.children(), indent + 1);
        printTags(osmRelation.tags, indent + 1);
    }

    private static void printOsmElements(Iterable<? extends OsmElement> osmElements, int indent) {
        for (OsmElement osmElement : osmElements) {
            if (osmElement.getClass() == OsmNode.class) {
                printOsmNode((OsmNode) osmElement, indent);
            } else if (osmElement.getClass() == OsmWay.class) {
                printOsmWay((OsmWay) osmElement, indent);
            } else if (osmElement.getClass() == OsmRelation.class) {
                printOsmRelation((OsmRelation) osmElement, indent);
            }
        }
    }

    private static void printIndent(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
    }

    private void printNode(int indent, Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
        System.out.print(node.getNodeName());

        NamedNodeMap nodeMap = node.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node attr = nodeMap.item(i);
            System.out.print(", " + attr.getNodeName() + '=' + attr.getNodeValue());
        }
        System.out.println();

        NodeList childNodes = node.getChildNodes();
        int l = childNodes.getLength();
        for (int i = 0; i < l; i++) {
            printNode(indent + 1, childNodes.item(i));
        }
    }


    static final Consumer<GL2> loading = (gl)->{
        gl.glColor3f(1, 0, 0);
        Draw.rectFrame(gl, 0, 0, 1, 1, 0.1f);
    };

    public Consumer<GL2> render(OsmSpace space, GL2 gl) {

        if (!ready)
            return loading;
        else {
            GLContext ctx = gl.getContext();
            Object c = ctx.getAttachedObject(id);
            if (c == null) {
                c = compile(gl, this);
                ctx.attachObject(id, c);
            }
            return (Consumer<GL2>) c;
        }
    }


    private OsmSpace.OsmRenderer compile(GL2 G, Osm osm) {

        OsmSpace.OsmRenderer rr = new OsmSpace.OsmRenderer(G);
        GLUtessellator tobj = rr.tobj;

        boolean wireframe = false;

        List<Consumer<GL2>> draw = rr.draw;


        float bx = osm.geoBounds.cx(), by = osm.geoBounds.cy();

        for (OsmWay way : osm.ways) {

            Map<String, String> tags = way.tags;
            String building, building_part, landuse, natural, route, highway;
            if (!tags.isEmpty()) {

                building = tags.get("building");
                building_part = tags.get("building:part");
                landuse = tags.get("landuse");
                natural = tags.get("natural");
                route = tags.get("route");
                highway = tags.get("highway");
            } else {
                building = building_part = landuse = natural = route = highway = null;
            }

            boolean isPolygon = false;
            boolean isClosed = way.isClosed();
            float r, g, b, a;
            float lw;
            short ls;

            if (building != null || building_part != null) {
                r = 0f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
                isPolygon = !wireframe && isClosed;
            } else if ("forest".equals(landuse) || "grass".equals(landuse) || "wood".equals(natural)) {
                r = 0f;
                g = 1f;
                b = 0f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
                isPolygon = !wireframe && isClosed;
            } else if ("water".equals(natural)) {
                r = 0f;
                g = 0f;
                b = 1f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
                isPolygon = !wireframe && isClosed;
            } else if ("pedestrian".equals(highway)) {
                r = 0f;
                g = 0.5f;
                b = 0f;
                a = 1f;
                lw = 2f;
                ls = (short) 0xFFFF;
            } else if ("motorway".equals(highway)) {
                r = 1f;
                g = 0.5f;
                b = 0f;
                a = 1f;
                lw = 5f;
                ls = (short) 0xFFFF;
            } else if (highway != null) {
                r = 1f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 3f;
                ls = (short) 0xFFFF;
            } else if ("road".equals(route)) {
                r = 1f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
            } else if ("train".equals(route)) {
                r = 1f;
                g = 1f;
                b = 1f;
                a = 1f;
                lw = 5f;
                ls = (short) 0xF0F0;
            } else {
                r = 0.5f;
                g = 0f;
                b = 0.5f;
                a = 1f;
                lw = 1f;
                ls = (short) 0xFFFF;
            }


            if (isPolygon) {
                List<OsmNode> nn = way.getOsmNodes();
                double coord[][] = new double[nn.size()][7];


                for (int i = 0, nnSize = nn.size(); i < nnSize; i++) {
                    OsmNode node = nn.get(i);

                    double[] ci = coord[i];
                    project(node.pos, ci, bx, by);
                    ci[3] = r;
                    ci[4] = g;
                    ci[5] = b;
                    ci[6] = a;
                }

                draw.add((gl) -> {
                    gl.glColor4f(r * 0.5f, g * .5f, b * 0.5f, a);
                    gl.glLineWidth(lw);
                    gl.glLineStipple(1, ls);

                    GLU.gluTessBeginPolygon(tobj, null);
                    GLU.gluTessBeginContour(tobj);
                    for (int i = 0, nnSize = nn.size(); i < nnSize; i++) {
                        double[] ci = coord[i];
                        GLU.gluTessVertex(tobj, ci, 0, ci);
                    }
                    GLU.gluTessEndContour(tobj);
                    GLU.gluTessEndPolygon(tobj);

                });


            } else {

                List<OsmNode> ways = way.getOsmNodes();
                int ws = ways.size();
                double c3[] = new double[3 * ws];
                for (int i = 0, waysSize = ws; i < waysSize; i++) {
                    project(ways.get(i).pos, c3, i * 3, bx, by);
                }

                draw.add((gl) -> {
                    gl.glColor4f(r, g, b, a);
                    gl.glLineWidth(lw);
                    gl.glLineStipple(1, ls);
                    gl.glBegin(GL_LINE_STRIP);
                    for (int i = 0; i < c3.length / 3; i++) {
                        gl.glVertex3dv(c3, i * 3);
                    }
                    gl.glEnd();
                });

            }

        }


        for (OsmNode node : osm.nodes) {
            Map<String, String> tags = node.tags;

            if (tags.isEmpty()) continue;
            String highway = tags.get("highway");
            String natural = tags.get("natural");

            float pointSize;
            float r, g, b, a;
            if ("bus_stop".equals(highway)) {

                pointSize = 3;
                r = g = b = 1f;
                a = 0.7f;
            } else if ("traffic_signals".equals(highway)) {
                pointSize = 3;
                r = g = 1f;
                b = 0f;
                a = 0.7f;

            } else if ("tree".equals(natural)) {
                pointSize = 3;
                g = 1f;
                r = b = 0f;
                a = 0.7f;

            } else {
                pointSize = 3;
                r = 1f;
                g = b = 0f;
                a = 0.7f;
            }

            double[] c3 = new double[3];
            project(node.pos, c3, bx, by);

            draw.add(gl -> {
                gl.glPointSize(pointSize);
                gl.glBegin(GL_POINTS);
                gl.glColor4f(r, g, b, a);

                gl.glVertex3d(c3[0], c3[1], c3[2]);
                gl.glEnd();
            });

        }

        return rr;
    }

    private static void project(GeoVec3 global, double[] target, float boundsX, float boundsY) {
        project(global, target, 0, boundsX, boundsY);
    }

    private static void project(GeoVec3 global, double[] target, int offset, float boundsX, float boundsY) {


        target[offset++] = (global.getLatitude());// - boundsY);
        target[offset++] = (global.getLongitude());// - boundsX);
        target[offset/*++*/] = 0;


    }

}
