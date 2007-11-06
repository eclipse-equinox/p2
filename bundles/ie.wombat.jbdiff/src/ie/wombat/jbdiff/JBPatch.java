/*
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package ie.wombat.jbdiff;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

/**
 * Java Binary patcher (based on bspatch by Colin Percival)
 * 
 * @author Joe Desbonnet, joe@galway.net
 */
public class JBPatch {

	// JBPatch extensions by Stefan.Liebig@compeople.de:
	//
	// - uses an extended version of the org.apache.tools.bzip2 compressor to
	// compress all of the blocks (ctrl,diff,extra).
	// - added an interface that allows using of JBPatch with streams and byte
	// arrays

	private static final String VERSION = "jbdiff-0.1.0";

	/**
	 * Run JBPatch from the command line. Params: oldfile newfile patchfile.
	 * newfile will be created.
	 * 
	 * @param arg
	 * @throws IOException
	 */
	public static void main(String[] arg) throws IOException {

		if (arg.length != 3) {
			System.err
					.println("usage example: java -Xmx200m ie.wombat.jbdiff.JBPatch oldfile newfile patchfile");
		}

		File oldFile = new File(arg[0]);
		File newFile = new File(arg[1]);
		File diffFile = new File(arg[2]);

		bspatch(oldFile, newFile, diffFile);
	}

	/**
	 * @param oldFile
	 * @param newFile
	 * @param diffFile
	 * @throws IOException
	 */
	public static void bspatch(File oldFile, File newFile, File diffFile)
			throws IOException {
		InputStream oldInputStream = new BufferedInputStream(
				new FileInputStream(oldFile));
		byte[] diffBytes = new byte[(int) diffFile.length()];
		InputStream diffInputStream = new FileInputStream(diffFile);
		Util.readFromStream(diffInputStream, diffBytes, 0, diffBytes.length);

		byte[] newBytes = bspatch(oldInputStream, (int) oldFile.length(),
				diffBytes);

		OutputStream newOutputStream = new FileOutputStream(newFile);
		newOutputStream.write(newBytes);
		newOutputStream.close();
	}

	/**
	 * @param oldInputStream
	 * @param diffInputStream
	 * @return
	 */
	public static byte[] bspatch(InputStream oldInputStream, int oldsize,
			byte[] diffBytes) throws IOException {
		/*
		 * Read in old file (file to be patched) to oldBuf
		 */
		// int oldsize = (int) oldFile.length();
		// byte[] oldBuf = new byte[oldsize + 1];
		byte[] oldBuf = new byte[oldsize];
		// InputStream oldIn = new FileInputStream( oldFile );
		Util.readFromStream(oldInputStream, oldBuf, 0, oldsize);
		oldInputStream.close();
		// oldIn.close();

		return JBPatch.bspatch(oldBuf, oldsize, diffBytes);
	}

	/**
	 * @param oldBuf
	 * @param oldsize
	 * @param diffBytes
	 * @return
	 * @throws IOException
	 */
	public static byte[] bspatch(byte[] oldBuf, int oldsize, byte[] diffBytes)
			throws IOException {
		return bspatch(oldBuf, oldsize, diffBytes, diffBytes.length);
	}

	/**
	 * @param oldBuf
	 * @param oldsize
	 * @param diffBuf
	 * @param diffSize
	 * @return
	 * @throws IOException
	 */
	public static byte[] bspatch(byte[] oldBuf, int oldsize, byte[] diffBuf,
			int diffSize) throws IOException {

		DataInputStream diffIn = new DataInputStream(new ByteArrayInputStream(
				diffBuf, 0, diffSize));

		// skip headerMagic at header offset 0 (length 8 bytes)
		diffIn.skip(8);

		// ctrlBlockLen after bzip2 compression at heater offset 8 (length 8
		// bytes)
		long ctrlBlockLen = diffIn.readLong();

		// diffBlockLen after bzip2 compression at header offset 16 (length 8
		// bytes)
		long diffBlockLen = diffIn.readLong();

		// size of new file at header offset 24 (length 8 bytes)
		int newsize = (int) diffIn.readLong();

		// System.err.println( "newsize=" + newsize );
		// System.err.println( "ctrlBlockLen=" + ctrlBlockLen );
		// System.err.println( "diffBlockLen=" + diffBlockLen );
		// System.err.println( "newsize=" + newsize );

		InputStream in;
		in = new ByteArrayInputStream(diffBuf, 0, diffSize);
		in.skip(Util.HEADER_SIZE);
		DataInputStream ctrlBlockIn = new DataInputStream(
				new CBZip2InputStream(in));

		in = new ByteArrayInputStream(diffBuf, 0, diffSize);
		in.skip(ctrlBlockLen + Util.HEADER_SIZE);
		InputStream diffBlockIn = new CBZip2InputStream(in);

		in = new ByteArrayInputStream(diffBuf, 0, diffSize);
		in.skip(diffBlockLen + ctrlBlockLen + Util.HEADER_SIZE);
		InputStream extraBlockIn = new CBZip2InputStream(in);

		// byte[] newBuf = new byte[newsize + 1];
		byte[] newBuf = new byte[newsize];

		int oldpos = 0;
		int newpos = 0;
		int[] ctrl = new int[3];
		// int nbytes;
		while (newpos < newsize) {

			for (int i = 0; i <= 2; i++) {
				// ctrl[i] = diffIn.readInt();
				ctrl[i] = ctrlBlockIn.readInt();
				// System.err.println (" ctrl[" + i + "]=" + ctrl[i]);
			}

			if (newpos + ctrl[0] > newsize) {
				throw new IOException("Corrupt patch.");
			}

			/*
			 * Read ctrl[0] bytes from diffBlock stream
			 */

			Util.readFromStream(diffBlockIn, newBuf, newpos, ctrl[0]);

			for (int i = 0; i < ctrl[0]; i++) {
				if ((oldpos + i >= 0) && (oldpos + i < oldsize)) {
					newBuf[newpos + i] += oldBuf[oldpos + i];
				}
			}

			newpos += ctrl[0];
			oldpos += ctrl[0];

			if (newpos + ctrl[1] > newsize) {
				throw new IOException("Corrupt patch.");
			}

			Util.readFromStream(extraBlockIn, newBuf, newpos, ctrl[1]);

			newpos += ctrl[1];
			oldpos += ctrl[2];
		}

		// TODO: Check if at end of ctrlIn
		// TODO: Check if at the end of diffIn
		// TODO: Check if at the end of extraIn

		// This check is not needed since the byte array has been allocated with
		// this constraint!
		// if ( newBuf.length - 1 != newsize ) {
		// throw new IOException( "Corrupt patch." );
		// }

		ctrlBlockIn.close();
		diffBlockIn.close();
		extraBlockIn.close();
		diffIn.close();

		return newBuf;
		// OutputStream out = new FileOutputStream( newFile );
		// out.write( newBuf, 0, newBuf.length - 1 );
		// out.close();
	}
}
