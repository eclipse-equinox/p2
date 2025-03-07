/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
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
package org.eclipse.equinox.p2.publisher.eclipse;

import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.p2.publisher.eclipse.GeneratorBundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
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

	private final String flavor;
	private final String[] configSpecs;

	public EquinoxLauncherCUAction(String flavor, String[] configSpecs) {
		this.flavor = flavor;
		this.configSpecs = configSpecs;
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		setPublisherInfo(publisherInfo);
		publishCU(ORG_ECLIPSE_EQUINOX_LAUNCHER, null, results);
		publishLauncherFragmentCUs(results);
		return Status.OK_STATUS;
	}

	/**
	 * For each of the configurations we are publishing, create a launcher fragment
	 * CU if there is version advice for the fragment.
	 */
	private void publishLauncherFragmentCUs(IPublisherResult results) {
		String id = null;
		for (String configSpec : configSpecs) {
			String[] specs = Utils.getTokens(configSpec, "."); //$NON-NLS-1$
			if (specs.length > 0 && !AbstractPublisherAction.CONFIG_ANY.equalsIgnoreCase(specs[0])) {
				id = ORG_ECLIPSE_EQUINOX_LAUNCHER + '.' + configSpec;
				publishCU(id, configSpec, results);
			}
		}
	}

	/**
	 * Publish a CU for the IU of the given id in the given config spec.  If the IU is the 
	 * launcher bundle iu then set it up as the startup JAR.  If it is a launcher fragment then 
	 * configure it in as the launcher.library for this configuration.
	 */
	private void publishCU(String id, String configSpec, IPublisherResult results) {
		Collection<IVersionAdvice> advice = info.getAdvice(configSpec, true, id, null, IVersionAdvice.class);
		for (IVersionAdvice versionSpec : advice) {
			Version version = versionSpec.getVersion(IInstallableUnit.NAMESPACE_IU_ID, id);
			if (version == null) {
				continue;
			}
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
			IMatchExpression<IInstallableUnit> filter = configSpec == null ? null : createFilterSpec(configSpec);
			IInstallableUnit cu = BundlesAction.createBundleConfigurationUnit(id, version, false, bundle, flavor, filter);
			if (cu != null) {
				results.addIU(cu, IPublisherResult.ROOT);
			}
		}
	}
}
