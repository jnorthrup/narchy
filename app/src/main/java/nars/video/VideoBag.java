//package nars.video;
//
//
//import boofcv.abst.flow.DenseOpticalFlow;
//import boofcv.alg.color.ColorRgb;
//import boofcv.factory.flow.FactoryDenseOpticalFlow;
//import boofcv.struct.flow.ImageFlow;
//import boofcv.struct.image.GrayU8;
//import boofcv.struct.image.ImageBase;
//import boofcv.struct.image.InterleavedU8;
//import com.github.sarxos.webcam.WebcamEvent;
//import com.jogamp.common.util.IOUtil;
//import com.jogamp.opengl.GL;
//import com.jogamp.opengl.GL2;
//import com.jogamp.opengl.GLContext;
//import com.jogamp.opengl.GLProfile;
//import com.jogamp.opengl.util.texture.Texture;
//import com.jogamp.opengl.util.texture.TextureIO;
//import jcog.Util;
//import jcog.spatial.RTree;
//import jcog.spatial.Rect1D;
//import nars.$;
//import nars.bag.ArrayBag;
//import nars.budget.BudgetMerge;
//import nars.gui.BagChart;
//import nars.link.BLink;
//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import spacegraph.SpaceGraph;
//import spacegraph.Surface;
//import spacegraph.render.Draw;
//import spacegraph.video.WebCam;
//
//import java.io.*;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.channels.FileChannel;
//import java.util.Date;
//import java.util.Queue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Consumer;
//
//import static jcog.Util.unitize;
//
///**
// * Created by me on 12/12/16.
// */
//public class VideoBag {
//
//    final static Logger logger = LoggerFactory.getLogger(VideoBag.class);
//
//    final RTree<Frame> index;
//
//    final ArrayBag<Frame> bag;
//
//    private final Queue<Frame> textureTrash = new ConcurrentLinkedQueue<>();
//
//    public class Frame extends Rect1D {
//
//        public final long t;
//        public final ImageBase image;
//        public final int width, height;
//        public Texture texture;
//
//        public Frame(long when, @NotNull ImageBase i) {
//            this(when, i, i.getWidth(), i.getHeight());
//        }
//
//        public Frame(long when, @NotNull ImageBase i, int width, int height) {
//            this.t = when;
//            this.image = i;
//            this.width = width;
//            this.height = height;
//        }
//
//        @Override
//        public String toString() {
//            return new Date(t).toString() + ": " + image.toString();
//        }
//
//        @Override
//        public double from() {
//            return t;
//        }
//
//        @Override
//        public double to() {
//            return t;
//        }
//
//        public int getWidth() {
//            return width;
//        }
//
//        public int getHeight() {
//            return height;
//        }
//
//        public void delete() {
//            textureTrash.add(this);
//        }
//    }
//
//    public VideoBag(int initialCapacity) {
//        bag = new ArrayBag<Frame>(initialCapacity, BudgetMerge.avgBlend, new ConcurrentHashMap<>()) {
//            @Override
//            public void onAdded(BLink<Frame> v) {
//                index.add(v.get());
//            }
//
//            @Override
//            public void onRemoved(BLink<Frame> v) {
//                Frame f = v.get();
//                index.remove(f);
//                f.delete();
//            }
//        };
//        index = new RTree<>((l) -> l, 2, 8, RTree.Split.LINEAR);
//
//
//    }
//
//    InterleavedU8 prev = null;
//    final AtomicBoolean busy = new AtomicBoolean(false);
//    private GrayU8 prevU, nextU;
//    private ImageFlow ff;
//
//    private final DenseOpticalFlow<GrayU8> flow = FactoryDenseOpticalFlow.
//            //hornSchunck(null, GrayU8.class);
//                    broxWarping(null, GrayU8.class);
//
//    public boolean put(@NotNull InterleavedU8 current) {
//
//        boolean added;
//        if (busy.compareAndSet(false, true)) {
//
//            synchronized (bag) {
//                long when = System.currentTimeMillis();
//                //                    ImageBase di =
//                //                            //        null;
//                //                            ImageBase.createFromData(c.width, c.height, false, true, ByteBuffer.wrap(current.data));
//
//                final Frame frame = new Frame(when, current);
//                //System.out.println(current.data + " " + bag.rtree.collectStats());
//
//                float scale;
//                InterleavedU8 prev = this.prev;
//                if (prev != null) {
//
//                    final int cw = current.width;
//                    final int ch = current.height;
//                    if (prevU == null || prevU.width != cw || prevU.height != ch) {
//                        prevU = new GrayU8(cw, ch);
//                        nextU = new GrayU8(cw, ch);
//                        ff = new ImageFlow(cw, ch);
//                    }
//
//                    ColorRgb.rgbToGray_Weighted(prev, prevU);
//
//                    ColorRgb.rgbToGray_Weighted(current, nextU);
//
//                    flow.process(prevU, nextU, ff);
//
//                    float totalFlow = 0;
//                    for (ImageFlow.D d : ff.data) {
//                        if (d.isValid()) {
//                            totalFlow += Math.sqrt(Util.sqr(d.x) + Util.sqr(d.y));
//                        }
//                    }
//                    float avgFlow = unitize(totalFlow / (cw * ch));
//                    logger.info("avg flow per pixel: {} ", avgFlow);
//
//
//                    //                    BufferedImage visualized = new BufferedImage(current.width,current.height, BufferedImage.TYPE_INT_RGB);
//                    //VisualizeOpticalFlow.colorized(ff, 10, visualized);
//                    //
//                    //                    gui.setContentPane(new ImagePanel(visualized));
//                    //                    gui.doLayout();
//                    //                    gui.invalidate();
//                    //                    gui.repaint();
//
//
//                    scale = unitize(avgFlow);
//
//
//                } else {
//
//                    scale = 1f;
//                }
//
//                bag.commit();
//                bag.put(frame, $.b(0.1f + 0.9f * scale, 0.1f + 0.9f * scale), 1f, null);
//                busy.set(false);
//
//                added = true;
//            }
//
//        } else {
//
//            logger.info("frame dropped");
//
//            added = false;
//
//        }
//
//        this.prev = current;
//
//        return added;
//
//    }
//
//    public static void main(String[] args) {
//        VideoBag bag = new VideoBag(16);
//        final BagChart s = new BagChart(bag.bag, 16);
//
//
////        JFrame gui = new JFrame();
////        gui.setSize(400,400);
////        gui.setVisible(true);
//
//
//        final WebCam cam;
//        cam = new WebCam(320, 200);
//        cam.eventChange.on(new Consumer<WebcamEvent>() {
//
//
//            //final ExecutorService exe = Executors.newSingleThreadExecutor();
//
//            @Override
//            public void accept(WebcamEvent c) {
//
//                //DDSImage di = DDSImage.createFromData(DDSImage.D3DFMT_R8G8B8, width, height, new ByteBuffer[]{image.asReadOnlyBuffer()});
//                //JPEGImage di = JPEGImage.read(new ByteBufferInputStream(image.asReadOnlyBuffer()));
//
//
//                s.update();
//
////                InterleavedU8 current = new InterleavedU8(c.width, c.height, 3);
////                current.data = c.image.clone();
//
//                //exe.execute(() -> {
//                if (cam.iimage!=null)
//                    bag.put(cam.iimage);
//                //});
//
//
//            }
//        });
//
//        SpaceGraph.window(s, 800, 800);
//        SpaceGraph.window(new EventTimeline(bag), 800, 800);
//    }
//
//    static class EventTimeline extends Surface {
//
//        public final VideoBag bag;
//
//        float scale = 0.25f; //seconds to visual units
//
//
//        EventTimeline(VideoBag bag) {
//            this.bag = bag;
//        }
//
//        @Override
//        protected void paint(GL2 gl) {
//            super.paint(gl);
//
//
////            //reclaim deleted textures
//            while (!bag.textureTrash.isEmpty()) {
//                Frame ff = bag.textureTrash.remove();
////                ((InterleavedU8)ff.image).data = null;
////                System.out.println("destroying: " + ff.texture.getTextureObject());
////                ff.texture.disable(gl);
////                ff.texture.destroy(gl);
////                ff.texture = null;
//            }
//
//            long now = System.currentTimeMillis();
//
//            bag.bag.forEach((ff) -> {
//                Frame f = ff.get();
//
//                Texture tt = f.texture;
//                if (tt == null) {
//
//
//                    byte[] data = ((InterleavedU8)f.image).data;
//
//
//                    tt = tgaTexture(f.width, f.height, false, data);
//
//                    System.out.println("creating: " + tt.getTextureObject());
//
//                    tt.enable(gl);
//                    tt.bind(gl);
//
//
//                    f.texture = tt;
//
//                }
//
//                float pp = ff.priElseZero();
//                float xx = scale * (f.t - now) / 1000f ;
//                float yy = (float) Math.sin(f.t);
//                //if (tt!=null) {
//                    float sc = scale * (1f + pp);
//                    Draw.rectTex(gl, tt, xx, yy, sc, sc, 0);
////                } else {
////                    Draw.colorPolarized(gl, 0.25f + 0.75f * pp);
////                    Draw.rect(gl, xx, yy, scale, scale);
////                }
//
//                tt.disable(gl);
//                tt.destroy(gl);
//                f.texture = null;
//            });
//        }
//
//
//
//    }
//
//    static class MyByteArrayOutputStream extends ByteArrayOutputStream {
//        public MyByteArrayOutputStream(int capacity) {
//            super(capacity );
//        }
//
//        public byte[] array() { return buf; }
//    }
//
//
//    public static class LEDataInputStream extends FilterInputStream implements DataInput
//    {
//        /**
//         * To reuse    some of    the    non    endian dependent methods from
//         * DataInputStreams    methods.
//         */
//        DataInputStream    dataIn;
//
//        public LEDataInputStream(final InputStream in)
//        {
//            super(in);
//            dataIn = new DataInputStream(in);
//        }
//
//        @Override
//        public void close() throws IOException
//        {
//            dataIn.close();        // better close as we create it.
//            // this will close underlying as well.
//        }
//
//        @Override
//        public synchronized    final int read(final byte    b[]) throws    IOException
//        {
//            return dataIn.read(b, 0, b.length);
//        }
//
//        @Override
//        public synchronized    final int read(final byte    b[], final int off, final int len) throws IOException
//        {
//            final int    rl = dataIn.read(b,    off, len);
//            return rl;
//        }
//
//        @Override
//        public final void readFully(final byte b[]) throws IOException
//        {
//            dataIn.readFully(b,    0, b.length);
//        }
//
//        @Override
//        public final void readFully(final byte b[], final int off, final int len)    throws IOException
//        {
//            dataIn.readFully(b,    off, len);
//        }
//
//        @Override
//        public final int skipBytes(final int n) throws IOException
//        {
//            return dataIn.skipBytes(n);
//        }
//
//        @Override
//        public final boolean readBoolean() throws IOException
//        {
//            final int    ch = dataIn.read();
//            if (ch < 0)
//                throw new EOFException();
//            return (ch != 0);
//        }
//
//        @Override
//        public final byte readByte() throws    IOException
//        {
//            final int    ch = dataIn.read();
//            if (ch < 0)
//                throw new EOFException();
//            return (byte)(ch);
//        }
//
//        @Override
//        public final int readUnsignedByte()    throws IOException
//        {
//            final int    ch = dataIn.read();
//            if (ch < 0)
//                throw new EOFException();
//            return ch;
//        }
//
//        @Override
//        public final short readShort() throws IOException
//        {
//            final int    ch1    = dataIn.read();
//            final int    ch2    = dataIn.read();
//            if ((ch1 | ch2)    < 0)
//                throw new EOFException();
//            return (short)((ch1    << 0) +    (ch2 <<    8));
//        }
//
//        @Override
//        public final int readUnsignedShort() throws    IOException
//        {
//            final int    ch1    = dataIn.read();
//            final int    ch2    = dataIn.read();
//            if ((ch1 | ch2)    < 0)
//                throw new EOFException();
//            return (ch1    << 0) +    (ch2 <<    8);
//        }
//
//        @Override
//        public final char readChar() throws    IOException
//        {
//            final int    ch1    = dataIn.read();
//            final int    ch2    = dataIn.read();
//            if ((ch1 | ch2)    < 0)
//                throw new EOFException();
//            return (char)((ch1 << 0) + (ch2    << 8));
//        }
//
//        @Override
//        public final int readInt() throws IOException
//        {
//            final int    ch1    = dataIn.read();
//            final int    ch2    = dataIn.read();
//            final int    ch3    = dataIn.read();
//            final int    ch4    = dataIn.read();
//            if ((ch1 | ch2 | ch3 | ch4)    < 0)
//                throw new EOFException();
//            return ((ch1 <<    0) + (ch2 << 8)    + (ch3 << 16) +    (ch4 <<    24));
//        }
//
//        @Override
//        public final long readLong() throws    IOException
//        {
//            final int    i1 = readInt();
//            final int    i2 = readInt();
//            return (i1 & 0xFFFFFFFFL) + ((long)i2 << 32);
//        }
//
//        @Override
//        public final float readFloat() throws IOException
//        {
//            return Float.intBitsToFloat(readInt());
//        }
//
//        @Override
//        public final double    readDouble() throws    IOException
//        {
//            return Double.longBitsToDouble(readLong());
//        }
//
//        /**
//         * dont call this it is not implemented.
//         * @return empty new string
//         **/
//        @Override
//        public final String    readLine() throws IOException
//        {
//            return "";
//        }
//
//        /**
//         * dont call this it is not implemented
//         * @return empty new string
//         **/
//        @Override
//        public final String    readUTF() throws IOException
//        {
//            return "";
//        }
//
//        /**
//         * dont call this it is not implemented
//         * @return empty new string
//         **/
//        public final static    String readUTF(final DataInput in) throws    IOException
//        {
//            return "";
//        }
//    }
//
//
//}
