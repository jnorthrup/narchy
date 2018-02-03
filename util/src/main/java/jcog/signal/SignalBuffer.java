package jcog.signal;

import org.apache.commons.math3.transform.DctNormalization;
import org.apache.commons.math3.transform.FastCosineTransformer;

/** waveform RAM
 * https://github.com/jcodec/jcodec/blob/master/src/test/java/org/jcodec/codecs/mpa/Mp3DecoderTest.java
 * */
public class SignalBuffer {

    static final FastCosineTransformer x = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I);

    public static void main(String[] args) {

    }
//    public static void main(String[] args) throws IOException, JCodecException {
//        File mp3 = new File("/tmp/1.mp3");
//
////        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
////        grab.seekToSecondPrecise(startSec);
//
//
//        SeekableByteChannel ch = null;
//        try {
//            ch = NIOUtils.readableChannel(mp3);
//
//            MPEGAudioDemuxer mp3Parser = new MPEGAudioDemuxer(ch);
//
//            DemuxerTrack demuxerTrack = mp3Parser.getAudioTracks().get(0);
//
//            DemuxerTrackMeta meta = demuxerTrack.getMeta();
//            //System.out.println(meta.getAudioCodecMeta());
//
//            //Assert.assertNull(mp3Parser.getVideoTracks());
//            Mp3Decoder decoder = new Mp3Decoder();
//
//            ByteBuffer bb = ByteBuffer.allocate(64*1024);
//
//            Packet pkt;// = demuxerTrack.nextFrame();
//            for (int i = 0; i < 16; i++) {
//
//                pkt = demuxerTrack.nextFrame();
//                System.out.println(pkt.getTapeTimecode());
//                System.out.println(pkt.getFrameType());
//                System.out.println(pkt.getData().remaining());
//                System.out.println(Texts.n2(pkt.getData().array()));
//                //Assert.assertNotNull(nextFrame);
//                //Assert.assertEquals(522, nextFrame.getData().remaining());
//
//                bb.rewind();
//                AudioBuffer audioBuffer = decoder.decodeFrame(pkt.getData(), bb);
//
//
//                byte[] frame = NIOUtils.toArray(audioBuffer.getData());
//                System.out.println(frame.length);
//
////                Assert.assertArrayEquals(String.format("frame %d", i), NIOUtils.toArray(frame),
////                        NIOUtils.toArray(audioBuffer.getData()));
//
//            }
//            //Assert.assertNull(demuxerTrack.nextFrame());
//
//
//            //Assert.assertEquals(44100, meta.getAudioCodecMeta().getSampleRate());
//            //Assert.assertEquals(2, meta.getAudioCodecMeta().getChannelCount());
//        } finally {
//            NIOUtils.closeQuietly(ch);
//        }
//    }
//
//    public static class SequenceEncoderDemo {
//        //private static final MainUtils.Flag FLAG_FRAMES = new MainUtils.Flag("n-frames", "frames", "Total frames to encode");
//        //private static final MainUtils.Flag[] FLAGS = new MainUtils.Flag[] {FLAG_FRAMES};
//
//        public static void main(String[] args) throws IOException {
//
//            final int speed = 4;
//            final int ballSize = 4;
//
//            AWTSequenceEncoder enc = AWTSequenceEncoder.createSequenceEncoder(new File("/tmp/x.mp4"),  20);
//            //SequenceEncoder enc = new SequenceEncoder()
//            int framesToEncode = 128;
//
//            long totalNano = 0;
//            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY);
//            for (int i = 0, x = 0, y = 0, incX = speed, incY = speed; i < framesToEncode; i++, x += incX, y += incY) {
//                Graphics g = image.getGraphics();
//                g.setColor(Color.BLACK);
//                g.fillRect(0, 0, image.getWidth(), image.getHeight());
//                g.setColor(Color.YELLOW);
//                if (x >= image.getWidth() - ballSize)
//                    incX = -speed;
//                if (y >= image.getHeight() - ballSize)
//                    incY = -speed;
//                if (x <= 0)
//                    incX = speed;
//                if (y <= 0)
//                    incY = speed;
//                g.fillOval(x, y, ballSize, ballSize);
//                long start = System.nanoTime();
//                enc.encodeImage(image);
//                totalNano += System.nanoTime() - start;
//            }
//            enc.finish();
//
//            System.out.println("FPS: " + ((1000000000L * framesToEncode) / totalNano));
//        }
//    }
//    public static class SequenceDecoderDemo {
//        public static void main(String[] args) throws IOException, JCodecException {
//
//            int frameCount = 10;
//            File file = new File("/tmp/x.mp4");
//
//            FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
//
//            //double startSec = 51.632;
//            //grab.seekToSecondPrecise(startSec);
//
//            for (int i=0;i<frameCount;i++) {
//                Picture picture = grab.getNativeFrame();
//                System.out.println(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
//                //for JDK (jcodec-javase)
//                BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
//                //ImageIO.write(bufferedImage, "png", new File("frame"+i+".png"));
//
//                //for Android (jcodec-android)
//                //Bitmap bitmap = AndroidUtil.toBitmap(picture);
//                //bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream("frame"+i+".png"));
//            }
//        }
//    }
}
