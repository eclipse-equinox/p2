/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others. All rights reserved. This
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
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.*;

public class EclipseInstallAction extends AbstractPublisherAction {
	protected String source;
	protected String id;
	protected Version version;
	protected String name;
	protected String executableName;
	protected String flavor;
	protected IVersionedId[] topLevel;
	protected String[] nonRootFiles;
	protected boolean start = false;

	protected EclipseInstallAction() {
		//hidden
	}

	public EclipseInstallAction(String source, String id, Version version, String name, String executableName, String flavor, IVersionedId[] topLevel, String[] nonRootFiles, boolean start) {
		this.source = source;
		this.id = id;
		this.version = version;
		this.name = name == null ? id : name;
		this.executableName = executableName == null ? "eclipse" : executableName; //$NON-NLS-1$
		this.flavor = flavor;
		this.topLevel = topLevel;
		this.nonRootFiles = nonRootFiles;
		this.start = start;
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		monitor = SubMonitor.convert(monitor);
		this.info = publisherInfo;
		IPublisherAction[] actions = createActions();
		MultiStatus finalStatus = new MultiStatus(EclipseInstallAction.class.getName(), 0, "publishing result", null); //$NON-NLS-1$
		for (int i = 0; i < actions.length; i++) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			finalStatus.merge(actions[i].perform(publisherInfo, results, monitor));
		}
		if (!finalStatus.isOK())
			return finalStatus;
		return Status.OK_STATUS;
	}

	protected IPublisherAction[] createActions() {
		createAdvice();
		ArrayList<IPublisherAction> actions = new ArrayList<IPublisherAction>();
		// create an action that just publishes the raw bundles and features
		IPublisherAction action = new MergeResultsAction(new IPublisherAction[] {createFeaturesAction(), createBundlesAction()}, IPublisherResult.MERGE_ALL_ROOT);
		actions.add(action);
		actions.add(createApplicationExecutableAction(info.getConfigurations()));
		actions.add(createRootFilesAction());
		actions.addAll(createAccumulateConfigDataActions(info.getConfigurations()));
		actions.add(createJREAction());
		actions.add(createConfigCUsAction());
		actions.add(createDefaultCUsAction());
		actions.add(createRootIUAction());
		return actions.toArray(new IPublisherAction[actions.size()]);
	}

	private void createAdvice() {
		createRootFilesAdvice();
		createRootAdvice();
	}

	protected void createRootAdvice() {
		if (topLevel != null)
			info.addAdvice(new RootIUAdvice(getTopLevel()));
		info.addAdvice(new RootIUResultFilterAdvice(null));
	}

	protected IPublisherAction createDefaultCUsAction() {
		return new DefaultCUsAction(info, flavor, 4, start);
	}

	protected IPublisherAction createRootIUAction() {
		return new RootIUAction(id, version, name);
	}

	protected Collection<IVersionedId> getTopLevel() {
		return Arrays.asList(topLevel);
	}

	protected IPublisherAction createJREAction() {
		return new JREAction((File) null);
	}

	protected IPublisherAction createApplicationExecutableAction(String[] configSpecs) {
		return new ApplicationLauncherAction(id, version, flavor, executableName, getExecutablesLocation(), configSpecs);
	}

	protected Collection<IPublisherAction> createAccumulateConfigDataActions(String[] configs) {
		File configuration = new File(source, "configuration/config.ini"); //$NON-NLS-1$
		if (!configuration.exists())
			configuration = null;

		Collection<IPublisherAction> result = new ArrayList<IPublisherAction>(configs.length);
		for (int i = 0; i < configs.length; i++) {
			String configSpec = configs[i];
			String os = AbstractPublisherAction.parseConfigSpec(configSpec)[1];
			File executable = ExecutablesDescriptor.findExecutable(os, computeExecutableLocation(configSpec), "eclipse"); //$NON-NLS-1$
			if (!executable.exists())
				executable = null;
			IPublisherAction action = new AccumulateConfigDataAction(info, configSpec, configuration, executable);
			result.add(action);
		}

		return result;
	}

	protected IPublisherAction createConfigCUsAction() {
		return new ConfigCUsAction(info, flavor, id, version);
	}

	protected IPublisherAction createFeaturesAction() {
		return new FeaturesAction(new File[] {new File(source, "features")}); //$NON-NLS-1$
	}

	protected Collection<IPublisherAction> createExecutablesActions(String[] configSpecs) {
		Collection<IPublisherAction> result = new ArrayList<IPublisherAction>(configSpecs.length);
		for (int i = 0; i < configSpecs.length; i++) {
			ExecutablesDescriptor executables = computeExecutables(configSpecs[i]);
			IPublisherAction action = new EquinoxExecutableAction(executables, configSpecs[i], id, version, flavor);
			result.add(action);
		}
		return result;
	}

	protected IPublisherAction createRootFilesAction() {
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

	protected IPublisherAdvice computeRootFileAdvice(String configSpec) {
		File root = computeRootFileRoot(configSpec);
		File[] inclusions = computeRootFileInclusions(configSpec);
		File[] exclusions = computeRootFileExclusions(configSpec);
		return new RootFilesAdvice(root, inclusions, exclusions, configSpec);
	}

	protected File[] computeRootFileExclusions(String configSpec) {
		ExecutablesDescriptor executables = computeExecutables(configSpec);
		File[] files = executables.getFiles();
		File[] result = new File[files.length + 1];
		System.arraycopy(files, 0, result, 0, files.length);
		result[files.length] = executables.getIniLocation();
		return result;
	}

	protected File[] computeRootFileExclusions() {
		if (nonRootFiles == null || nonRootFiles.length == 0)
			return null;
		ArrayList<File> result = new ArrayList<File>();
		for (int i = 0; i < nonRootFiles.length; i++) {
			String filename = nonRootFiles[i];
			File file = new File(filename);
			if (file.isAbsolute())
				result.add(file);
			else
				result.add(new File(source, filename));
		}
		return result.toArray(new File[result.size()]);
	}

	protected ExecutablesDescriptor computeExecutables(String configSpec) {
		String os = AbstractPublisherAction.parseConfigSpec(configSpec)[1];
		// TODO here we should not assume that the executable is called "eclipse"
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

	protected File getExecutablesLocation() {
		return new File(source);
	}

	protected IPublisherAction createBundlesAction() {
		// TODO need to add in the simple configorator and reconciler bundle descriptions.
		// TODO bundles action needs to take bundleDescriptions directly rather than just files.
		return new BundlesAction(new File[] {new File(source, "plugins")}); //$NON-NLS-1$
	}
}
