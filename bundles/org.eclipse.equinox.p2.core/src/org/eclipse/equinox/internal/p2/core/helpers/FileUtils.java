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
import java.util.*;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

public class FileUtils {

	private static File[] untarFile(File source, File outputDir) throws IOException, TarException {
		TarFile tarFile = new TarFile(source);
		List untarredFiles = new ArrayList();
		for (Enumeration e = tarFile.entries(); e.hasMoreElements();) {
			TarEntry entry = (TarEntry) e.nextElement();
			InputStream input = tarFile.getInputStream(entry);
			try {
				File outFile = new File(outputDir, entry.getName());
				untarredFiles.add(outFile);
				if (entry.getFileType() == TarEntry.DIRECTORY) {
					outFile.mkdirs();
				} else {
					if (outFile.exists())
						outFile.delete();
					else
						outFile.getParentFile().mkdirs();
					try {
						copyStream(input, false, new FileOutputStream(outFile), true);
					} catch (FileNotFoundException e1) {
						// TEMP: ignore this for now in case we're trying to replace
						// a running eclipse.exe
					}
					outFile.setLastModified(entry.getTime());
				}
			} finally {
				input.close();
			}
		}
		return (File[]) untarredFiles.toArray(new File[untarredFiles.size()]);
	}

	/**
	 * Unzip from a File to an output directory.
	 */
	public static File[] unzipFile(File zipFile, File outputDir) throws IOException {
		// check to see if we have a tar'd and gz'd file
		if (zipFile.getName().toLowerCase().endsWith(".tar.gz")) {
			try {
				return untarFile(zipFile, outputDir);
			} catch (TarException e) {
				throw new IOException(e.getMessage());
			}
		}
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

	/**
	 * Creates a zip archive at the given destination that contains all of the given inclusions
	 * except for the given exclusions.  Inclusions and exclusions can be phrased as files or folders.
	 * Including a folder implies that all files and folders under the folder 
	 * should be considered for inclusion. Excluding a folder implies that all files and folders
	 * under that folder will be excluded. Inclusions with paths deeper than an exclusion folder
	 * are filtered out and do not end up in the resultant archive.
	 * <p>
	 * All entries in the archive are computed using the given path computer.  the path computer
	 * is reset between every explicit entry in the inclusions list.
	 * </p>
	 * @param inclusions the set of files and folders to be considered for inclusion in the result
	 * @param exclusions the set of files and folders to be excluded from the result.  May be <code>null</code>.
	 * @param destinationArchive the location of the resultant archive
	 * @param pathComputer the path computer used to create the path of the files in the result
	 * @throws IOException if there is an IO issue during this operation.
	 */
	public static void zip(File[] inclusions, File[] exclusions, File destinationArchive, IPathComputer pathComputer) throws IOException {
		FileOutputStream fileOutput = new FileOutputStream(destinationArchive);
		ZipOutputStream output = new ZipOutputStream(fileOutput);
		HashSet exclusionSet = exclusions == null ? new HashSet() : new HashSet(Arrays.asList(exclusions));
		try {
			for (int i = 0; i < inclusions.length; i++) {
				pathComputer.reset();
				zip(output, inclusions[i], exclusionSet, pathComputer);
			}
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				// closing a zip file with no entries apparently fails on some JREs.
				// if we failed closing the zip, try closing the raw file.  
				try {
					fileOutput.close();
				} catch (IOException e2) {
					// give up.
				}
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

	/**
	 * Path computers are used to transform a given File path into a path suitable for use
	 * as the to identify that file in an archive file or copy.
	 */
	public static interface IPathComputer {
		/**
		 * Returns the path representing the given file.  Often this trims or otherwise
		 * transforms the segments of the source file path.
		 * @param source the file path to be transformed
		 * @return the transformed path
		 */
		public IPath computePath(File source);

		/**
		 * Resets this path computer. Path computers can accumulate state or other information
		 * for use in computing subsequent paths.  Resetting a computer causes it to flush that
		 * state and start afresh.  The exact semantics of resetting depends on the nature of the
		 * computer itself.
		 */
		public void reset();
	}

	/**
	 * Creates a path computer that trims all paths according to the given root path.
	 * Paths that have no matching segments are returned unchanged.
	 * @param root the base path to use for trimming
	 * @return a path computer that trims according to the given root
	 */
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

	/**
	 * Creates a path computer that is a cross between the root and parent computers.
	 * When this computer is reset, the first path seen is considered a new root.  That path
	 * is trimmed by the given number of segments and then used as in the same way as the 
	 * root path computer.  Every time this computer is reset, a new root is computed.
	 * <p>
	 * This is useful when handling several sets of disjoint files but for each set you want
	 * to have a common root.  Rather than having to compute the roots ahead of time and 
	 * then manage their relationships, you can simply reset the computer between groups.
	 * </p><p>	
	 * For example, say you have the a list of folders { /a/b/c/eclipse/plugins/, /x/y/eclipse/features/}
	 * and want to end up with a zip containing plugins and features folders.  Using a dynamic
	 * path computer and keeping 1 segment allows this to be done simply by resetting the computer
	 * between elements of the top level list.
	 * </p>
	 * @param segmentsToKeep the number of segments of encountered paths to keep
	 * relative to the dynamically computed roots.
	 * @return a path computer that trims but keeps the given number of segments  relative 
	 * to the dynamically computed roots.
	 */
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

	/**
	 * Creates a path computer that retains the given number of trailing segments.
	 * @param segmentsToKeep number of segments to keep
	 * @return a path computer that retains the given number of trailing segments.
	 */
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
