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
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;

public class EclipseInstallAction implements IPublishingAction {
	protected String source;
	protected String id;
	protected String version = "1.0.0"; //$NON-NLS-1$
	protected String name;
	protected String flavor;
	protected String[] topLevel;
	protected IPublisherInfo info;
	protected String[] configurations;
	protected String[] nonRootFiles;

	public EclipseInstallAction(String source, String id, String version, String name, String flavor, String[] topLevel, String[] configurations, String[] nonRootFiles) {
		this.source = source;
		this.id = id;
		this.version = version;
		this.name = name == null ? id : name;
		this.flavor = flavor;
		this.topLevel = topLevel;
		this.configurations = configurations;
		this.nonRootFiles = nonRootFiles;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		this.info = info;
		IPublishingAction[] actions = createActions();
		for (int i = 0; i < actions.length; i++)
			actions[i].perform(info, results);
		return Status.OK_STATUS;
	}

	protected IPublishingAction[] createActions() {
		ArrayList result = new ArrayList();
		// create an action that just publishes the raw bundles and features
		IPublishingAction action = new MergeResultsAction(new IPublishingAction[] {createFeaturesAction(), createBundlesAction()}, IPublisherResult.MERGE_ALL_NON_ROOT);
		result.add(action);
		result.addAll(createEquinoxExecutableActions(configurations));
		result.addAll(createRootFilesActions(configurations));
		result.add(createEquinoxLauncherFragmentsAction());
		result.addAll(createAccumulateConfigDataActions(configurations));
		result.add(createJREAction());
		result.add(createConfigIUsAction());
		result.add(createDefaultIUsAction());
		result.add(createRootIUAction());
		return (IPublishingAction[]) result.toArray(new IPublishingAction[result.size()]);
	}

	protected IPublishingAction createDefaultIUsAction() {
		return new DefaultCUsAction(info, flavor);
	}

	protected IPublishingAction createRootIUAction() {
		return new RootIUAction(id, version, name, topLevel, info);
	}

	protected IPublishingAction createJREAction() {
		return new JREAction(info, null);
	}

	protected IPublishingAction createEquinoxLauncherFragmentsAction() {
		// TODO fillin real values here
		return new EquinoxLauncherCUAction(info, null, flavor);
	}

	protected Collection createAccumulateConfigDataActions(String[] configs) {
		Collection result = new ArrayList(configs.length);
		for (int i = 0; i < configs.length; i++) {
			File configuration = computeConfigurationLocation(configs[i]);
			File executable = computeExecutables(configs[i])[0];
			IPublishingAction action = new AccumulateConfigDataAction(info, configs[i], configuration, executable);
			result.add(action);
		}
		return result;
	}

	protected IPublishingAction createConfigIUsAction() {
		return new ConfigCUsAction(info, flavor, id, version, ConfigCUsAction.CONFIG_INI);
	}

	protected IPublishingAction createFeaturesAction() {
		return new FeaturesAction(new File[] {new File(source, "features")}, info); //$NON-NLS-1$
	}

	protected Collection createEquinoxExecutableActions(String[] configs) {
		Collection result = new ArrayList(configs.length);
		for (int i = 0; i < configs.length; i++) {
			File[] executables = computeExecutables(configs[i]);
			IPublishingAction action = new EquinoxExecutableAction(info, executables, configs[i], id, version, flavor);
			result.add(action);
		}
		return result;
	}

	protected Collection createRootFilesActions(String[] configs) {
		Collection result = new ArrayList(configs.length);
		for (int i = 0; i < configs.length; i++) {
			File[] exclusions = computeRootFileExclusions(configs[i]);
			IPublishingAction action = new RootFilesAction(info, computeRootFileLocation(configs[i]), exclusions, configs[i], id, version, flavor);
			result.add(action);
		}
		return result;
	}

	protected File[] computeRootFileExclusions(String configSpec) {
		ArrayList result = new ArrayList();
		result.addAll(Arrays.asList(computeExecutables(configSpec)));
		if (nonRootFiles != null) {
			for (int i = 0; i < nonRootFiles.length; i++) {
				String filename = nonRootFiles[i];
				File file = new File(filename);
				if (file.isAbsolute())
					result.add(file);
				else
					result.add(new File(source, filename));
			}
		}
		return (File[]) result.toArray(new File[result.size()]);
	}

	protected File[] computeExecutables(String configSpec) {
		String os = AbstractPublishingAction.parseConfigSpec(configSpec)[1];
		return EquinoxExecutableAction.findExecutables(computeExecutableLocation(configSpec), os, "eclipse");
	}

	protected File computeRootFileLocation(String configSpec) {
		return new File(source);
	}

	protected File computeExecutableLocation(String configSpec) {
		return new File(source);
	}

	protected File computeConfigurationLocation(String configSpec) {
		return new File(source, "configuration"); //$NON-NLS-1$
	}

	protected IPublishingAction createBundlesAction() {
		return new BundlesAction(new File[] {new File(source, "plugins")}); //$NON-NLS-1$
	}

}
