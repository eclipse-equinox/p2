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
	//	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	//	private static final String ORG_ECLIPSE_EQUINOX_P2_RECONCILER_DROPINS = "org.eclipse.equinox.p2.reconciler.dropins"; //$NON-NLS-1$

	protected String source;
	protected String id;
	protected String version = "1.0.0"; //$NON-NLS-1$
	protected String name;
	protected String flavor;
	protected String[] topLevel;
	protected IPublisherInfo info;
	protected String[] nonRootFiles;
	protected boolean start = false;

	protected EclipseInstallAction() {
	}

	public EclipseInstallAction(String source, String id, String version, String name, String flavor, String[] topLevel, String[] nonRootFiles, boolean start) {
		this.source = source;
		this.id = id;
		this.version = version;
		this.name = name == null ? id : name;
		this.flavor = flavor;
		this.topLevel = topLevel;
		this.nonRootFiles = nonRootFiles;
		this.start = start;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		this.info = info;
		IPublishingAction[] actions = createActions();
		for (int i = 0; i < actions.length; i++)
			actions[i].perform(info, results);
		return Status.OK_STATUS;
	}

	protected IPublishingAction[] createActions() {
		createAdvice();
		ArrayList result = new ArrayList();
		// create an action that just publishes the raw bundles and features
		IPublishingAction action = new MergeResultsAction(new IPublishingAction[] {createFeaturesAction(), createBundlesAction()}, IPublisherResult.MERGE_ALL_NON_ROOT);
		result.add(action);
		result.addAll(createExecutablesActions(info.getConfigurations()));
		result.add(createRootFilesAction());
		result.add(createEquinoxLauncherFragmentsAction());
		result.addAll(createAccumulateConfigDataActions(info.getConfigurations()));
		result.add(createJREAction());
		result.add(createConfigCUsAction());
		result.add(createDefaultCUsAction());
		result.add(createRootIUAction());
		return (IPublishingAction[]) result.toArray(new IPublishingAction[result.size()]);
	}

	private void createAdvice() {
		createRootFilesAdvice();
		createRootAdvice();
	}

	protected void createRootAdvice() {
		info.addAdvice(new RootIUAdvice(getTopLevel()));
	}

	protected IPublishingAction createDefaultCUsAction() {
		return new DefaultCUsAction(info, flavor, 4, start);
	}

	protected IPublishingAction createRootIUAction() {
		return new RootIUAction(id, version, name, info);
	}

	protected Collection getTopLevel() {
		return Arrays.asList(topLevel);
	}

	protected IPublishingAction createJREAction() {
		return new JREAction(info, null);
	}

	protected IPublishingAction createEquinoxLauncherFragmentsAction() {
		return new EquinoxLauncherCUAction(flavor);
	}

	protected Collection createAccumulateConfigDataActions(String[] configs) {
		Collection result = new ArrayList(configs.length);
		for (int i = 0; i < configs.length; i++) {
			String configSpec = configs[i];
			File configuration = computeConfigurationLocation(configSpec);
			String os = AbstractPublishingAction.parseConfigSpec(configSpec)[1];
			File executable = ExecutablesDescriptor.findExecutable(os, computeExecutableLocation(configSpec), "eclipse"); //$NON-NLS-1$
			IPublishingAction action = new AccumulateConfigDataAction(info, configSpec, configuration, executable);
			result.add(action);
		}
		return result;
	}

	protected IPublishingAction createConfigCUsAction() {
		return new ConfigCUsAction(info, flavor, id, version);
	}

	protected IPublishingAction createFeaturesAction() {
		return new FeaturesAction(new File[] {new File(source, "features")}); //$NON-NLS-1$
	}

	protected Collection createExecutablesActions(String[] configSpecs) {
		Collection result = new ArrayList(configSpecs.length);
		for (int i = 0; i < configSpecs.length; i++) {
			ExecutablesDescriptor executables = computeExecutables(configSpecs[i]);
			IPublishingAction action = new EquinoxExecutableAction(executables, configSpecs[i], id, version, flavor);
			result.add(action);
		}
		return result;
	}

	protected IPublishingAction createRootFilesAction() {
		return new RootFilesAction(info, id, version, flavor);
	}

	protected void createRootFilesAdvice() {
		File[] baseExclusions = computeRootFileExclusions();
		if (baseExclusions != null)
			info.addAdvice(new RootFilesAdvice(null, null, baseExclusions, null));
		String[] configs = info.getConfigurations();
		for (int i = 0; i < configs.length; i++)
			info.addAdvice(computeRootFileAdvice(configs[i]));
	}

	protected IPublishingAdvice computeRootFileAdvice(String configSpec) {
		File root = computeRootFileRoot(configSpec);
		File[] inclusions = computeRootFileInclusions(configSpec);
		File[] exclusions = computeRootFileExclusions(configSpec);
		return new RootFilesAdvice(root, inclusions, exclusions, configSpec);
	}

	protected File[] computeRootFileExclusions(String configSpec) {
		return computeExecutables(configSpec).getFiles();
	}

	protected File[] computeRootFileExclusions() {
		if (nonRootFiles == null || nonRootFiles.length == 0)
			return null;
		ArrayList result = new ArrayList();
		for (int i = 0; i < nonRootFiles.length; i++) {
			String filename = nonRootFiles[i];
			File file = new File(filename);
			if (file.isAbsolute())
				result.add(file);
			else
				result.add(new File(source, filename));
		}
		return (File[]) result.toArray(new File[result.size()]);
	}

	protected ExecutablesDescriptor computeExecutables(String configSpec) {
		String os = AbstractPublishingAction.parseConfigSpec(configSpec)[1];
		return ExecutablesDescriptor.createDescriptor(os, "eclipse", computeExecutableLocation(configSpec)); //$NON-NLS-1$
	}

	protected File computeRootFileRoot(String configSpec) {
		return new File(source);
	}

	protected File[] computeRootFileInclusions(String configSpec) {
		return new File[] {new File(source)};
	}

	protected File computeExecutableLocation(String configSpec) {
		return new File(source);
	}

	protected File computeConfigurationLocation(String configSpec) {
		return new File(source, "configuration/config.ini"); //$NON-NLS-1$
	}

	protected IPublishingAction createBundlesAction() {
		// TODO need to add in the simple configorator and reconciler bundle descriptions.
		// TODO bundles action needs to take bundleDescriptions directly rather than just files.
		return new BundlesAction(new File[] {new File(source, "plugins")}); //$NON-NLS-1$
	}

	//TODO reconsitute these methods
	//	private GeneratorBundleInfo createSimpleConfiguratorBundleInfo() {
	//		GeneratorBundleInfo result = new GeneratorBundleInfo();
	//		result.setSymbolicName(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR);
	//		result.setVersion("0.0.0"); //$NON-NLS-1$
	//		result.setStartLevel(1);
	//		result.setMarkedAsStarted(true);
	//		return result;
	//	}
	//
	//	private GeneratorBundleInfo createDropinsReconcilerBundleInfo() {
	//		GeneratorBundleInfo result = new GeneratorBundleInfo();
	//		result.setSymbolicName(ORG_ECLIPSE_EQUINOX_P2_RECONCILER_DROPINS);
	//		result.setVersion("0.0.0"); //$NON-NLS-1$
	//		result.setMarkedAsStarted(true);
	//		result.setSpecialConfigCommands("mkdir(path:${installFolder}/dropins)"); //$NON-NLS-1$
	//		result.setSpecialUnconfigCommands("rmdir(path:${installFolder}/dropins)"); //$NON-NLS-1$
	//		return result;
	//	}

}
