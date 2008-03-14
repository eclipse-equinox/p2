/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.service.resolver.*;

/**
 * Publish IUs for all of the bundles in the given set of locations.  The locations can 
 * be actual locations of the bundles or folders of bundles.
 */
public class BundlesAction extends AbstractPublishingAction {

	protected static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	protected static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$

	private File[] locations;
	private StateObjectFactory stateObjectFactory;

	public BundlesAction(File[] locations) {
		this.locations = expandLocations(locations);
		// TODO need to figure a better way of configuring the generator...
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getContext(), PlatformAdmin.class.getName());
		if (platformAdmin != null)
			stateObjectFactory = platformAdmin.getFactory();
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		BundleDescription[] bundles = getBundleDescriptions(locations);
		generateBundleIUs(bundles, results, info);
		return Status.OK_STATUS;
	}

	private File[] expandLocations(File[] list) {
		if (list == null)
			return new File[] {};
		ArrayList result = new ArrayList();
		for (int i = 0; i < list.length; i++) {
			File location = list[i];
			if (location.isDirectory()) {
				File[] entries = location.listFiles();
				for (int j = 0; j < entries.length; j++)
					result.add(entries[j]);
			} else {
				result.add(location);
			}
		}
		return (File[]) result.toArray(new File[result.size()]);
	}

	protected void generateBundleIUs(BundleDescription[] bundles, IPublisherResult result, IPublisherInfo info) {
		// Computing the path for localized property files in a NL fragment bundle
		// requires the BUNDLE_LOCALIZATION property from the manifest of the host bundle,
		// so a first pass is done over all the bundles to cache this value as well as the tags
		// from the manifest for the localizable properties.
		final int CACHE_PHASE = 0;
		final int GENERATE_PHASE = 1;
		final int BUNDLE_LOCALIZATION_INDEX = MetadataGeneratorHelper.BUNDLE_LOCALIZATION_INDEX;
		Map bundleLocalizationMap = new HashMap(bundles.length);
		Set localizationIUs = new HashSet(32);
		for (int phase = CACHE_PHASE; phase <= GENERATE_PHASE; phase++) {
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription bd = bundles[i];
				// A bundle may be null if the associated plug-in does not have a manifest file -
				// for example, org.eclipse.jdt.launching.j9
				if (bd != null && bd.getSymbolicName() != null && bd.getVersion() != null) {
					Map bundleManifest = (Map) bd.getUserObject();

					if (phase == CACHE_PHASE) {
						if (bundleManifest != null) {
							String[] cachedValues = MetadataGeneratorHelper.getManifestCachedValues(bundleManifest);
							bundleLocalizationMap.put(makeSimpleKey(bd), cachedValues);
						}
					} else {
						IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bd.getSymbolicName(), bd.getVersion().toString());
						IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(key, new File(bd.getLocation()), true, false);
						IArtifactRepository destination = info.getArtifactRepository();
						// don't consider any advice here as we want to know about the real form on disk
						if (isDir(bd, null))
							publishArtifact(ad, new File(bd.getLocation()), new File(bd.getLocation()).listFiles(), info, INCLUDE_ROOT);
						else
							publishArtifact(ad, new File(bd.getLocation()), new File[] {new File(bd.getLocation())}, info, AS_IS | INCLUDE_ROOT);

						boolean isDir = isDir(bd, (IBundleAdvice) info.getAdvice(IBundleAdvice.ID));
						IInstallableUnit bundleIU = MetadataGeneratorHelper.createBundleIU(bd, bundleManifest, isDir, key, localizationIUs);

						if (isFragment(bd)) {
							// TODO: Can NL fragments be multi-host?  What special handling
							//		 is required for multi-host fragments in general?
							String hostId = bd.getHost().getName();
							String hostKey = makeSimpleKey(hostId);
							String[] cachedValues = (String[]) bundleLocalizationMap.get(hostKey);

							if (cachedValues != null) {
								MetadataGeneratorHelper.createHostLocalizationFragments(bd, hostId, cachedValues, localizationIUs);
							}
						}

						result.addIU(bundleIU, IPublisherResult.ROOT);
						result.addIUs(localizationIUs, IPublisherResult.NON_ROOT);
						localizationIUs.clear();
					}
				}
			}
		}
	}

	private boolean isDir(BundleDescription bundle, IBundleAdvice advice) {
		// if the advice has a shape, use it
		if (advice != null) {
			String shape = advice.getShape(bundle.getSymbolicName(), bundle.getVersion().toString());
			if (shape != null)
				return shape.equals(IBundleAdvice.DIR);
		}
		// otherwise go with whatever we figured out from the manifest or the shape on disk
		Map manifest = (Map) bundle.getUserObject();
		String format = (String) manifest.get(BundleDescriptionFactory.BUNDLE_SHAPE);
		return BundleDescriptionFactory.DIR.equals(format);
	}

	private String makeSimpleKey(BundleDescription bd) {
		// TODO: can't use the bundle version in the key for the BundleLocalization
		//		 property map since the host specification for a fragment has a
		//		 version range, not a version. Hence, this mechanism for finding
		// 		 manifest localization property files may break under changes
		//		 to the BundleLocalization property of a bundle.
		return makeSimpleKey(bd.getSymbolicName() /*, bd.getVersion() */);
	}

	private String makeSimpleKey(String id /*, Version version */) {
		return id; // + '_' + version.toString();
	}

	private boolean isFragment(BundleDescription bd) {
		return (bd.getHost() != null ? true : false);
	}

	protected BundleDescription[] getBundleDescriptions(File[] bundleLocations) {
		if (bundleLocations == null)
			return new BundleDescription[0];
		boolean addSimpleConfigurator = false;
		boolean scIn = false;
		for (int i = 0; i < bundleLocations.length; i++) {
			if (!addSimpleConfigurator)
				addSimpleConfigurator = bundleLocations[i].toString().indexOf(ORG_ECLIPSE_UPDATE_CONFIGURATOR) > 0;
			if (!scIn) {
				scIn = bundleLocations[i].toString().indexOf(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR) > 0;
				if (scIn)
					break;
			}
		}
		if (scIn)
			addSimpleConfigurator = false;
		BundleDescription[] result = new BundleDescription[bundleLocations.length + (addSimpleConfigurator ? 1 : 0)];
		BundleDescriptionFactory factory = getBundleFactory();
		for (int i = 0; i < bundleLocations.length; i++) {
			result[i] = factory.getBundleDescription(bundleLocations[i]);
		}
		if (addSimpleConfigurator) {
			//Add simple configurator to the list of bundles
			try {
				File location = new File(FileLocator.toFileURL(Activator.getContext().getBundle().getEntry(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR + ".jar")).getFile()); //$NON-NLS-1$
				result[result.length - 1] = factory.getBundleDescription(location);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	protected BundleDescriptionFactory getBundleFactory() {
		return new BundleDescriptionFactory(stateObjectFactory, null);
	}

}
