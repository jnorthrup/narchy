package spacegraph.util.geo.osm;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import jcog.Util;
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

    @Override
    public String toString() {
        return geoBounds + " (" + nodes.size() + " nodes, " + relations.size() + " relations, " + ways.size() + " ways";
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

    /** TODO make static so it can customize/optimize the returned instance */
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
            Node n = childNodes.item(i);

            switch (n.getNodeName()) {
                case "bounds": {
                    Element e = ((Element) n);
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
                    Element childElement = (Element) n;
                    long id = l(childElement.getAttribute("id"));

                    Map<String, String> osmTags = null;
                    NodeList nodeChildren = ((Element) n).getElementsByTagName("tag") /*childNode.getChildNodes()*/;
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
                    Element childElement = (Element) n;
                    long id = l(childElement.getAttribute("id"));

                    List<OsmNode> refOsmNodes = new FasterList<>();
                    Map<String, String> osmTags = null;

                    NodeList wayChildren = n.getChildNodes();
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
                    Element childElement = (Element) n;
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

            List<? extends OsmElement> oc = osmRelation.children;
            for (int i = 0, ocSize = oc.size(); i < ocSize; i++) {
                OsmElement e1 = oc.get(i);

                if (e1 == null || e1.getClass() != OsmWay.class)
                    return;

                OsmWay w1 = (OsmWay) e1;
                if (w1.isLoop())
                    return;

                {
                    ListIterator<? extends OsmElement> ii = osmRelation.children.listIterator(i);
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


    public boolean isEmpty() {
        return this.nodes.isEmpty() && this.ways.isEmpty() && this.relations.isEmpty();
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
        for (OsmElement osmNode : osmNodes) {
            printOsmNode((OsmNode) osmNode, indent);
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
        printOsmElements(osmRelation.children, indent + 1);
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

    public Consumer<GL2> render(GL2 gl) {

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

        boolean wireframe = false;

        List<Consumer<GL2>> draw = rr.draw;

        for (OsmWay way : osm.ways) {

            Map<String, String> tags = way.tags;

            boolean isClosed = way.isLoop();
            boolean isPolygon = false;
            float r, g, b, a;
            float lw;
            short ls;

            r = 0.5f;
            g = 0.5f;
            b = 0.5f;
            a = 1f;
            lw = 1f;
            ls = (short) 0xFFFF;

            if (!tags.isEmpty()) {

                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    String k = entry.getKey(), v = entry.getValue();
                    switch (k) {
                        case "building":
                            r = 0f;
                            g = 1f;
                            b = 1f;
                            a = 1f;
                            lw = 1f;
                            isPolygon = true;
                            break;
                        case "natural":
                            switch (v) {
                                case "water":
                                    r = 0f;
                                    g = 0f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                                case "wood":
                                    r = 0f;
                                    g = 1f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 1f;
                                    isPolygon = true;
                                    break;
                                default:
                                    System.out.println("unstyled: " + k + " = " + v);
                                    break;
                            }
                            break;
//                        case "landuse":
//                            switch (v) {
//                                case "forest":
//                                case "grass":
//                                    r = 0.1f;
//                                    g = 0.9f;
//                                    b = 0f;
//                                    a = 1f;
//                                    lw = 1f;
//                                    isPolygon = true;
//                                    break;
//                                case "industrial":
//                                    break;
//                                default:
//                                    System.out.println("unstyled: " + k + " = " + v);
//                                    break;
//                            }
//                            break;
                        case "route":
                            switch (v) {
                                case "road":
                                    r = 1f;
                                    g = 1f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 1f;
                                    break;
                                case "train":
                                    r = 1f;
                                    g = 1f;
                                    b = 1f;
                                    a = 1f;
                                    lw = 5f;
                                    ls = (short) 0xF0F0;
                                    break;
                                default:
                                    System.out.println("unstyled: " + k + " = " + v);
                                    break;
                            }
                            break;
                        case "highway":
                            switch (v) {
                                case "pedestrian":
                                    r = 0f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 2f;
                                    break;
                                case "motorway":
                                    r = 1f;
                                    g = 0.5f;
                                    b = 0f;
                                    a = 1f;
                                    lw = 5f;
                                    break;
                                default:
                                    r = 1f;
                                    g = 1f;
                                    b = 1f;
                                    a = 0.5f;
                                    lw = 3f;
                                    break;
                            }
                            break;
                    }
                }

            }

            isPolygon &= !wireframe && isClosed;

            if (isPolygon) {
                List<OsmNode> nn = way.getOsmNodes();
                float[][] coord = new float[nn.size()][7];


                for (int i = 0, nnSize = nn.size(); i < nnSize; i++) {
                    OsmNode node = nn.get(i);

                    float[] ci = coord[i];
                    project(node.pos, ci);
                    ci[3] = r;
                    ci[4] = g;
                    ci[5] = b;
                    ci[6] = a;
                }

                draw.add(new OsmPolygonDraw(r, g, b, a, lw, ls, rr.tobj, nn, coord));


            } else {

                List<OsmNode> ways = way.getOsmNodes();
                int ws = ways.size();
                float[] c3 = new float[3 * ws];
                for (int i = 0, waysSize = ws; i < waysSize; i++) {
                    project(ways.get(i).pos, c3, i * 3);
                }

                draw.add(new OsmLineDraw(r, g, b, a, lw, ls, c3));

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

            float[] c3 = new float[3];

            project(node.pos, c3);

            draw.add(new OsmDrawPoint(pointSize, r, g, b, a, c3));
        }

        return rr;
    }

    private static void project(GeoVec3 global, float[] target) {
        project(global, target, 0);
    }

    private static void project(GeoVec3 global, float[] target, int offset) {
        target[offset++] = global.getLongitude();
        target[offset++] = global.getLatitude();
        target[offset/*++*/] = 0;
    }

    private static class OsmPolygonDraw implements Consumer<GL2> {
        private final float r,g,b,a;
        private final float lw;
        private final short ls;
        private final GLUtessellator tobj;
        private final List<OsmNode> nn;
        private final float[][] coord;

        OsmPolygonDraw(float r, float g, float b, float a, float lw, short ls, GLUtessellator tobj, List<OsmNode> nn, float[][] coord) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lw = lw;
            this.ls = ls;
            this.tobj = tobj;
            this.nn = nn;
            this.coord = coord;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor4f(r, g , b, a);
            gl.glLineWidth(lw);
            gl.glLineStipple(1, ls);

            GLU.gluTessBeginPolygon(tobj, null);
            GLU.gluTessBeginContour(tobj);
            for (int i = 0, nnSize = nn.size(); i < nnSize; i++) {
                float[] ci = coord[i];
                GLU.gluTessVertex(tobj, Util.toDouble(ci), 0, ci);
            }
            GLU.gluTessEndContour(tobj);
            GLU.gluTessEndPolygon(tobj);

        }
    }

    private static class OsmLineDraw implements Consumer<GL2> {
        private final float r,g,b,a;
        private final float lw;
        private final short ls;
        private final float[] c3;

        public OsmLineDraw(float r, float g, float b, float a, float lw, short ls, float[] c3) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lw = lw;
            this.ls = ls;
            this.c3 = c3;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor4f(r, g, b, a);
            gl.glLineWidth(lw);
            gl.glLineStipple(1, ls);
            gl.glBegin(GL_LINE_STRIP);
            for (int i = 0; i < c3.length / 3; i++) {
                gl.glVertex3fv(c3, i * 3);
            }
            gl.glEnd();
        }
    }

    private static class OsmDrawPoint implements Consumer<GL2> {
        private final float pointSize;
        private final float r, g, b, a;
        private final float[] c3;

        public OsmDrawPoint(float pointSize, float r, float g, float b, float a, float[] c3) {
            this.pointSize = pointSize;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.c3 = c3;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glPointSize(pointSize);
            gl.glBegin(GL_POINTS);
            gl.glColor4f(r, g, b, a);

            gl.glVertex3f(c3[0], c3[1], c3[2]);
            gl.glEnd();
        }
    }
}
