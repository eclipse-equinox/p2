/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.osgi.framework.Version;

/**
 * Publishing advice from a p2 advice file. An advice file (p2.inf) can be embedded
 * in the source of a bundle, feature, or product to specify additional advice to be
 * added to the {@link IInstallableUnit} corresponding to the bundle, feature, or product.
 */
public class AdviceFileAdvice extends AbstractAdvice implements ITouchpointAdvice {
	/**
	 * The location of the bundle advice file, relative to the bundle root location.
	 */
	public static final IPath BUNDLE_ADVICE_FILE = new Path("META-INF/p2.inf"); //$NON-NLS-1$

	private static final String ADVICE_INSTRUCTIONS_PREFIX = "instructions."; //$NON-NLS-1$
	private final IPath basePath;
	private final IPath adviceFilePath;

	private final String id;
	private final Version version;

	/**
	 * Creates advice for an advice file at the given location. If <tt>basePath</tt>
	 * is a directory, then <tt>adviceFilePath</tt> is appended to this location to
	 * obtain the location of the advice file. If <tt>basePath</tt> is a file, then
	 * <tt>adviceFilePath</tt> is used to 
	 * @param id The symbolic id of the installable unit this advice applies to
	 * @param version The version of the installable unit this advice applies to
	 * @param basePath The root location of the the advice file. This is either the location of
	 * the jar containing the advice, or a directory containing the advice file
	 * @param adviceFilePath The location of the advice file within the base path. This is
	 * either the path of a jar entry, or the path of the advice file within the directory
	 * specified by the base path.
	 */
	public AdviceFileAdvice(String id, Version version, IPath basePath, IPath adviceFilePath) {
		Assert.isNotNull(id);
		Assert.isNotNull(version);
		Assert.isNotNull(basePath);
		Assert.isNotNull(adviceFilePath);
		this.id = id;
		this.version = version;
		this.basePath = basePath;
		this.adviceFilePath = adviceFilePath;
	}

	/**
	 * Loads the advice file and returns it in map form.
	 */
	private Map getInstructions() {
		File location = basePath.toFile();
		if (location == null || !location.exists())
			return Collections.EMPTY_MAP;

		ZipFile jar = null;
		InputStream stream = null;
		try {
			if (location.isDirectory()) {
				File adviceFile = new File(location, adviceFilePath.toString());
				try {
					stream = new BufferedInputStream(new FileInputStream(adviceFile));
				} catch (IOException e) {
					return Collections.EMPTY_MAP;
				}
			} else if (location.isFile()) {
				try {
					jar = new ZipFile(location);
					ZipEntry entry = jar.getEntry(adviceFilePath.toString());
					if (entry == null)
						return Collections.EMPTY_MAP;
					stream = new BufferedInputStream(jar.getInputStream(entry));
				} catch (IOException e) {
					return Collections.EMPTY_MAP;
				}
			}

			Properties advice = null;
			try {
				advice = new Properties();
				advice.load(stream);
			} catch (IOException e) {
				return Collections.EMPTY_MAP;
			}
			return advice != null ? advice : Collections.EMPTY_MAP;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					// ignore secondary failure
				}
			if (jar != null)
				try {
					jar.close();
				} catch (IOException e) {
					// ignore secondary failure
				}
		}
	}

	public boolean isApplicable(String configSpec, boolean includeDefault, String candidateId, Version candidateVersion) {
		if (!id.equals(candidateId) || !version.equals(candidateVersion))
			return false;
		// only process this advice if there is an advice file present
		File location = basePath.toFile();
		if (!location.isDirectory())
			return location.exists();
		return new File(location, adviceFilePath.toString()).exists();
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.publisher.eclipse.ITouchpointAdvice#getTouchpointData()
	 */
	public TouchpointData getTouchpointData(TouchpointData existing) {
		Map touchpointData = new HashMap(existing.getInstructions());
		Map bundleAdvice = getInstructions();
		for (Iterator iterator = bundleAdvice.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			if (key.startsWith(ADVICE_INSTRUCTIONS_PREFIX)) {
				String phase = key.substring(ADVICE_INSTRUCTIONS_PREFIX.length());
				String instruction = ""; //$NON-NLS-1$
				if (touchpointData.containsKey(phase)) {
					Object previous = touchpointData.get(phase);
					instruction = previous instanceof TouchpointInstruction ? ((TouchpointInstruction) previous).getBody() : (String) previous;
					if (instruction.length() > 0 && !instruction.endsWith(";")) //$NON-NLS-1$
						instruction += ';';
				}
				instruction += ((String) bundleAdvice.get(key)).trim();
				touchpointData.put(phase, instruction);
			}
		}
		return MetadataFactory.createTouchpointData(touchpointData);
	}

}
