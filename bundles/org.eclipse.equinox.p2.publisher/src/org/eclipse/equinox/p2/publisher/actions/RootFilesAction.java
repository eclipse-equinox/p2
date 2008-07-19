/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

// TODO need to merge this functionality with the FeaturesAction work on root files
public class RootFilesAction extends AbstractPublisherAction {
	private String idBase;
	private String versionSpec = "1.0.0"; //$NON-NLS-1$
	private String flavor;

	public RootFilesAction(IPublisherInfo info, String idBase, String version, String flavor) {
		this.idBase = idBase == null ? "org.eclipse" : idBase; //$NON-NLS-1$
		// if the given version is not the default "replace me" version then save it
		if (version != null && !version.equals("0.0.0")) //$NON-NLS-1$
			this.versionSpec = version;
		this.flavor = flavor;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		String[] configSpecs = info.getConfigurations();
		for (int i = 0; i < configSpecs.length; i++)
			generateRootFileIUs(configSpecs[i], info, results);
		return Status.OK_STATUS;
	}

	/**
	 * Generates IUs and CUs for the files that make up the root files for a given
	 * ws/os/arch combination.
	 */
	protected void generateRootFileIUs(String configSpec, IPublisherInfo info, IPublisherResult result) {
		// Create the IU for the executable
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String idPrefix = idBase + ".rootfiles"; //$NON-NLS-1$
		String iuId = idPrefix + '.' + createIdString(configSpec);
		iu.setId(iuId);
		Version version = new Version(versionSpec);
		iu.setVersion(version);
		String filter = createFilterSpec(configSpec);
		iu.setFilter(filter);
		IArtifactKey key = PublisherHelper.createLauncherArtifactKey(iuId, version);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		ProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(flavor + idBase, idPrefix, version); //$NON-NLS-1$
		iu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(iuId, version), launcherCapability});
		result.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);

		// Create the CU that installs/configures the executable
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = flavor + iuId;
		cu.setId(configUnitId);
		cu.setVersion(version);
		cu.setFilter(filter);
		cu.setHost(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iuId, new VersionRange(version, true, version, true), null, false, false)});
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());

		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(configUnitId, version)});

		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.addIU(unit, IPublisherResult.ROOT);

		if ((info.getArtifactOptions() & (IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH)) > 0) {
			// Create the artifact descriptor.  we have several files so no path on disk
			IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
			IRootFilesAdvice advice = getAdvice(configSpec, info);
			publishArtifact(descriptor, filterRootFiles(advice, info), advice.getRoot(), info, INCLUDE_ROOT);
		}
	}

	private File[] filterRootFiles(IRootFilesAdvice advice, IPublisherInfo info) {
		File[] inclusions = advice.getIncludedFiles();
		Set exclusions = new HashSet(Arrays.asList(advice.getExcludedFiles()));
		ArrayList result = new ArrayList();
		for (int i = 0; i < inclusions.length; i++)
			filterFile(inclusions[i], exclusions, result);
		return (File[]) result.toArray(new File[result.size()]);
	}

	private void filterFile(File inclusion, Collection exclusions, ArrayList result) {
		if (exclusions == null || !exclusions.contains(inclusion))
			if (inclusion.isDirectory()) {
				File[] list = inclusion.listFiles();
				for (int i = 0; i < list.length; i++)
					filterFile(list[i], exclusions, result);
			} else
				result.add(inclusion);
	}

	/**
	 * Compiles the <class>IRootFilesAdvice</class> from the <code>info</code> into one <class>IRootFilesAdvice</class> 
	 * and returns the result.
	 * @param configSpec
	 * @param info - the publisher info holding the advice.
	 * @return a compilation of <class>IRootfilesAdvice</class> from the <code>info</code>.
	 */
	private IRootFilesAdvice getAdvice(String configSpec, IPublisherInfo info) {
		Collection advice = info.getAdvice(configSpec, true, null, null, IRootFilesAdvice.class);
		ArrayList inclusions = new ArrayList();
		ArrayList exclusions = new ArrayList();
		File root = null;
		for (Iterator i = advice.iterator(); i.hasNext();) {
			IRootFilesAdvice entry = (IRootFilesAdvice) i.next();
			// TODO for now we simply get root from the first advice that has one
			if (root == null)
				root = entry.getRoot();
			File[] list = entry.getIncludedFiles();
			if (list != null)
				inclusions.addAll(Arrays.asList(list));
			list = entry.getExcludedFiles();
			if (list != null)
				exclusions.addAll(Arrays.asList(list));
		}
		File[] includeList = (File[]) inclusions.toArray(new File[inclusions.size()]);
		File[] excludeList = (File[]) exclusions.toArray(new File[exclusions.size()]);
		return new RootFilesAdvice(root, includeList, excludeList, configSpec);
	}

}
