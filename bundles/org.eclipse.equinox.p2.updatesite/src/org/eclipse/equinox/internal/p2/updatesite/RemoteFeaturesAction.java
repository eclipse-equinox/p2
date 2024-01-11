/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;

public class RemoteFeaturesAction extends FeaturesAction {
	private UpdateSite updateSite;

	public RemoteFeaturesAction(UpdateSite updateSite) {
		super((Feature[]) null);
		this.updateSite = updateSite;
	}

	public RemoteFeaturesAction(Feature[] features) {
		super(features);
		throw new IllegalArgumentException();
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		try {
			this.info = publisherInfo;
			features = updateSite.loadFeatures(monitor);
			return super.perform(publisherInfo, results, monitor);
		} catch (ProvisionException e) {
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Error_Generation, updateSite), e);
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		}

	}

	@Override
	protected void generateFeatureIUs(Feature[] featureList, IPublisherResult result) {
		Map<String, String> extraProperties = new HashMap<>();
		extraProperties.put(IInstallableUnit.PROP_PARTIAL_IU, Boolean.TRUE.toString());
		for (Feature feature : featureList) {
			FeatureEntry[] featureEntries = feature.getEntries();
			for (FeatureEntry entry : featureEntries) {
				if (entry.isPlugin() && !entry.isRequires()) {
					Dictionary<String, String> mockManifest = new Hashtable<>();
					mockManifest.put("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
					mockManifest.put("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
					mockManifest.put("Bundle-SymbolicName", entry.getId()); //$NON-NLS-1$
					mockManifest.put("Bundle-Version", entry.getVersion()); //$NON-NLS-1$
					BundleDescription bundleDescription = BundlesAction.createBundleDescription(mockManifest, null);
					IArtifactKey key = BundlesAction.createBundleArtifactKey(entry.getId(), entry.getVersion());
					IInstallableUnit[] bundleIUs = EclipsePublisherHelper.createEclipseIU(bundleDescription, false, key,
							extraProperties);
					for (IInstallableUnit bundleIU : bundleIUs) {
						result.addIU(bundleIU, IPublisherResult.ROOT);
					}
				}
			}
			IInstallableUnit featureIU = createFeatureJarIU(feature, new PublisherInfo());
			List<IInstallableUnit> childIUs = new ArrayList<>();
			childIUs.add(featureIU);
			IInstallableUnit groupIU = createGroupIU(feature, childIUs, new PublisherInfo());
			result.addIU(featureIU, IPublisherResult.ROOT);
			result.addIU(groupIU, IPublisherResult.ROOT);
			generateSiteReferences(feature, result, info);
		}
	}
}
