/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;

public class StateResolverUtils {

	public static Bundle[] getAdditionalRefresh(Collection currentResolved, BundleContext context) {
		ServiceReference ref = context.getServiceReference(PlatformAdmin.class.getName());
		if (ref == null)
			return new Bundle[0];
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(ref);
		if (platformAdmin == null)
			return new Bundle[0];
		try {
			State state = platformAdmin.getState(false);
			BundleDescription[] bundles = state.getBundles();
			HashSet results = new HashSet(bundles.length);
			getAdditionRefresh(bundles, state, currentResolved, results, context);
			return (Bundle[]) results.toArray(new Bundle[results.size()]);
		} finally {
			context.ungetService(ref);
		}
	}

	private static void getAdditionRefresh(BundleDescription[] bundleDescriptions, State state, Collection currentResolved, Set results, BundleContext context) {
		bundles: for (int i = 0; i < bundleDescriptions.length; i++) {
			Bundle bundle = context.getBundle(bundleDescriptions[i].getBundleId());
			if (bundle == null)
				continue bundles;
			// look for a fragment which adds a conflicted constraint to an already resolved host
			if (!bundleDescriptions[i].isResolved() && bundleDescriptions[i].getHost() != null) {
				ResolverError[] errors = state.getResolverErrors(bundleDescriptions[i]);
				for (int j = 0; j < errors.length; j++) {
					if ((errors[j].getType() & ResolverError.FRAGMENT_CONFLICT) != 0) {
						BundleDescription[] possibleHosts = state.getBundles(bundleDescriptions[i].getHost().getName());
						for (int k = 0; k < possibleHosts.length; k++) {
							Bundle hostBundle = context.getBundle(possibleHosts[k].getBundleId());
							if (hostBundle != null && currentResolved.contains(hostBundle) && bundleDescriptions[i].getHost().isSatisfiedBy(possibleHosts[k]))
								results.add(hostBundle);
						}
					}
				}
				continue bundles;
			}
			if (!currentResolved.contains(bundle) || !bundleDescriptions[i].isResolved())
				continue bundles;
			// look for optional imports which are unresolved but are resolvable
			ImportPackageSpecification[] imports = bundleDescriptions[i].getImportPackages();
			for (int j = 0; j < imports.length; j++)
				if (ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(imports[j].getDirective(Constants.RESOLUTION_DIRECTIVE)) && !imports[j].isResolved() && state.getStateHelper().isResolvable(imports[j])) {
					results.add(bundle);
					continue bundles;
				}
			// look for optional requires which are unresolved but are resolvable
			BundleSpecification[] requires = bundleDescriptions[i].getRequiredBundles();
			for (int j = 0; j < requires.length; j++)
				if (requires[j].isOptional() && !requires[j].isResolved() && state.getStateHelper().isResolvable(requires[j])) {
					results.add(bundle);
					continue bundles;
				}
		}
	}
}
