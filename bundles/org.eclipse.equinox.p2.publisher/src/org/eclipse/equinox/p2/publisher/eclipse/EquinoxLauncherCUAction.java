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
package org.eclipse.equinox.p2.publisher.eclipse;

import org.eclipse.equinox.internal.provisional.p2.core.Version;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.GeneratorBundleInfo;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.IVersionAdvice;

/**
 * Create CUs for all Equinox launcher related IUs for the given set of configurations
 * such that the launcher is configured as the startup code and the fragments
 * are configured as the launcher.library.  
 * <p>
 * This action expects to have find the versions of the launcher and launcher fragments
 * via IVersionAdvice in the supplied info object.
 * </p>
 */
public class EquinoxLauncherCUAction extends AbstractPublisherAction {

	public static final String ORG_ECLIPSE_EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	private String flavor;
	private String[] configSpecs;

	public EquinoxLauncherCUAction(String flavor, String[] configSpecs) {
		this.flavor = flavor;
		this.configSpecs = configSpecs;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		publishCU(ORG_ECLIPSE_EQUINOX_LAUNCHER, null, info, results);
		publishLauncherFragmentCUs(info, results);
		return Status.OK_STATUS;
	}

	/**
	 * For each of the configurations we are publishing, create a launcher fragment
	 * CU if there is version advice for the fragment.
	 */
	private void publishLauncherFragmentCUs(IPublisherInfo info, IPublisherResult results) {
		for (int i = 0; i < configSpecs.length; i++) {
			String configSpec = configSpecs[i];
			String id = ORG_ECLIPSE_EQUINOX_LAUNCHER + '.' + configSpec;
			publishCU(id, configSpec, info, results);
		}
	}

	/**
	 * Publish a CU for the IU of the given id in the given config spec.  If the IU is the 
	 * launcher bundle iu then set it up as the startup JAR.  If it is a launcher fragment then 
	 * configure it in as the launcher.library for this configuration.
	 */
	private void publishCU(String id, String configSpec, IPublisherInfo info, IPublisherResult results) {
		Collection advice = info.getAdvice(configSpec, true, id, null, IVersionAdvice.class);
		for (Iterator j = advice.iterator(); j.hasNext();) {
			IVersionAdvice versionSpec = (IVersionAdvice) j.next();
			Version version = versionSpec.getVersion(IInstallableUnit.NAMESPACE_IU_ID, id);
			if (version == null)
				continue;
			GeneratorBundleInfo bundle = new GeneratorBundleInfo();
			bundle.setSymbolicName(id);
			bundle.setVersion(version.toString());
			if (id.equals(ORG_ECLIPSE_EQUINOX_LAUNCHER)) {
				bundle.setSpecialConfigCommands("addProgramArg(programArg:-startup);addProgramArg(programArg:@artifact);"); //$NON-NLS-1$
				bundle.setSpecialUnconfigCommands("removeProgramArg(programArg:-startup);removeProgramArg(programArg:@artifact);"); //$NON-NLS-1$
			} else {
				bundle.setSpecialConfigCommands("addProgramArg(programArg:--launcher.library);addProgramArg(programArg:@artifact);"); //$NON-NLS-1$
				bundle.setSpecialUnconfigCommands("removeProgramArg(programArg:--launcher.library);removeProgramArg(programArg:@artifact);"); //$NON-NLS-1$
			}
			String filter = configSpec == null ? null : createFilterSpec(configSpec);
			IInstallableUnit cu = BundlesAction.createBundleConfigurationUnit(id, version, false, bundle, flavor, filter);
			if (cu != null)
				results.addIU(cu, IPublisherResult.ROOT);
		}
	}
}
