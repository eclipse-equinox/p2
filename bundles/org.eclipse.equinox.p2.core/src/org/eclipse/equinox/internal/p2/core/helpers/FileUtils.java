/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

public class FileUtils {

	/**
	 * Unzip from a File to an output directory.
	 */
	public static File[] unzipFile(File zipFile, File outputDir) throws IOException {
		InputStream in = new FileInputStream(zipFile);
		try {
			return unzipStream(in, zipFile.length(), outputDir, null, null);
		} catch (IOException e) {
			// add the file name to the message
			throw new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipFile, e.getMessage()));
		} finally {
			in.close();
		}
	}

	/**
	 * Unzip from a File to an output directory, with progress indication.
	 * monitor may be null.
	 */
	public static File[] unzipFile(File zipFile, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		InputStream in = new FileInputStream(zipFile);
		try {
			return unzipStream(in, zipFile.length(), outputDir, taskName, monitor);
		} catch (IOException e) {
			// add the file name to the message
			throw new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipFile, e.getMessage()));
		} finally {
			in.close();
		}
	}

	/**
	 * Unzip from a URL to an output directory, with progress indication.
	 * monitor may be null.
	 */
	public static File[] unzipURL(URL zipURL, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		int size = zipURL.openConnection().getContentLength();
		InputStream in = zipURL.openStream();
		try {
			return unzipStream(in, size, outputDir, taskName, monitor);
		} catch (IOException e) {
			// add the URL to the message
			throw new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipURL, e.getMessage()));
		} finally {
			in.close();
		}
	}

	/**
	 * Unzip from an InputStream to an output directory.
	 */
	public static File[] unzipStream(InputStream stream, long size, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		InputStream is = monitor == null ? stream : stream; // new ProgressMonitorInputStream(stream, size, size, taskName, monitor); TODO Commented code
		ZipInputStream in = new ZipInputStream(new BufferedInputStream(is));
		ZipEntry ze = in.getNextEntry();
		if (ze == null) {
			// There must be at least one entry in a zip file.
			// When there isn't getNextEntry returns null.
			in.close();
			throw new IOException(Messages.Util_Invalid_Zip_File_Format);
		}
		ArrayList unzippedFiles = new ArrayList();
		do {
			File outFile = new File(outputDir, ze.getName());
			unzippedFiles.add(outFile);
			if (ze.isDirectory()) {
				outFile.mkdirs();
			} else {
				if (outFile.exists()) {
					outFile.delete();
				} else {
					outFile.getParentFile().mkdirs();
				}
				try {
					copyStream(in, false, new FileOutputStream(outFile), true);
				} catch (FileNotFoundException e) {
					// TEMP: ignore this for now in case we're trying to replace
					// a running eclipse.exe
				}
				outFile.setLastModified(ze.getTime());
			}
			in.closeEntry();
		} while ((ze = in.getNextEntry()) != null);
		in.close();

		return (File[]) unzippedFiles.toArray(new File[unzippedFiles.size()]);
	}

	// Delete empty directories under dir, including dir itself.
	public static void deleteEmptyDirs(File dir) throws IOException {
		File[] files = dir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i += 1) {
				deleteEmptyDirs(files[i]);
			}
			dir.getCanonicalFile().delete();
		}
	}

	// Delete the given file whether it is a file or a directory
	public static void deleteAll(File file) {
		if (!file.exists())
			return;
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null)
				for (int i = 0; i < files.length; i++)
					deleteAll(files[i]);
		}
		file.delete();
	}

	/**
	 * Copy an input stream to an output stream.
	 * Optionally close the streams when done.
	 * Return the number of bytes written.
	 */
	public static int copyStream(InputStream in, boolean closeIn, OutputStream out, boolean closeOut) throws IOException {
		try {
			int written = 0;
			byte[] buffer = new byte[16 * 1024];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
				written += len;
			}
			return written;
		} finally {
			try {
				if (closeIn) {
					in.close();
				}
			} finally {
				if (closeOut) {
					out.close();
				}
			}
		}
	}

	public static void copy(File source, File destination, File root, boolean overwrite) throws IOException {
		File sourceFile = new File(source, root.getPath());
		if (!sourceFile.exists())
			throw new FileNotFoundException("Source: " + sourceFile + " does not exist"); //$NON-NLS-1$//$NON-NLS-2$

		File destinationFile = new File(destination, root.getPath());

		if (destinationFile.exists())
			if (overwrite)
				deleteAll(destinationFile);
			else
				throw new IOException("Destination: " + destinationFile + " already exists"); //$NON-NLS-1$//$NON-NLS-2$
		if (sourceFile.isDirectory()) {
			destinationFile.mkdirs();
			File[] list = sourceFile.listFiles();
			for (int i = 0; i < list.length; i++)
				copy(source, destination, new File(root, list[i].getName()), false);
		} else {
			destinationFile.getParentFile().mkdirs();
			InputStream in = null;
			OutputStream out = null;
			try {
				in = new BufferedInputStream(new FileInputStream(sourceFile));
				out = new BufferedOutputStream(new FileOutputStream(destinationFile));
				copyStream(in, false, out, false);
			} finally {
				try {
					if (in != null)
						in.close();
				} finally {
					if (out != null)
						out.close();
				}
			}
		}
	}

	public static void zip(File[] inclusions, File[] exclusions, File destinationArchive, IPathComputer pathComputer) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destinationArchive));
		HashSet exclusionSet = exclusions == null ? new HashSet() : new HashSet(Arrays.asList(exclusions));
		try {
			for (int i = 0; i < inclusions.length; i++) {
				pathComputer.reset();
				zip(output, inclusions[i], exclusionSet, pathComputer);
			}
		} finally {
			try {
				// Note! This call will fail miserably if no entries were added to the zip!
				// The file is left open after an exception is thrown.
				output.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private static void zip(ZipOutputStream output, File source, Set exclusions, IPathComputer pathComputer) throws IOException {
		if (exclusions.contains(source))
			return;
		if (source.isDirectory()) //if the file path is a URL then isDir and isFile are both false
			zipDir(output, source, exclusions, pathComputer);
		else
			zipFile(output, source, pathComputer);
	}

	public static interface IPathComputer {
		public IPath computePath(File source);

		public void reset();
	}

	/*
	 * Zip the contents of the given directory into the zip file represented by
	 * the given zip stream. Prepend the given prefix to the file paths.
	 */
	private static void zipDir(ZipOutputStream output, File source, Set exclusions, IPathComputer pathComputer) throws IOException {
		File[] files = source.listFiles();
		if (files.length == 0) {
			try {
				ZipEntry dirEntry = new ZipEntry(pathComputer.computePath(source).toString() + "/"); //$NON-NLS-1$
				dirEntry.setTime(source.lastModified());
				output.putNextEntry(dirEntry);
			} catch (ZipException ze) {
				//TODO: something about duplicate entries
			} finally {
				try {
					output.closeEntry();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		for (int i = 0; i < files.length; i++)
			zip(output, files[i], exclusions, pathComputer);
	}

	/*
	 * Add the given file to the zip file represented by the specified stream.
	 * Prepend the given prefix to the path of the file.
	 */
	private static void zipFile(ZipOutputStream output, File source, IPathComputer pathComputer) throws IOException {
		InputStream input = new FileInputStream(source);
		try {
			ZipEntry zipEntry = new ZipEntry(pathComputer.computePath(source).toString());
			zipEntry.setTime(source.lastModified());
			output.putNextEntry(zipEntry);
			copyStream(input, true, output, false);
		} catch (ZipException ze) {
			//TODO: something about duplicate entries, and rethrow other exceptions
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				output.closeEntry();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public static IPathComputer createRootPathComputer(final File root) {
		return new IPathComputer() {
			public IPath computePath(File source) {
				IPath result = new Path(source.getAbsolutePath());
				IPath rootPath = new Path(root.getAbsolutePath());
				result = result.removeFirstSegments(rootPath.matchingFirstSegments(result));
				return result.setDevice(null);
			}

			public void reset() {
			}
		};
	}

	public static IPathComputer createDynamicPathComputer(final int segmentsToKeep) {
		return new IPathComputer() {
			IPathComputer computer = null;

			public IPath computePath(File source) {
				if (computer == null) {
					IPath sourcePath = new Path(source.getAbsolutePath());
					sourcePath = sourcePath.removeLastSegments(segmentsToKeep);
					computer = createRootPathComputer(sourcePath.toFile());
				}
				return computer.computePath(source);
			}

			public void reset() {
				computer = null;
			}
		};
	}

	public static IPathComputer createParentPrefixComputer(final int segmentsToKeep) {
		return new IPathComputer() {
			public IPath computePath(File source) {
				IPath sourcePath = new Path(source.getAbsolutePath());
				sourcePath = sourcePath.removeFirstSegments(Math.max(0, sourcePath.segmentCount() - segmentsToKeep));
				return sourcePath.setDevice(null);
			}

			public void reset() {
			}
		};
	}
}
