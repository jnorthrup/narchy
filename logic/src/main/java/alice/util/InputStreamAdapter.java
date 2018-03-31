//package alice.util;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.Reader;
//
///**
// * This class let a Reader be viewed as an InputStream.
// * The characters read from the given reader are casted to bytes.
// * It works correctly only if the reader use the ASCII-8 enconding
// * (because a char is 2 bytes, but, using the ASCII-8 enconding, the
// * high byte is always 0, and so the downcasting can be done without
// * losing data)
// * @author Andrea Bucaletti
// *
// */
//public class InputStreamAdapter extends InputStream {
//
//	private final Reader reader;
//
//	public InputStreamAdapter(Reader rd) {
//		this.reader = rd;
//	}
//
//	@Override
//	public int read() throws IOException {
//		int x = reader.read();
//
//        return x == -1 ? -1 : x & 0xFF;
//	}
//
//	/**
//	 * Since the Reader class doesn't provide the available() method,
//	 * there is no way to know how many characters can be read without
//	 * blocking. So we use the ready() method of the Reader, which is true
//	 * if the next character can be read without blocking.
//	 * So if writer.ready() is true, at least 1 chatacter is available;
//	 * otherwise return 0.
//	 */
//	@Override
//	public int available() {
//		try { return reader.ready() ? 1 : 0; }
//		catch(IOException ex) { return 0; }
//	}
//
//	@Override
//	public void mark(int readLimit) {
//		try {
//			reader.mark(readLimit);
//		}
//		catch(IOException ex) {}
//	}
//
//	@Override
//	public boolean markSupported() {
//		return reader.markSupported();
//	}
//
//	@Override
//	public void reset() throws IOException {
//		reader.reset();
//	}
//
//	@Override
//	public long skip(long n) throws IOException {
//		return reader.skip(n);
//	}
//
//	@Override
//	public void close() throws IOException {
//		reader.close();
//	}
//
//}
