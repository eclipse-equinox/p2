/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import org.eclipse.equinox.internal.provisional.p2.core.VersionedName;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.LatestIUVersionQuery;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.p2.publisher.actions.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public abstract class AbstractPublisherAction implements IPublisherAction {
	static final class CapabilityKey {
		private String namespace;
		private String name;

		public CapabilityKey(String namespace, String name) {
			this.namespace = namespace;
			this.name = name;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CapabilityKey other = (CapabilityKey) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (namespace == null) {
				if (other.namespace != null)
					return false;
			} else if (!namespace.equals(other.namespace))
				return false;
			return true;
		}

	}

	private static final String CONFIG_ANY = "ANY"; //$NON-NLS-1$
	public static final String CONFIG_SEGMENT_SEPARATOR = "."; //$NON-NLS-1$

	protected IPublisherInfo info;

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Returns a string array of { ws, os, arch } as parsed from the given string
	 * @param configSpec the string to parse
	 * @return the ws, os, arch form of the given string
	 */
	public static String[] parseConfigSpec(String configSpec) {
		String[] result = getArrayFromString(configSpec, CONFIG_SEGMENT_SEPARATOR);
		for (int i = 0; i < result.length; i++)
			if (result[i].equals("*")) //$NON-NLS-1$
				result[i] = CONFIG_ANY;
		return result;
	}

	/**
	 * Returns the canonical form of config spec with the given ws, os and arch.
	 * Note that the result is intended to be machine readable (i.e., parseConfigSpec
	 * will parse the the result).
	 * @param ws the window system
	 * @param os the operating system
	 * @param arch the machine architecture
	 * @return the machine readable format of the given config spec
	 */
	public static String createConfigSpec(String ws, String os, String arch) {
		return ws + '.' + os + '.' + arch;
	}

	protected void addSelfCapability(InstallableUnitDescription root) {
		root.setCapabilities(new IProvidedCapability[] {createSelfCapability(root.getId(), root.getVersion())});
	}

	/**
	 * Returns the LDAP filter form that matches the given config spec.  Returns
	 * an empty String if the spec does not identify an ws, os or arch.
	 * @param configSpec a config spec to filter
	 * @return the LDAP filter for the given spec.  <code>null</code> if the given spec does not 
	 * parse into a filter.
	 */
	protected String createFilterSpec(String configSpec) {
		String[] config = parseConfigSpec(configSpec);
		if (config[0] != null || config[1] != null || config[2] != null) {
			String filterWs = config[0] != null && config[0] != CONFIG_ANY ? "(osgi.ws=" + config[0] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterOs = config[1] != null && config[1] != CONFIG_ANY ? "(osgi.os=" + config[1] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterArch = config[2] != null && config[2] != CONFIG_ANY ? "(osgi.arch=" + config[2] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (filterWs.length() == 0 && filterOs.length() == 0 && filterArch.length() == 0)
				return null;
			return "(& " + filterWs + filterOs + filterArch + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	/**
	 * Returns the normalized string form of the given config spec.  This is useful for putting
	 * in IU ids etc. Note that the result is not intended to be machine readable (i.e., parseConfigSpec
	 * may not work on the result).
	 * @param configSpec the config spec to format
	 * @return the readable format of the given config spec
	 */
	protected String createIdString(String configSpec) {
		String[] config = parseConfigSpec(configSpec);
		return config[0] + '.' + config[1] + '.' + config[2];
	}

	protected String createCUIdString(String id, String type, String flavor, String configSpec) {
		return flavor + id + "." + type + "." + createIdString(configSpec); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates and returns a collection of RequiredCapabilities for the IUs represented
	 * by the given collection.  The collection may include a mixture of IInstallableUnits
	 * or VersionedNames.
	 * @param children descriptions of the IUs on which requirements are to be made
	 * @return a collection of RequiredCapabilities representing the given IUs
	 */
	protected Collection createIURequirements(Collection children) {
		ArrayList result = new ArrayList(children.size());
		for (Iterator i = children.iterator(); i.hasNext();) {
			Object next = i.next();
			if (next instanceof IInstallableUnit) {
				IInstallableUnit iu = (IInstallableUnit) next;
				VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
				result.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
			} else if (next instanceof VersionedName) {
				VersionedName name = (VersionedName) next;
				Version version = name.getVersion();
				VersionRange range = (version == null || Version.emptyVersion.equals(version)) ? VersionRange.emptyRange : new VersionRange(version, true, version, true);
				String filter = getFilterAdvice(name);
				result.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, name.getId(), range, filter, false, false));
			}
		}
		return result;
	}

	private String getFilterAdvice(VersionedName name) {
		if (info == null)
			return null;
		Collection filterAdvice = info.getAdvice(CONFIG_ANY, true, name.getId(), name.getVersion(), IFilterAdvice.class);
		for (Iterator i = filterAdvice.iterator(); i.hasNext();) {
			IFilterAdvice advice = (IFilterAdvice) i.next();
			String result = advice.getFilter(name.getId(), name.getVersion(), false);
			if (result != null)
				return result;
		}
		return null;
	}

	protected InstallableUnitDescription createIUShell(String id, Version version) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setId(id);
		root.setVersion(version);
		return root;
	}

	protected IArtifactDescriptor createPack200ArtifactDescriptor(IArtifactKey key, File pathOnDisk, String installSize) {
		final String PACKED_FORMAT = "packed"; //$NON-NLS-1$
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null) {
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, installSize);
			// TODO - this is wrong but I'm testing a work-around for bug 205842
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));
		}
		ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
		result.setProcessingSteps(steps);
		result.setProperty(IArtifactDescriptor.FORMAT, PACKED_FORMAT);
		return result;
	}

	protected InstallableUnitDescription createParentIU(Collection children, String id, Version version) {
		InstallableUnitDescription root = createIUShell(id, version);
		root.addRequiredCapabilities(createIURequirements(children));
		addSelfCapability(root);
		return root;
	}

	//This is to hide FileUtils from other actions
	protected IPathComputer createParentPrefixComputer(int segmentsToKeep) {
		return FileUtils.createParentPrefixComputer(segmentsToKeep);
	}

	//This is to hide FileUtils from other actions
	protected IPathComputer createRootPrefixComputer(final File root) {
		return FileUtils.createRootPathComputer(root);
	}

	protected IProvidedCapability createSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return MetadataFactory.createProvidedCapability(PublisherHelper.IU_NAMESPACE, installableUnitId, installableUnitVersion);
	}

	protected static InstallableUnitDescription[] processAdditionalInstallableUnitsAdvice(IInstallableUnit iu, IPublisherInfo publisherInfo) {
		Collection advice = publisherInfo.getAdvice(null, false, iu.getId(), iu.getVersion(), IAdditionalInstallableUnitAdvice.class);
		if (advice.isEmpty())
			return null;

		List ius = new ArrayList();
		for (Iterator iterator = advice.iterator(); iterator.hasNext();) {
			IAdditionalInstallableUnitAdvice entry = (IAdditionalInstallableUnitAdvice) iterator.next();
			InstallableUnitDescription[] others = entry.getAdditionalInstallableUnitDescriptions(iu);
			if (others != null)
				ius.addAll(Arrays.asList(others));
		}
		return (InstallableUnitDescription[]) ius.toArray(new InstallableUnitDescription[ius.size()]);
	}

	/**
	 * Add all of the advised artifact properties for the given IU and artifact descriptor.
	 * @param iu the IU
	 * @param descriptor the descriptor to decorate
	 * @param info the publisher info supplying the advice
	 */
	protected static void processArtifactPropertiesAdvice(IInstallableUnit iu, ArtifactDescriptor descriptor, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, iu.getId(), iu.getVersion(), IPropertyAdvice.class);
		for (Iterator i = advice.iterator(); i.hasNext();) {
			IPropertyAdvice entry = (IPropertyAdvice) i.next();
			Properties props = entry.getArtifactProperties(iu, descriptor);
			if (props == null)
				continue;
			for (Iterator j = props.keySet().iterator(); j.hasNext();) {
				String key = (String) j.next();
				descriptor.setRepositoryProperty(key, props.getProperty(key));
			}
		}
	}

	/**
	 * Add all of the advised IU properties for the given IU.
	 * @param iu the IU to decorate
	 * @param info the publisher info supplying the advice
	 */
	protected static void processInstallableUnitPropertiesAdvice(InstallableUnitDescription iu, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, iu.getId(), iu.getVersion(), IPropertyAdvice.class);
		for (Iterator i = advice.iterator(); i.hasNext();) {
			IPropertyAdvice entry = (IPropertyAdvice) i.next();
			Properties props = entry.getInstallableUnitProperties(iu);
			if (props == null)
				continue;
			for (Iterator j = props.keySet().iterator(); j.hasNext();) {
				String key = (String) j.next();
				iu.setProperty(key, props.getProperty(key));
			}
		}
	}

	/**
	 * Add all of the advised provided and required capabilities for the given installable unit.
	 * @param iu the IU to decorate
	 * @param info the publisher info supplying the advice
	 */
	protected static void processCapabilityAdvice(InstallableUnitDescription iu, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, iu.getId(), iu.getVersion(), ICapabilityAdvice.class);
		if (advice.isEmpty())
			return;

		Map requiredMap = new HashMap();
		IRequiredCapability[] required = iu.getRequiredCapabilities();
		for (int i = 0; i < required.length; i++) {
			requiredMap.put(new CapabilityKey(required[i].getNamespace(), required[i].getName()), required[i]);
		}

		Map metaRequiredMap = new HashMap();
		IRequiredCapability[] metaRequired = iu.getMetaRequiredCapabilities();
		for (int i = 0; i < metaRequired.length; i++) {
			metaRequiredMap.put(new CapabilityKey(metaRequired[i].getNamespace(), metaRequired[i].getName()), metaRequired[i]);
		}

		Map providedMap = new HashMap();
		IProvidedCapability[] provided = iu.getProvidedCapabilities();
		for (int i = 0; i < provided.length; i++) {
			providedMap.put(new CapabilityKey(provided[i].getNamespace(), provided[i].getName()), provided[i]);
		}

		for (Iterator it = advice.iterator(); it.hasNext();) {
			ICapabilityAdvice entry = (ICapabilityAdvice) it.next();

			IRequiredCapability[] requiredAdvice = entry.getRequiredCapabilities(iu);
			if (requiredAdvice != null) {
				for (int i = 0; i < requiredAdvice.length; i++) {
					requiredMap.put(new CapabilityKey(requiredAdvice[i].getNamespace(), requiredAdvice[i].getName()), requiredAdvice[i]);
				}
			}

			IRequiredCapability[] metaRequiredAdvice = entry.getMetaRequiredCapabilities(iu);
			if (metaRequiredAdvice != null) {
				for (int i = 0; i < metaRequiredAdvice.length; i++) {
					metaRequiredMap.put(new CapabilityKey(metaRequiredAdvice[i].getNamespace(), metaRequiredAdvice[i].getName()), metaRequiredAdvice[i]);
				}
			}

			IProvidedCapability[] providedAdvice = entry.getProvidedCapabilities(iu);
			if (providedAdvice != null) {
				for (int i = 0; i < providedAdvice.length; i++) {
					providedMap.put(new CapabilityKey(providedAdvice[i].getNamespace(), providedAdvice[i].getName()), providedAdvice[i]);
				}
			}
		}

		IRequiredCapability[] resultRequired = (IRequiredCapability[]) requiredMap.values().toArray(new IRequiredCapability[requiredMap.size()]);
		iu.setRequiredCapabilities(resultRequired);

		IRequiredCapability[] resultMetaRequired = (IRequiredCapability[]) metaRequiredMap.values().toArray(new IRequiredCapability[metaRequiredMap.size()]);
		iu.setMetaRequiredCapabilities(resultMetaRequired);

		IProvidedCapability[] resultProvided = (IProvidedCapability[]) providedMap.values().toArray(new IProvidedCapability[providedMap.size()]);
		iu.setCapabilities(resultProvided);
	}

	/**
	 * Adds all applicable touchpoint advice to the given installable unit.
	 * @param iu The installable unit to add touchpoint advice to
	 * @param currentInstructions The set of touchpoint instructions assembled for this IU so far
	 * @param info The publisher info
	 */
	protected static void processTouchpointAdvice(InstallableUnitDescription iu, Map currentInstructions, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, iu.getId(), iu.getVersion(), ITouchpointAdvice.class);
		if (currentInstructions == null) {
			if (advice.isEmpty())
				return;

			currentInstructions = Collections.EMPTY_MAP;
		}

		ITouchpointData result = MetadataFactory.createTouchpointData(currentInstructions);
		for (Iterator i = advice.iterator(); i.hasNext();) {
			ITouchpointAdvice entry = (ITouchpointAdvice) i.next();
			result = entry.getTouchpointData(result);
		}
		iu.addTouchpointData(result);
	}

	/**
	 * Publishes the artifact by zipping the <code>files</code> using <code>root</code>
	 * as a base for relative paths. Then copying the zip into the repository.
	 * @param descriptor used to identify the zip.
	 * @param inclusion the file to be published. files can be <code>null</code> but no action is taken.
	 * @param publisherInfo the publisher info.
	 */
	protected void publishArtifact(IArtifactDescriptor descriptor, File inclusion, IPublisherInfo publisherInfo) {
		// no files to publish so this is done.
		if (inclusion == null)
			return;
		// if the destination already contains the descriptor, there is nothing to do.
		IArtifactRepository destination = publisherInfo.getArtifactRepository();
		if (destination == null || destination.contains(descriptor))
			return;

		if (destination instanceof IFileArtifactRepository) {
			// TODO  need to review this logic to ensure it makes sense.
			// if the file is already in the same location the repo will put it, just add the descriptor and exit
			File descriptorFile = ((IFileArtifactRepository) destination).getArtifactFile(descriptor);
			if (inclusion.equals(descriptorFile)) {
				destination.addDescriptor(descriptor);
				return;
			}
		}

		// if all we are doing is indexing things then add the descriptor and get on with it
		if ((publisherInfo.getArtifactOptions() & IPublisherInfo.A_PUBLISH) == 0) {
			destination.addDescriptor(descriptor);
			return;
		}
		try {
			if (destination instanceof IFileArtifactRepository) {
				// TODO  need to review this logic to ensure it makes sense.
				// if the file is already in the same location the repo will put it, just add the descriptor and exit
				File descriptorFile = ((IFileArtifactRepository) destination).getArtifactFile(descriptor);
				if (inclusion.equals(descriptorFile)) {
					destination.addDescriptor(descriptor);
					return;
				}
			}

			// TODO need to implement the overwrite story in the repos
			//			boolean overwrite = (info.getArtifactOptions() & IPublisherInfo.A_OVERWRITE) > 0;
			OutputStream output = destination.getOutputStream(descriptor);
			if (output == null)
				return;
			output = new BufferedOutputStream(output);
			FileUtils.copyStream(new BufferedInputStream(new FileInputStream(inclusion)), true, output, true);
		} catch (ProvisionException e) {
			LogHelper.log(e.getStatus());
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
		}
	}

	/**
	 * Publishes the artifact by zipping the <code>files</code> using <code>root</code>
	 * as a base for relative paths. Then copying the zip into the repository.
	 * @param descriptor used to identify the zip.
	 * @param inclusions and folders to be included in the zip. files can be null.
	 * @param exclusions and folders to be excluded in the zip. files can be null.
	 * @param publisherInfo the publisher info.
	 * @param prefixComputer
	 */
	protected void publishArtifact(IArtifactDescriptor descriptor, File[] inclusions, File[] exclusions, IPublisherInfo publisherInfo, IPathComputer prefixComputer) {
		// no files to publish so this is done.
		if (inclusions == null || inclusions.length < 1)
			return;
		// if the destination already contains the descriptor, there is nothing to do.
		IArtifactRepository destination = publisherInfo.getArtifactRepository();
		if (destination == null || destination.contains(descriptor))
			return;
		// if all we are doing is indexing things then add the descriptor and get on with it
		if ((publisherInfo.getArtifactOptions() & IPublisherInfo.A_PUBLISH) == 0) {
			destination.addDescriptor(descriptor);
			return;
		}

		// TODO need to implement the overwrite story in the repos
		//		boolean overwrite = (info.getArtifactOptions() & IPublisherInfo.A_OVERWRITE) > 0;
		// if there is just one file and the mode is as-is, just copy the file into the repo
		// otherwise, zip up the files and copy the zip into the repo
		File tempFile = null;
		try {
			OutputStream output = destination.getOutputStream(descriptor);
			if (output == null)
				return;
			output = new BufferedOutputStream(output);
			tempFile = File.createTempFile("p2.generator", ""); //$NON-NLS-1$ //$NON-NLS-2$
			FileUtils.zip(inclusions, exclusions, tempFile, prefixComputer);
			if (output != null)
				FileUtils.copyStream(new BufferedInputStream(new FileInputStream(tempFile)), true, output, true);
		} catch (ProvisionException e) {
			LogHelper.log(e.getStatus());
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
			e.printStackTrace();
		} finally {
			if (tempFile != null)
				tempFile.delete();
		}
	}

	/**
	 * Loop over the known metadata repositories looking for the given IU.
	 * Return the first IU found.
	 * @param iuId  the id of the IU to look for
	 * @return the first matching IU or <code>null</code> if none.
	 */
	protected IInstallableUnit queryForIU(IPublisherResult publisherResult, String iuId, Version version) {
		Query query = null;
		Collector collector = null;
		if (version != null && !Version.emptyVersion.equals(version)) {
			query = new InstallableUnitQuery(iuId, version);
			collector = new SingleElementCollector();
		} else {
			query = new CompositeQuery(new Query[] {new InstallableUnitQuery(iuId), new LatestIUVersionQuery()});
			collector = new Collector();
		}

		NullProgressMonitor progress = new NullProgressMonitor();
		if (publisherResult != null)
			collector = publisherResult.query(query, collector, progress);
		if (collector.isEmpty() && info.getMetadataRepository() != null)
			collector = info.getMetadataRepository().query(query, collector, progress);
		if (collector.isEmpty() && info.getContextMetadataRepository() != null)
			collector = info.getContextMetadataRepository().query(query, collector, progress);

		if (!collector.isEmpty())
			return (IInstallableUnit) collector.iterator().next();
		return null;
	}

	/**
	 * Loop over the known metadata repositories looking for the given IU within a particular range
	 * @param publisherResult
	 * @param iuId the id of the IU to look for
	 * @param versionRange the version range to consider
	 * @return The the IUs with the matching ids in the given range
	 */
	protected IInstallableUnit[] queryForIUs(IPublisherResult publisherResult, String iuId, VersionRange versionRange) {
		Query query = null;
		Collector collector = new Collector();
		query = new InstallableUnitQuery(iuId, versionRange);
		NullProgressMonitor progress = new NullProgressMonitor();
		if (publisherResult != null)
			collector = publisherResult.query(query, collector, progress);
		if (collector.isEmpty() && info.getMetadataRepository() != null)
			collector = info.getMetadataRepository().query(query, collector, progress);
		if (collector.isEmpty() && info.getContextMetadataRepository() != null)
			collector = info.getContextMetadataRepository().query(query, collector, progress);

		if (!collector.isEmpty())
			return (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
		return new IInstallableUnit[0];
	}

	public abstract IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor);

	public void setPublisherInfo(IPublisherInfo info) {
		this.info = info;
	}
}
