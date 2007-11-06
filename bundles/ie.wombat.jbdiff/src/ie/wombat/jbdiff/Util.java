/*
 * Created on Feb 28, 2005
 */
package ie.wombat.jbdiff;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author
 * @author Joe Desbonnet, joe@galway.net
 * 
 */
public class Util {

	// JBDiff extensions by Stefan.Liebig@compeople.de:
	//
	// - introduced a HEADER_SIZE constant here

	/**
	 * Length of the diff file header.
	 */
	public static final int HEADER_SIZE = 32;

	/**
	 * Equiv of C library memcmp().
	 * 
	 * @param s1
	 * @param s1offset
	 * @param s2
	 * @param n
	 * @return
	 */
	/*
	 * public final static int memcmp(byte[] s1, int s1offset, byte[] s2, int
	 * s2offset, int n) {
	 * 
	 * if ((s1offset + n) > s1.length) { n = s1.length - s1offset; } if
	 * ((s2offset + n) > s2.length) { n = s2.length - s2offset; } for (int i =
	 * 0; i < n; i++) { if (s1[i + s1offset] != s2[i + s2offset]) { return s1[i +
	 * s1offset] < s2[i + s2offset] ? -1 : 1; } }
	 * 
	 * return 0; }
	 */

	/**
	 * Equiv of C library memcmp().
	 * 
	 * @param s1
	 * @param s1offset
	 * @param s2
	 * @param n
	 * @return
	 */
	public final static int memcmp(byte[] s1, int s1Size, int s1offset,
			byte[] s2, int s2Size, int s2offset) {

		int n = s1Size - s1offset;

		if (n > (s2Size - s2offset)) {
			n = s2Size - s2offset;
		}
		for (int i = 0; i < n; i++) {
			if (s1[i + s1offset] != s2[i + s2offset]) {
				return s1[i + s1offset] < s2[i + s2offset] ? -1 : 1;
			}
		}

		return 0;
		// int n = s1.length - s1offset;
		//
		// if (n > (s2.length - s2offset)) {
		// n = s2.length - s2offset;
		// }
		// for (int i = 0; i < n; i++) {
		// if (s1[i + s1offset] != s2[i + s2offset]) {
		// return s1[i + s1offset] < s2[i + s2offset] ? -1 : 1;
		// }
		// }
		//
		// return 0;
	}

	/**
	 * Read from input stream and fill the given buffer from the given offset up
	 * to length len.
	 * 
	 * @param in
	 * @param buf
	 * @param offset
	 * @param len
	 * @throws IOException
	 */
	public static final void readFromStream(InputStream in, byte[] buf,
			int offset, int len) throws IOException {

		int totalBytesRead = 0;
		while (totalBytesRead < len) {
			int bytesRead = in.read(buf, offset + totalBytesRead, len
					- totalBytesRead);
			if (bytesRead < 0) {
				throw new IOException(
						"Could not read expected number of bytes.");
			}
			totalBytesRead += bytesRead;
		}
	}

}
