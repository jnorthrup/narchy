package jcog.io.tar;

import java.io.*;

class TARTestUtils {

	private static final int BUFFER = 2048;

	public static File writeStringToFile(String string, File file) throws IOException {
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
			writer.write(string);
		}

		return file;
	}

	public static String readFile(File file) throws IOException {
		final char[] buffer = new char[BUFFER];
		final StringBuilder out = new StringBuilder();
		try (final Reader in = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
			return readFromStream(buffer, out, in);
		}
	}

	private static String readFromStream(final char[] buffer, final StringBuilder out, final Reader in) throws IOException {
		while (true) {
			final int read = in.read(buffer, 0, BUFFER);
			if (read <= 0) {
				break;
			}
			out.append(buffer, 0, read);
		}
		return out.toString();
	}
}
