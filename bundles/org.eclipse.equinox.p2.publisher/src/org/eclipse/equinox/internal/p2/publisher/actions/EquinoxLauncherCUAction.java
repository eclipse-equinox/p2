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

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.osgi.framework.Version;

/**
 * Create CUs for all Equinox launcher IUs (not fragments) found in the current results
 * such that the corresponding host IU is configured as the launch.library.  
 */
public class EquinoxLauncherCUAction extends AbstractPublishingAction {

	public static final String ORG_ECLIPSE_EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	private String flavor;

	public EquinoxLauncherCUAction(String flavor) {
		this.flavor = flavor;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		createLauncherCUs(results);
		return Status.OK_STATUS;
	}

	/**
	 * Returns the set of all known IUs in the result that are part of the Equinox launcher. 
	 * @param results the results to scan and supplement
	 */
	private void createLauncherCUs(IPublisherResult results) {
		Collection launchers = getIUs(results.getIUs(null, null), ORG_ECLIPSE_EQUINOX_LAUNCHER);
		for (Iterator i = launchers.iterator(); i.hasNext();) {
			IInstallableUnit launcherIU = (IInstallableUnit) i.next();
			// skip over source bundles and fragments
			// TODO should we use the source property here rather than magic name matching?
			if (launcherIU.getId().endsWith(".source") || launcherIU.isFragment()) //$NON-NLS-1$
				continue;
			GeneratorBundleInfo bundle = new GeneratorBundleInfo();
			bundle.setSymbolicName(launcherIU.getId());
			bundle.setVersion(launcherIU.getVersion().toString());
			bundle.setSpecialConfigCommands("addProgramArg(programArg:-startup);addProgramArg(programArg:@artifact);"); //$NON-NLS-1$
			bundle.setSpecialUnconfigCommands("removeProgramArg(programArg:-startup);removeProgramArg(programArg:@artifact);"); //$NON-NLS-1$
			String filter = launcherIU.getFilter();
			IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, flavor, filter);
			if (cu != null)
				results.addIU(cu, IPublisherResult.ROOT);
		}
	}

	private Collection getIUs(Collection ius, String prefix) {
		Set result = new HashSet();
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit tmp = (IInstallableUnit) iterator.next();
			if (tmp.getId().startsWith(prefix))
				result.add(tmp);
		}
		return result;
	}
}
