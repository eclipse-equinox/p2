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
import java.util.*;
import java.util.jar.*;
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

	private static final String META_INF = "meta-inf/"; //$NON-NLS-1$
	private static final String DSA_EXT = ".dsa"; //$NON-NLS-1$
	private static final String RSA_EXT = ".rsa"; //$NON-NLS-1$
	private static final String SF_EXT = ".sf"; //$NON-NLS-1$

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

	private boolean isSigningEntry(String entry) {
		return (entry.startsWith(META_INF) && (entry.endsWith(SF_EXT) || entry.endsWith(RSA_EXT) || entry.endsWith(DSA_EXT)));
	}

	public IStatus compare(File sourceFile, File destinationFile) {
		ZipFile firstFile = null;
		ZipFile secondFile = null;
		try {
			firstFile = new ZipFile(sourceFile);
			secondFile = new ZipFile(destinationFile);
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
					String lowerCase = entryName.toLowerCase();
					if (isSigningEntry(lowerCase)) {
						continue;
					}

					Disassembler disassembler = new Disassembler();
					byte[] firstEntryClassFileBytes = Utility.getZipEntryByteContent(entry, firstFile);
					byte[] secondEntryClassFileBytes = Utility.getZipEntryByteContent(entry2, secondFile);
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
							Utility.close(stream);
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
							Utility.close(stream);
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
					} else if (entryName.equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
						// MANIFEST.MF file
						if (!compareManifest(firstEntryClassFileBytes, secondEntryClassFileBytes)) {
							return newErrorStatus(NLS.bind(Messages.differentEntry, entryName, sourceFile.getAbsolutePath()));
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
		} catch (IOException e) {
			// missing entry
			return newErrorStatus(NLS.bind(Messages.ioexception, new String[] {sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath()}), e);
		} finally {
			Utility.close(firstFile);
			Utility.close(secondFile);
		}
		return Status.OK_STATUS;
	}

	private boolean compareManifest(byte[] firstEntryClassFileBytes, byte[] secondEntryClassFileBytes) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(firstEntryClassFileBytes);
		Manifest manifest = null;
		try {
			manifest = new Manifest(inputStream);
		} catch (IOException e) {
			// ignore
		} finally {
			Utility.close(inputStream);
		}
		if (manifest == null) {
			return true;
		}

		inputStream = new ByteArrayInputStream(secondEntryClassFileBytes);
		Manifest manifest2 = null;
		try {
			manifest2 = new Manifest(inputStream);
		} catch (IOException e) {
			// ignore
		} finally {
			Utility.close(inputStream);
		}
		if (manifest2 == null) {
			return true;
		}
		Attributes attributes = manifest.getMainAttributes();
		Attributes attributes2 = manifest2.getMainAttributes();
		if (attributes.size() != attributes2.size())
			return false;
		for (Iterator iterator = attributes.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			Object value2 = attributes2.get(entry.getKey());
			if (value2 == null) {
				return false;
			}
			if (!value2.equals(entry.getValue())) {
				return false;
			}
		}
		return true;
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
