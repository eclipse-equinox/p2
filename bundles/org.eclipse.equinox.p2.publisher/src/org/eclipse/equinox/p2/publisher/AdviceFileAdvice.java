/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.actions.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Publishing advice from a p2 advice file. An advice file (p2.inf) can be embedded
 * in the source of a bundle, feature, or product to specify additional advice to be
 * added to the {@link IInstallableUnit} corresponding to the bundle, feature, or product.
 */
public class AdviceFileAdvice extends AbstractAdvice implements ITouchpointAdvice, ICapabilityAdvice, IUpdateDescriptorAdvice, IPropertyAdvice, IAdditionalInstallableUnitAdvice {

	/**
	 * The location of the bundle advice file, relative to the bundle root location.
	 */
	public static final IPath BUNDLE_ADVICE_FILE = new Path("META-INF/p2.inf"); //$NON-NLS-1$

	private final String id;
	private final Version version;

	private Map<String, ITouchpointInstruction> touchpointInstructions;
	private IProvidedCapability[] providedCapabilities;
	private IRequirement[] requiredCapabilities;
	private IRequirement[] metaRequiredCapabilities;
	private Map<String, String> iuProperties;
	private InstallableUnitDescription[] additionalIUs;
	private IUpdateDescriptor updateDescriptor;
	private boolean containsAdvice = false;

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

		Map<String, String> advice = loadAdviceMap(basePath, adviceFilePath);
		if (advice.isEmpty())
			return;

		AdviceFileParser parser = new AdviceFileParser(id, version, advice);
		try {
			parser.parse();
		} catch (Exception e) {
			String message = "An error occured while parsing advice file: basePath=" + basePath + ", adviceFilePath=" + adviceFilePath + "."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			IStatus status = new Status(IStatus.ERROR, Activator.ID, message, e);
			LogHelper.log(status);
			return;
		}
		touchpointInstructions = parser.getTouchpointInstructions();
		providedCapabilities = parser.getProvidedCapabilities();
		requiredCapabilities = parser.getRequiredCapabilities();
		metaRequiredCapabilities = parser.getMetaRequiredCapabilities();
		iuProperties = parser.getProperties();
		additionalIUs = parser.getAdditionalInstallableUnitDescriptions();
		updateDescriptor = parser.getUpdateDescriptor();
		containsAdvice = true;
	}

	public boolean containsAdvice() {
		return containsAdvice;
	}

	/**
	 * Loads the advice file and returns it in map form.
	 */
	private static Map<String, String> loadAdviceMap(IPath basePath, IPath adviceFilePath) {
		File location = basePath.toFile();
		if (location == null || !location.exists())
			return Collections.<String, String> emptyMap();

		ZipFile jar = null;
		InputStream stream = null;
		try {
			if (location.isDirectory()) {
				File adviceFile = new File(location, adviceFilePath.toString());
				if (!adviceFile.isFile())
					return Collections.<String, String> emptyMap();
				stream = new BufferedInputStream(new FileInputStream(adviceFile));
			} else if (location.isFile()) {
				jar = new ZipFile(location);
				ZipEntry entry = jar.getEntry(adviceFilePath.toString());
				if (entry == null)
					return Collections.<String, String> emptyMap();

				stream = new BufferedInputStream(jar.getInputStream(entry));
			}
			return CollectionUtils.loadProperties(stream);
		} catch (IOException e) {
			String message = "An error occured while reading advice file: basePath=" + basePath + ", adviceFilePath=" + adviceFilePath + "."; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			IStatus status = new Status(IStatus.ERROR, Activator.ID, message, e);
			LogHelper.log(status);
			return Collections.<String, String> emptyMap();
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

	@Override
	public boolean isApplicable(String configSpec, boolean includeDefault, String candidateId, Version candidateVersion) {
		return id.equals(candidateId) && version.equals(candidateVersion);
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.publisher.eclipse.ITouchpointAdvice#getTouchpointData()
	 */
	public ITouchpointData getTouchpointData(ITouchpointData existing) {
		return MetadataFactory.mergeTouchpointData(existing, touchpointInstructions);
	}

	public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu) {
		return providedCapabilities;
	}

	public IRequirement[] getRequiredCapabilities(InstallableUnitDescription iu) {
		return requiredCapabilities;
	}

	public IRequirement[] getMetaRequiredCapabilities(InstallableUnitDescription iu) {
		return metaRequiredCapabilities;
	}

	public InstallableUnitDescription[] getAdditionalInstallableUnitDescriptions(IInstallableUnit iu) {
		return additionalIUs;
	}

	public IUpdateDescriptor getUpdateDescriptor(InstallableUnitDescription iu) {
		return updateDescriptor;
	}

	public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
		return null;
	}

	public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu) {
		return iuProperties;
	}
}
