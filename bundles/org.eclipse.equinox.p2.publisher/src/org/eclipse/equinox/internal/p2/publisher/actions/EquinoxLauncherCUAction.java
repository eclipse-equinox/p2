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
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * Create fragments for all Equinox launcher IUs (not fragments) such that the corresponding
 * host IU is configured as the launch.library.  
 */
public class EquinoxLauncherCUAction extends FragmentIUsAction {

	public static final String ORG_ECLIPSE_EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	public EquinoxLauncherCUAction(IPublisherInfo info, GeneratorBundleInfo[] bundles, String flavor) {
		super(info, null, flavor);
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		bundles = createLauncherBundleInfos(results);
		return super.perform(info, results);
	}

	/**
	 * Returns the set of all known IUs in the result that are part of the Equinox launcher. 
	 * @param results the resutls to scan
	 * @return the Equinox launcher IUs
	 */
	private GeneratorBundleInfo[] createLauncherBundleInfos(IPublisherResult results) {
		Collection result = new HashSet();
		Collection launchers = getIUs(results.getIUs(null, null), "org.eclipse.equinox.launcher."); //$NON-NLS-1$
		for (Iterator iterator = launchers.iterator(); iterator.hasNext();) {
			IInstallableUnit object = (IInstallableUnit) iterator.next();
			// skip over source bundles and fragments
			// TODO should we use the source property here rather than magic name matching?
			if (object.getId().endsWith(".source") || object.isFragment()) //$NON-NLS-1$
				continue;
			GeneratorBundleInfo temp = new GeneratorBundleInfo();
			temp.setSymbolicName(object.getId());
			temp.setVersion(object.getVersion().toString());
			temp.setSpecialConfigCommands("addProgramArg(programArg:--launcher.library);addProgramArg(programArg:@artifact);"); //$NON-NLS-1$
			temp.setSpecialUnconfigCommands("removeProgramArg(programArg:--launcher.library);removeProgramArg(programArg:@artifact);"); //$NON-NLS-1$
			result.add(temp);
		}
		return (GeneratorBundleInfo[]) result.toArray(new GeneratorBundleInfo[result.size()]);
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
