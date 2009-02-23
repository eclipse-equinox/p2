/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.comparator;

import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.osgi.util.NLS;

public class JarComparator implements IArtifactComparator {

	private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
	private static final String CLASS_EXTENSION = ".class"; //$NON-NLS-1$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String PLUGIN_ID = "org.eclipse.equinox.p2.repository.tools"; //$NON-NLS-1$
	private static final String DESTINATION_ARTIFACT_PREFIX = "destinationartifact"; //$NON-NLS-1$
	private static final String SUFFIX_JAR = ".jar"; //$NON-NLS-1$
	private static final String SOURCE_ARTIFACT_PREFIX = "sourceartifact"; //$NON-NLS-1$
	private static final String OSGI_BUNDLE_CLASSIFIER = "osgi.bundle"; //$NON-NLS-1$

	public IStatus compare(IArtifactRepository source, IArtifactDescriptor sourceDescriptor, IArtifactRepository destination, IArtifactDescriptor destinationDescriptor) {
		String classifier = sourceDescriptor.getArtifactKey().getClassifier();
		if (!OSGI_BUNDLE_CLASSIFIER.equals(classifier)) {
			return Status.OK_STATUS;
		}
		classifier = destinationDescriptor.getArtifactKey().getClassifier();
		if (!OSGI_BUNDLE_CLASSIFIER.equals(classifier)) {
			return Status.OK_STATUS;
		}
		File firstTempFile = null;
		BufferedOutputStream stream = null;
		try {
			firstTempFile = File.createTempFile(SOURCE_ARTIFACT_PREFIX, SUFFIX_JAR);
			stream = new BufferedOutputStream(new FileOutputStream(firstTempFile));
			IStatus status = source.getArtifact(sourceDescriptor, stream, new NullProgressMonitor());
			if (!status.isOK()) {
				return status;
			}
			stream.flush();
		} catch (FileNotFoundException e) {
			return newErrorStatus("FileNotFoundException", e); //$NON-NLS-1$
		} catch (IOException e) {
			return newErrorStatus("IOException", e); //$NON-NLS-1$
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}

		File secondTempFile = null;
		stream = null;
		try {
			secondTempFile = File.createTempFile(DESTINATION_ARTIFACT_PREFIX, SUFFIX_JAR);
			stream = new BufferedOutputStream(new FileOutputStream(secondTempFile));
			IStatus status = destination.getArtifact(destinationDescriptor, stream, null);
			if (!status.isOK()) {
				return status;
			}
			stream.flush();
		} catch (FileNotFoundException e) {
			return newErrorStatus("FileNotFoundException", e); //$NON-NLS-1$
		} catch (IOException e) {
			return newErrorStatus("IOException", e); //$NON-NLS-1$
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}

		try {
			return compare(firstTempFile, secondTempFile);
		} finally {
			if (firstTempFile != null) {
				firstTempFile.delete();
			}
			secondTempFile.delete();
		}
	}

	public IStatus compare(File sourceFile, File destinationFile) {
		try {
			ZipFile firstFile = new ZipFile(sourceFile);
			ZipFile secondFile = new ZipFile(destinationFile);
			final int firstFileSize = firstFile.size();
			final int secondFileSize = secondFile.size();
			if (firstFileSize != secondFileSize) {
				return newErrorStatus(NLS.bind(Messages.differentNumberOfEntries, new String[] {sourceFile.getName(), Integer.toString(firstFileSize), destinationFile.getName(), Integer.toString(secondFileSize)}));
			}
			for (Enumeration enumeration = firstFile.entries(); enumeration.hasMoreElements();) {
				ZipEntry entry = (ZipEntry) enumeration.nextElement();
				String entryName = entry.getName();
				final ZipEntry entry2 = secondFile.getEntry(entryName);
				if (!entry.isDirectory() && entry2 != null) {
					Disassembler disassembler = new Disassembler();
					byte[] firstEntryClassFileBytes = Utility.getZipEntryByteContent(entry, firstFile);
					byte[] secondEntryClassFileBytes = Utility.getZipEntryByteContent(entry2, secondFile);
					String lowerCase = entryName.toLowerCase();
					if (lowerCase.endsWith(CLASS_EXTENSION)) {
						String contentsFile1 = null;
						String contentsFile2 = null;
						try {
							contentsFile1 = disassembler.disassemble(firstEntryClassFileBytes, LINE_SEPARATOR, Disassembler.DETAILED | Disassembler.COMPACT);
							contentsFile2 = disassembler.disassemble(secondEntryClassFileBytes, LINE_SEPARATOR, Disassembler.DETAILED | Disassembler.COMPACT);
						} catch (ClassFormatException e) {
							return newErrorStatus(NLS.bind(Messages.differentEntry, entryName, sourceFile.getAbsolutePath()), e);
						}
						if (!contentsFile1.equals(contentsFile2)) {
							return newErrorStatus(NLS.bind(Messages.differentEntry, entryName, sourceFile.getAbsolutePath()));
						}
					} else if (lowerCase.endsWith(JAR_EXTENSION)) {
						File firstTempFile = null;
						BufferedOutputStream stream = null;
						try {
							firstTempFile = File.createTempFile(SOURCE_ARTIFACT_PREFIX + normalize(entryName), SUFFIX_JAR);
							stream = new BufferedOutputStream(new FileOutputStream(firstTempFile));
							stream.write(firstEntryClassFileBytes);
							stream.flush();
						} catch (FileNotFoundException e) {
							return newErrorStatus(NLS.bind(Messages.filenotfoundexception, entryName, sourceFile.getAbsolutePath()), e);
						} catch (IOException e) {
							return newErrorStatus(NLS.bind(Messages.ioexceptioninentry, entryName, sourceFile.getAbsolutePath()), e);
						} finally {
							try {
								if (stream != null) {
									stream.close();
								}
							} catch (IOException e) {
								// ignore
							}
						}
						File secondTempFile = null;
						stream = null;
						try {
							secondTempFile = File.createTempFile(DESTINATION_ARTIFACT_PREFIX + normalize(entryName), SUFFIX_JAR);
							stream = new BufferedOutputStream(new FileOutputStream(secondTempFile));
							stream.write(secondEntryClassFileBytes);
							stream.flush();
						} catch (FileNotFoundException e) {
							return newErrorStatus(NLS.bind(Messages.filenotfoundexception, entryName, sourceFile.getAbsolutePath()), e);
						} catch (IOException e) {
							return newErrorStatus(NLS.bind(Messages.ioexceptioninentry, entryName, sourceFile.getAbsolutePath()), e);
						} finally {
							try {
								if (stream != null) {
									stream.close();
								}
							} catch (IOException e) {
								// ignore
							}
						}

						try {
							IStatus status = compare(firstTempFile, secondTempFile);
							if (!status.isOK()) {
								return status;
							}
						} finally {
							if (firstTempFile != null) {
								firstTempFile.delete();
							}
							secondTempFile.delete();
						}
					} else if (!Arrays.equals(firstEntryClassFileBytes, secondEntryClassFileBytes)) {
						// do a binary compare byte per byte
						return newErrorStatus(NLS.bind(Messages.differentEntry, entryName, sourceFile.getAbsolutePath()));
					}
				} else if (!entry.isDirectory()) {
					// missing entry
					return newErrorStatus(NLS.bind(Messages.missingEntry, entryName, sourceFile.getAbsolutePath()));
				}
			}
			firstFile.close();
			secondFile.close();
		} catch (IOException e) {
			// missing entry
			return newErrorStatus(NLS.bind(Messages.ioexception, new String[] {sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath()}), e);
		}
		return Status.OK_STATUS;
	}

	private String normalize(String entryName) {
		StringBuffer buffer = new StringBuffer();
		char[] chars = entryName.toCharArray();
		for (int i = 0, max = chars.length; i < max; i++) {
			char currentChar = chars[i];
			if (!Character.isJavaIdentifierPart(currentChar)) {
				buffer.append('_');
			} else {
				buffer.append(currentChar);
			}
		}
		return String.valueOf(buffer);
	}

	private IStatus newErrorStatus(String message, Exception e) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message, e);
	}

	private IStatus newErrorStatus(String message) {
		return newErrorStatus(message, null);
	}
}
