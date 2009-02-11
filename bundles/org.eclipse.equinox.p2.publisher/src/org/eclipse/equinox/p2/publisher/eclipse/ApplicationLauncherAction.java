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

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.VersionAdvice;

/**
 * Create IUs and CUs that represent the entire launcher for an application.  This includes
 * the executable, the launcher fragments as well as the CUs required to install and configure
 * these elements.
 */
public class ApplicationLauncherAction extends AbstractPublisherAction {

	private String flavor;
	private String[] configSpecs;
	private File location;
	private String executableName;
	private Version version;
	private String id;

	/**
	 * Returns the id of the top level IU published by this action for the given id and flavor.
	 * @param id the id of the application being published
	 * @param flavor the flavor being published
	 * @return the if for ius published by this action
	 */
	public static String computeIUId(String id, String flavor) {
		return flavor + id + ".application"; //$NON-NLS-1$
	}

	public ApplicationLauncherAction(String id, Version version, String flavor, String executableName, File location, String[] configSpecs) {
		this.flavor = flavor;
		this.configSpecs = configSpecs;
		this.id = id;
		this.version = version;
		this.executableName = executableName;
		this.location = location;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		// Create the basic actions and run them putting the IUs in a temporary result
		Collection actions = createActions(info);
		createAdvice(info, results);
		IPublisherResult innerResult = new PublisherResult();
		MultiStatus finalStatus = new MultiStatus(ApplicationLauncherAction.class.getName(), 0, "publishing result", null); //$NON-NLS-1$
		for (Iterator i = actions.iterator(); i.hasNext();) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			finalStatus.merge(((IPublisherAction) i.next()).perform(info, innerResult, monitor));
		}
		if (!finalStatus.isOK())
			return finalStatus;
		// merge the IUs  into the final result as non-roots and create a parent IU that captures them all
		results.merge(innerResult, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishApplicationLauncherIU(innerResult.getIUs(null, IPublisherResult.ROOT), results);
		return Status.OK_STATUS;
	}

	/**
	 * Create advice needed by the actions related to and following this action
	 */
	private void createAdvice(IPublisherInfo info, IPublisherResult results) {
		createLauncherAdvice(info, results);
	}

	/**
	 * Create and register advice that will tell people what versions of the launcher bundle and 
	 * fragments are in use in this particular result.
	 */
	private void createLauncherAdvice(IPublisherInfo info, IPublisherResult results) {
		Collection ius = getIUs(results.getIUs(null, null), EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER);
		VersionAdvice advice = new VersionAdvice();
		boolean found = false;
		for (Iterator i = ius.iterator(); i.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) i.next();
			// skip over source bundles and fragments
			// TODO should we use the source property here rather than magic name matching?
			if (iu.getId().endsWith(".source") || iu.isFragment()) //$NON-NLS-1$
				continue;
			advice.setVersion(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), iu.getVersion());
			found = true;
		}
		if (found)
			info.addAdvice(advice);
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

	private void publishApplicationLauncherIU(Collection children, IPublisherResult result) {
		InstallableUnitDescription descriptor = createParentIU(children, computeIUId(id, flavor), version);
		descriptor.setSingleton(true);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return;
		result.addIU(rootIU, IPublisherResult.ROOT);
	}

	private Collection createActions(IPublisherInfo info) {
		Collection actions = new ArrayList();
		actions.add(new EquinoxLauncherCUAction(flavor, configSpecs));
		actions.addAll(createExecutablesActions(configSpecs));
		return actions;
	}

	protected Collection createExecutablesActions(String[] configSpecs) {
		Collection actions = new ArrayList(configSpecs.length);
		for (int i = 0; i < configSpecs.length; i++) {
			ExecutablesDescriptor executables = computeExecutables(configSpecs[i]);
			IPublisherAction action = new EquinoxExecutableAction(executables, configSpecs[i], id, version, flavor);
			actions.add(action);
		}
		return actions;
	}

	protected ExecutablesDescriptor computeExecutables(String configSpec) {
		// See if we know about an executables feature then use it as the source
		ExecutablesDescriptor result = ExecutablesDescriptor.createExecutablesFromFeature(location, configSpec);
		if (result != null)
			return result;
		// otherwise, assume that we are running against an Eclipse install and do the default thing
		String os = AbstractPublisherAction.parseConfigSpec(configSpec)[1];
		return ExecutablesDescriptor.createDescriptor(os, executableName, location);
	}
}
