/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.VersionedName;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.*;

public class ProductAction extends AbstractPublisherAction {
	protected String source;
	protected String id;
	protected Version version;
	protected String name;
	protected String executableName;
	protected String flavor;
	protected boolean start = false;
	//protected String productLocation;
	protected File executablesFeatureLocation;
	protected IProductDescriptor product;

	public ProductAction(String source, IProductDescriptor product, String flavor, File executablesFeatureLocation) {
		super();
		this.source = source;
		this.flavor = flavor;
		this.executablesFeatureLocation = executablesFeatureLocation;
		this.product = product;
		//this.productLocation = productLocation;
	}

	protected IPublisherAction[] createActions() {
		// generate the advice we can up front.
		createAdvice();

		// create all the actions needed to publish a product
		ArrayList actions = new ArrayList();
		// products include the executable so add actions to publish them
		if (getExecutablesLocation() != null)
			actions.add(createApplicationExecutableAction(info.getConfigurations()));
		// add the actions that just configure things.
		actions.add(createConfigCUsAction());
		actions.add(createDefaultCUsAction());
		actions.add(createRootIUAction());
		return (IPublisherAction[]) actions.toArray(new IPublisherAction[actions.size()]);
	}

	protected IPublisherAction createApplicationExecutableAction(String[] configSpecs) {
		return new ApplicationLauncherAction(id, version, flavor, executableName, getExecutablesLocation(), configSpecs);
	}

	protected IPublisherAction createDefaultCUsAction() {
		return new DefaultCUsAction(info, flavor, 4, false);
	}

	protected IPublisherAction createRootIUAction() {
		return new RootIUAction(id, version, name);
	}

	protected IPublisherAction createConfigCUsAction() {
		return new ConfigCUsAction(info, flavor, id, version);
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		monitor = SubMonitor.convert(monitor);
		this.info = info;
		IPublisherAction[] actions = createActions();
		MultiStatus finalStatus = new MultiStatus(EclipseInstallAction.class.getName(), 0, "publishing result", null); //$NON-NLS-1$
		for (int i = 0; i < actions.length; i++) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			finalStatus.merge(actions[i].perform(info, results, monitor));
		}
		if (!finalStatus.isOK())
			return finalStatus;
		return Status.OK_STATUS;
	}

	private void createAdvice() {
		executableName = product.getLauncherName();
		createProductAdvice();
		createAdviceFileAdvice();
		createRootAdvice();
		info.addAdvice(new RootIUResultFilterAdvice(null));
	}

	/**
	 * Create advice for a p2.inf file co-located with the product file, if any.
	 */
	private void createAdviceFileAdvice() {
		File productFileLocation = product.getLocation();
		if (productFileLocation == null)
			return;
		info.addAdvice(new AdviceFileAdvice(product.getId(), new Version(product.getVersion()), new Path(productFileLocation.getParent()), new Path("p2.inf"))); //$NON-NLS-1$

	}

	private void createRootAdvice() {
		Collection list;
		if (product.useFeatures())
			list = listElements(product.getFeatures(), ".feature.group"); //$NON-NLS-1$
		else
			list = listElements(product.getBundles(true), null);
		info.addAdvice(new RootIUAdvice(list));
	}

	private void createProductAdvice() {
		id = product.getId();
		version = new Version(product.getVersion());
		name = product.getId();

		String[] configSpecs = info.getConfigurations();
		for (int i = 0; i < configSpecs.length; i++)
			info.addAdvice(new ProductFileAdvice(product, configSpecs[i]));
	}

	private Collection listElements(List elements, String suffix) {
		if (suffix == null || suffix.length() == 0)
			return elements;
		ArrayList result = new ArrayList(elements.size());
		for (Iterator i = elements.iterator(); i.hasNext();) {
			VersionedName name = (VersionedName) i.next();
			result.add(new VersionedName(name.getId() + suffix, name.getVersion()));
		}
		return result;
	}

	protected File getExecutablesLocation() {
		if (executablesFeatureLocation != null)
			return executablesFeatureLocation;
		if (source != null)
			return new File(source);
		return null;
	}
}
