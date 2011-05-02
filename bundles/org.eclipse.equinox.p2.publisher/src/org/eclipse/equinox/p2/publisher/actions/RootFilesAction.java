/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class RootFilesAction extends AbstractPublisherAction {
	private String idBase;
	private Version version;
	private String flavor;
	private boolean createParent = true;

	/**
	 * Returns the id of the top level IU published by this action for the given id and flavor.
	 * @param id the id of the application being published
	 * @param flavor the flavor being published
	 * @return the if for ius published by this action
	 */
	public static String computeIUId(String id, String flavor) {
		return flavor + id + ".rootfiles"; //$NON-NLS-1$
	}

	public RootFilesAction(IPublisherInfo info, String idBase, Version version, String flavor) {
		this.idBase = idBase == null ? "org.eclipse" : idBase; //$NON-NLS-1$
		this.version = version;
		this.flavor = flavor;
	}

	public void setCreateParent(boolean createParent) {
		this.createParent = createParent;
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		setPublisherInfo(publisherInfo);
		IPublisherResult innerResult = new PublisherResult();
		// we have N platforms, generate a CU for each
		// TODO try and find common properties across platforms
		String[] configSpecs = publisherInfo.getConfigurations();
		for (int i = 0; i < configSpecs.length; i++) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			generateRootFileIUs(configSpecs[i], innerResult);
		}
		// merge the IUs  into the final result as non-roots and create a parent IU that captures them all
		results.merge(innerResult, IPublisherResult.MERGE_ALL_NON_ROOT);
		if (createParent)
			publishTopLevelRootFilesIU(innerResult.getIUs(null, IPublisherResult.ROOT), results);
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;
		return Status.OK_STATUS;
	}

	private void publishTopLevelRootFilesIU(Collection<? extends IVersionedId> children, IPublisherResult result) {
		InstallableUnitDescription descriptor = createParentIU(children, computeIUId(idBase, flavor), version);
		descriptor.setSingleton(true);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return;
		result.addIU(rootIU, IPublisherResult.ROOT);
	}

	/**
	 * Generates IUs and CUs for the files that make up the root files for a given
	 * ws/os/arch combination.
	 */
	private void generateRootFileIUs(String configSpec, IPublisherResult result) {
		// Create the IU for the executable
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String idPrefix = idBase + ".rootfiles"; //$NON-NLS-1$
		String iuId = idPrefix + '.' + createIdString(configSpec);
		iu.setId(iuId);
		iu.setVersion(version);
		IMatchExpression<IInstallableUnit> filter = createFilterSpec(configSpec);
		iu.setFilter(filter);
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey(iuId, version);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		IProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(flavor + idBase, idPrefix, version);
		iu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(iuId, version), launcherCapability});
		result.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);

		// Create the CU that installs/configures the executable
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = flavor + iuId;
		cu.setId(configUnitId);
		cu.setVersion(version);
		cu.setFilter(filter);
		cu.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iuId, new VersionRange(version, true, version, true), null, false, false)});
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());

		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(configUnitId, version)});

		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		Map<String, String> touchpointData = new HashMap<String, String>();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		processTouchpointAdvice(cu, touchpointData, info, configSpec);
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.addIU(unit, IPublisherResult.ROOT);

		if ((info.getArtifactOptions() & (IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH)) > 0) {
			// Create the artifact descriptor.  we have several files so no path on disk
			IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(info, key, null);
			IRootFilesAdvice advice = getAdvice(configSpec);
			publishArtifact(descriptor, advice.getIncludedFiles(), advice.getExcludedFiles(), info, createPrefixComputer(advice.getRoot()));
		}
	}

	private IPathComputer createPrefixComputer(File root) {
		if (root == null)
			return createParentPrefixComputer(1);
		return createRootPrefixComputer(root);
	}

	/**
	 * Compiles the <class>IRootFilesAdvice</class> from the <code>info</code> into one <class>IRootFilesAdvice</class> 
	 * and returns the result.
	 * @param configSpec
	 * @return a compilation of <class>IRootfilesAdvice</class> from the <code>info</code>.
	 */
	private IRootFilesAdvice getAdvice(String configSpec) {
		Collection<IRootFilesAdvice> advice = info.getAdvice(configSpec, true, null, null, IRootFilesAdvice.class);
		ArrayList<File> inclusions = new ArrayList<File>();
		ArrayList<File> exclusions = new ArrayList<File>();
		File root = null;
		for (IRootFilesAdvice entry : advice) {
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
		File[] includeList = inclusions.toArray(new File[inclusions.size()]);
		File[] excludeList = exclusions.toArray(new File[exclusions.size()]);
		return new RootFilesAdvice(root, includeList, excludeList, configSpec);
	}

}
