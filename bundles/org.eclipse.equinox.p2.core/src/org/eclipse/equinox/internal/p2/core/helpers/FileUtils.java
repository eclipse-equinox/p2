/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

public class FileUtils {

	private static File[] untarFile(File source, File outputDir) throws IOException, TarException {
		List<File> untarredFiles = new ArrayList<>();
		try (TarFile tarFile = new TarFile(source)) {
			for (TarEntry entry : tarFile.entries()) {
				try (InputStream input = tarFile.getInputStream(entry)) {
					File outFile = createSubPathFile(outputDir, entry.getName());
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
				}
			}
		}
		return untarredFiles.toArray(new File[untarredFiles.size()]);
	}

	/**
	 * Unzip from a File to an output directory.
	 */
	public static File[] unzipFile(File zipFile, File outputDir) throws IOException {
		// check to see if we have a tar'd and gz'd file
		if (zipFile.getName().toLowerCase().endsWith(".tar.gz")) { //$NON-NLS-1$
			try {
				return untarFile(zipFile, outputDir);
			} catch (TarException e) {
				IOException ioException = new IOException(e.getMessage());
				ioException.initCause(e);
				throw ioException;
			}
		}
		try (InputStream in = new FileInputStream(zipFile)) {
			return unzipStream(in, zipFile.length(), outputDir, null, null);
		} catch (IOException e) {
			// add the file name to the message
			IOException ioException = new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipFile, e.getMessage()));
			ioException.initCause(e);
			throw ioException;
		}
	}

	/**
	 * Unzip from a File to an output directory, with progress indication.
	 * monitor may be null.
	 */
	public static File[] unzipFile(File zipFile, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		try (InputStream in = new FileInputStream(zipFile)) {
			return unzipStream(in, zipFile.length(), outputDir, taskName, monitor);
		} catch (IOException e) {
			// add the file name to the message
			IOException ioException = new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipFile, e.getMessage()));
			ioException.initCause(e);
			throw ioException;
		}
	}

	/**
	 * Unzip from an InputStream to an output directory.
	 */
	public static File[] unzipStream(InputStream stream, long size, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		InputStream is = monitor == null ? stream : stream; // new ProgressMonitorInputStream(stream, size, size, taskName, monitor); TODO Commented code
		ArrayList<File> unzippedFiles = new ArrayList<>();
		try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(is))) {
			ZipEntry ze = in.getNextEntry();
			if (ze == null) {
				// There must be at least one entry in a zip file.
				// When there isn't getNextEntry returns null.
				in.close();
				throw new IOException(Messages.Util_Invalid_Zip_File_Format);
			}
			do {
				File outFile = createSubPathFile(outputDir, ze.getName());
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
		}

		return unzippedFiles.toArray(new File[unzippedFiles.size()]);
	}

	private static final boolean IS_WINDOWS = File.separatorChar == '\\';

	// reserved names according to
	// https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
	private static final Set<String> RESERVED_NAMES = new HashSet<>(Arrays.asList("aux", "com1", "com2", "com3", "com4", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"com5", "com6", "com7", "com8", "com9", "con", "lpt1", "lpt2", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
			"lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "nul", "prn")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

	/** Tests whether the filename can escape path into special device **/
	public static boolean isReservedFileName(File file) {
		// Directory names are not checked here because illegal directory names will be
		// handled by OS.
		if (!IS_WINDOWS) { // only windows has special file names which can escape any path
			return false;
		}
		String fileName = file.getName();
		// Illegal characters are not checked here because they are check by both JDK
		// and OS. This is only a check against technical allowed but unwanted device
		// names.
		int dot = fileName.indexOf('.');
		// on windows, filename suffixes are not relevant to name validity
		String basename = dot == -1 ? fileName : fileName.substring(0, dot);
		return RESERVED_NAMES.contains(basename.toLowerCase());
	}

	private static File createSubPathFile(File root, String subPath) throws IOException {
		File result = new File(root, subPath);
		if (subPath.contains("..")) { //$NON-NLS-1$
			// do the extra check to make sure the path did not escape the root path
			java.nio.file.Path resultNormalized = result.toPath().normalize();
			java.nio.file.Path rootBaseNormalized = root.toPath().normalize();
			if (!resultNormalized.startsWith(rootBaseNormalized)) {
				throw new IOException("Invalid path: " + subPath); //$NON-NLS-1$
			}
		}
		// Additional check if it is a special device instead of a regular file.
		if (isReservedFileName(result)) {
			throw new IOException("Invalid filename: " + subPath); //$NON-NLS-1$
		}
		return result;
	}

	// Delete empty directories under dir, including dir itself.
	public static void deleteEmptyDirs(File dir) throws IOException {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				deleteEmptyDirs(file);
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
				for (File f : files) {
					deleteAll(f);
				}
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
			for (File file : list) {
				copy(source, destination, new File(root, file.getName()), false);
			}
		} else {
			destinationFile.getParentFile().mkdirs();
			try (InputStream in = new BufferedInputStream(new FileInputStream(sourceFile)); OutputStream out = new BufferedOutputStream(new FileOutputStream(destinationFile));) {
				copyStream(in, false, out, false);
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
		try (FileOutputStream fileOutput = new FileOutputStream(destinationArchive); ZipOutputStream output = new ZipOutputStream(fileOutput)) {
			HashSet<File> exclusionSet = exclusions == null ? new HashSet<>() : new HashSet<>(Arrays.asList(exclusions));
			HashSet<IPath> directoryEntries = new HashSet<>();
			for (File inclusion : inclusions) {
				pathComputer.reset();
				zip(output, inclusion, exclusionSet, pathComputer, directoryEntries);
			}
		}
	}

	/**
	 * Writes the given file or folder to the given ZipOutputStream.  The stream is not closed, we recurse into folders
	 * @param output - the ZipOutputStream to write into
	 * @param source - the file or folder to zip
	 * @param exclusions - set of files or folders to exclude
	 * @param pathComputer - computer used to create the path of the files in the result.
	 * @throws IOException
	 */
	public static void zip(ZipOutputStream output, File source, Set<File> exclusions, IPathComputer pathComputer) throws IOException {
		zip(output, source, exclusions, pathComputer, new HashSet<>());
	}

	public static void zip(ZipOutputStream output, File source, Set<File> exclusions, IPathComputer pathComputer, Set<IPath> directoryEntries) throws IOException {
		if (exclusions.contains(source))
			return;
		if (source.isDirectory()) //if the file path is a URL then isDir and isFile are both false
			zipDir(output, source, exclusions, pathComputer, directoryEntries);
		else
			zipFile(output, source, pathComputer, directoryEntries);
	}

	private static void zipDirectoryEntry(ZipOutputStream output, IPath entry, long time, Set<IPath> directoryEntries) throws IOException {
		entry = entry.addTrailingSeparator();
		if (!directoryEntries.contains(entry)) {
			//make sure parent entries are in the zip
			if (entry.segmentCount() > 1)
				zipDirectoryEntry(output, entry.removeLastSegments(1), time, directoryEntries);

			try {
				ZipEntry dirEntry = new ZipEntry(entry.toString());
				dirEntry.setTime(time);
				output.putNextEntry(dirEntry);
				directoryEntries.add(entry);
			} catch (ZipException ze) {
				//duplicate entries shouldn't happen because we checked the set
			} finally {
				try {
					output.closeEntry();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/*
	 * Zip the contents of the given directory into the zip file represented by
	 * the given zip stream. Prepend the given prefix to the file paths.
	 */
	private static void zipDir(ZipOutputStream output, File source, Set<File> exclusions, IPathComputer pathComputer, Set<IPath> directoryEntries) throws IOException {
		File[] files = source.listFiles();
		if (files.length == 0) {
			zipDirectoryEntry(output, pathComputer.computePath(source), source.lastModified(), directoryEntries);
		}

		// Different OSs return files in a different order.  This affects the creation
		// the dynamic path computer.  To address this, we sort the files such that
		// those with deeper paths appear later, and files are always before directories
		// foo/bar.txt
		// foo/something/bar2.txt
		// foo/something/else/bar3.txt
		Arrays.sort(files, (arg0, arg1) -> {
			IPath a = IPath.fromOSString(arg0.getAbsolutePath());
			IPath b = IPath.fromOSString(arg1.getAbsolutePath());
			if (a.segmentCount() == b.segmentCount()) {
				if (arg0.isDirectory() && arg1.isFile())
					return 1;
				else if (arg0.isDirectory() && arg1.isDirectory())
					return 0;
				else if (arg0.isFile() && arg1.isDirectory())
					return -1;
				else
					return 0;
			}
			return a.segmentCount() - b.segmentCount();
		});

		for (File file : files) {
			zip(output, file, exclusions, pathComputer, directoryEntries);
		}
	}

	/*
	 * Add the given file to the zip file represented by the specified stream.
	 * Prepend the given prefix to the path of the file.
	 */
	private static void zipFile(ZipOutputStream output, File source, IPathComputer pathComputer, Set<IPath> directoryEntries) throws IOException {
		boolean isManifest = false; //manifest files are special
		try (InputStream input = new BufferedInputStream(new FileInputStream(source))) {
			IPath entryPath = pathComputer.computePath(source);
			if (entryPath.isAbsolute())
				throw new IOException(Messages.Util_Absolute_Entry);
			if (entryPath.segmentCount() == 0)
				throw new IOException(Messages.Util_Empty_Zip_Entry);

			//make sure parent directory entries are in the zip
			if (entryPath.segmentCount() > 1) {
				//manifest files should be first, add their directory entry afterwards
				isManifest = JarFile.MANIFEST_NAME.equals(entryPath.toString());
				if (!isManifest)
					zipDirectoryEntry(output, entryPath.removeLastSegments(1), source.lastModified(), directoryEntries);
			}

			ZipEntry zipEntry = new ZipEntry(entryPath.toString());
			zipEntry.setTime(source.lastModified());
			output.putNextEntry(zipEntry);
			copyStream(input, true, output, false);
		} catch (ZipException ze) {
			//TODO: something about duplicate entries, and rethrow other exceptions
		} finally {
			try {
				output.closeEntry();
			} catch (IOException e) {
				// ignore
			}
		}

		if (isManifest) {
			zipDirectoryEntry(output, IPath.fromOSString("META-INF"), source.lastModified(), directoryEntries); //$NON-NLS-1$
		}
	}

	/**
	 * Path computers are used to transform a given File path into a path suitable for use
	 * as the to identify that file in an archive file or copy.
	 */
	public interface IPathComputer {
		/**
		 * Returns the path representing the given file.  Often this trims or otherwise
		 * transforms the segments of the source file path.
		 * @param source the file path to be transformed
		 * @return the transformed path
		 */
		IPath computePath(File source);

		/**
		 * Resets this path computer. Path computers can accumulate state or other information
		 * for use in computing subsequent paths.  Resetting a computer causes it to flush that
		 * state and start afresh.  The exact semantics of resetting depends on the nature of the
		 * computer itself.
		 */
		void reset();
	}

	/**
	 * Creates a path computer that trims all paths according to the given root path.
	 * Paths that have no matching segments are returned unchanged.
	 * @param root the base path to use for trimming
	 * @return a path computer that trims according to the given root
	 */
	public static IPathComputer createRootPathComputer(final File root) {
		return new IPathComputer() {
			@Override
			public IPath computePath(File source) {
				IPath result = IPath.fromOSString(source.getAbsolutePath());
				IPath rootPath = IPath.fromOSString(root.getAbsolutePath());
				result = result.removeFirstSegments(rootPath.matchingFirstSegments(result));
				return result.setDevice(null);
			}

			@Override
			public void reset() {
				//nothing
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

			@Override
			public IPath computePath(File source) {
				if (computer == null) {
					IPath sourcePath = IPath.fromOSString(source.getAbsolutePath());
					sourcePath = sourcePath.removeLastSegments(segmentsToKeep);
					computer = createRootPathComputer(sourcePath.toFile());
				}
				return computer.computePath(source);
			}

			@Override
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
			@Override
			public IPath computePath(File source) {
				IPath sourcePath = IPath.fromOSString(source.getAbsolutePath());
				sourcePath = sourcePath.removeFirstSegments(Math.max(0, sourcePath.segmentCount() - segmentsToKeep));
				return sourcePath.setDevice(null);
			}

			@Override
			public void reset() {
				//nothing
			}
		};
	}
}
