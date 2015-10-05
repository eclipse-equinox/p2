/*******************************************************************************
 * Copyright (c) 2015  Rapicorp, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rapicorp, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.eclipse.equinox.internal.p2.metadata.repository.Messages;
import org.eclipse.osgi.util.NLS;
import org.tukaani.xz.*;

/**
 * A class taking care of creating the XZ'ed version of a repository.
 * It takes as input the folder where a p2 repository is stored and produces the new files in this folder.
 * 
 * The files expected in to be found as input are content.xml or content.jar, or artifacts.xml or artifacts.jar. 
 * Note that the tool does not require both the a metadata repository and an artifact repository.
 * The output will be <fileName>.xml.xz, and a p2.index
 *
 */

public class XZCompressor {
	private static final String CONTENT_XML_XZ = "content.xml.xz"; //$NON-NLS-1$
	private static final String ARTIFACTS_XML_XZ = "artifacts.xml.xz"; //$NON-NLS-1$
	private static final String ARTIFACTS2 = "artifacts"; //$NON-NLS-1$
	private static final String CONTENT = "content"; //$NON-NLS-1$
	private static final String JAR = ".jar"; //$NON-NLS-1$
	private static final String XML = ".xml"; //$NON-NLS-1$

	private String repoFolder;
	private boolean preserveOriginalFile = true;
	private ArrayList<File> filesToDelete = new ArrayList<File>();

	public String getRepoFolder() {
		return repoFolder;
	}

	public void setRepoFolder(String repoFolder) {
		this.repoFolder = repoFolder;
	}

	public boolean isPreserveOriginalFile() {
		return preserveOriginalFile;
	}

	public void setPreserveOriginalFile(boolean preserveOriginalFile) {
		this.preserveOriginalFile = preserveOriginalFile;
	}

	private File uncompressJar(File jarFile, String fileToExtract) throws IOException {
		JarInputStream jarStream = new JarInputStream(new FileInputStream(jarFile));
		JarEntry jarEntry = jarStream.getNextJarEntry();
		while (jarEntry != null && (!fileToExtract.equals(jarEntry.getName()))) {
			jarEntry = jarStream.getNextJarEntry();
		}
		//if there is a jar but the entry is missing or invalid, treat this as an invalid repository
		if (jarEntry == null) {
			jarStream.close();
			throw new IOException(NLS.bind(Messages.repoMan_invalidLocation, jarFile.getAbsolutePath()));
		}

		BufferedInputStream input = null;
		OutputStream output = null;
		File extractedFile = new File(jarFile.getAbsoluteFile().getParent(), fileToExtract);
		try {
			input = new BufferedInputStream(jarStream);
			output = new BufferedOutputStream(new FileOutputStream(extractedFile));

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} finally {
			if (input != null) {
				input.close();
			}
			if (output != null) {
				output.close();
			}
		}
		filesToDelete.add(extractedFile);
		return extractedFile;
	}

	private File getMetadataFile(String prefix) throws IOException {
		File candidate = new File(repoFolder, prefix + XML);
		if (candidate.exists()) {
			if (!preserveOriginalFile) {
				filesToDelete.add(candidate);
			}
			return candidate;
		}

		candidate = new File(repoFolder, prefix + JAR);
		if (candidate.exists()) {
			if (!preserveOriginalFile) {
				filesToDelete.add(candidate);
			}
			return uncompressJar(candidate, prefix + XML);
		}
		return null;
	}

	public void compressRepo() throws IOException {
		File metadata = getMetadataFile(CONTENT);
		if (metadata != null)
			compressFile(metadata, new File(repoFolder, CONTENT_XML_XZ));

		File artifacts = getMetadataFile(ARTIFACTS2);
		if (artifacts != null)
			compressFile(artifacts, new File(repoFolder, ARTIFACTS_XML_XZ));

		createP2Index(metadata != null, artifacts != null);
		deleteFiles();
	}

	private void deleteFiles() {
		for (Iterator<File> iterator = filesToDelete.iterator(); iterator.hasNext();) {
			iterator.next().delete();
		}
	}

	private void compressFile(File input, File output) throws IOException {
		LZMA2Options options = new LZMA2Options();
		try {
			options.setDictSize(LZMA2Options.DICT_SIZE_DEFAULT);
			options.setLcLp(3, 0);
			options.setPb(LZMA2Options.PB_MAX);
			options.setMode(LZMA2Options.MODE_NORMAL);
			options.setNiceLen(LZMA2Options.NICE_LEN_MAX);
			options.setMatchFinder(LZMA2Options.MF_BT4);
			options.setDepthLimit(512);
		} catch (UnsupportedOptionsException e) {
			//Can't happen 
		}
		XZOutputStream out = null;
		java.io.FileInputStream is = null;
		try {
			out = new XZOutputStream(new FileOutputStream(output), options);
			is = new FileInputStream(input);
			byte[] buf = new byte[8192];
			int size;
			while ((size = is.read(buf)) != -1)
				out.write(buf, 0, size);
		} finally {
			if (out != null)
				out.close();
			if (is != null)
				is.close();
		}

	}

	private void createP2Index(boolean metadata, boolean artifacts) throws IOException {
		Properties p2Index = new Properties();
		if (metadata) {
			if (preserveOriginalFile)
				p2Index.setProperty("metadata.repository.factory.order", "content.xml.xz,content.xml,!"); //$NON-NLS-1$//$NON-NLS-2$
			else
				p2Index.setProperty("metadata.repository.factory.order", "content.xml.xz,!"); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (artifacts) {
			if (preserveOriginalFile)
				p2Index.setProperty("artifact.repository.factory.order", "artifacts.xml.xz,artifacts.xml,!"); //$NON-NLS-1$//$NON-NLS-2$
			else
				p2Index.setProperty("artifact.repository.factory.order", "artifacts.xml.xz,!"); //$NON-NLS-1$//$NON-NLS-2$
		}
		p2Index.setProperty("version", "1"); //$NON-NLS-1$//$NON-NLS-2$
		OutputStream output = null;
		try {
			output = new FileOutputStream(new File(repoFolder, "p2.index")); //$NON-NLS-1$
			p2Index.store(output, null);
		} finally {
			if (output != null)
				output.close();
		}
	}
}
