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
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.actions.MergeResultsAction;
import org.eclipse.equinox.p2.publisher.actions.RootIUAdvice;

public class ProductAction extends EclipseInstallAction {

	private String productLocation;
	private File executablesFeatureLocation;

	public ProductAction(String source, String productLocation, String flavor, File executablesFeatureLocation) {
		super();
		this.source = source;
		this.flavor = flavor;
		this.executablesFeatureLocation = executablesFeatureLocation;
		this.productLocation = productLocation;
	}

	protected IPublisherAction[] createActions() {
		// generate the advice we can up front.
		createAdvice();

		// create all the actions needed to publish a product
		ArrayList actions = new ArrayList();
		// if we have a source location then create actions that publishes the bundles 
		// and features as well as the root files, launchers, etc.
		if (source != null)
			actions.add(new MergeResultsAction(new IPublisherAction[] {createFeaturesAction(), createBundlesAction()}, IPublisherResult.MERGE_ALL_NON_ROOT));
		// products include the executable so add actions to publish them
		actions.add(createApplicationExecutableAction(info.getConfigurations()));
		// add the actions that just configure things.
		actions.add(createJREAction());
		actions.add(createConfigCUsAction());
		actions.add(createDefaultCUsAction());
		actions.add(createRootIUAction());
		return (IPublisherAction[]) actions.toArray(new IPublisherAction[actions.size()]);
	}

	private ProductFile loadProduct() {
		ProductFile product = null;
		try {
			product = new ProductFile(productLocation);
		} catch (Exception e) {
			//TODO
		}
		if (product == null)
			throw new IllegalArgumentException("unable to load product file"); //$NON-NLS-1$
		return product;
	}

	private void createAdvice() {
		ProductFile product = loadProduct();
		executableName = product.getLauncherName();
		createProductAdvice(product);
		createAdviceFileAdvice(product);
		createRootAdvice(product);
	}

	/**
	 * Create advice for a p2.inf file co-located with the product file, if any.
	 */
	private void createAdviceFileAdvice(ProductFile product) {
		File productFileLocation = product.getLocation();
		if (productFileLocation == null)
			return;
		info.addAdvice(new AdviceFileAdvice(product.getId(), new Version(product.getVersion()), new Path(productFileLocation.getParent()), new Path("p2.inf"))); //$NON-NLS-1$
	}

	protected void createRootAdvice(ProductFile product) {
		Collection list;
		if (product.useFeatures())
			list = listElements(product.getFeatures(), ".feature.group"); //$NON-NLS-1$
		else
			list = listElements(product.getBundles(true), ""); //$NON-NLS-1$
		info.addAdvice(new RootIUAdvice(list));
	}

	private void createProductAdvice(ProductFile product) {
		id = product.getId();
		version = new Version(product.getVersion());
		name = product.getId();

		String[] configSpecs = info.getConfigurations();
		for (int i = 0; i < configSpecs.length; i++)
			info.addAdvice(new ProductFileAdvice(product, configSpecs[i]));
	}

	private Collection listElements(List elements, String suffix) {
		ArrayList result = new ArrayList(elements.size());
		for (Iterator i = elements.iterator(); i.hasNext();)
			result.add(((String) i.next()) + suffix);
		return result;
	}

	protected File getExecutablesLocation() {
		return executablesFeatureLocation == null ? super.getExecutablesLocation() : executablesFeatureLocation;
	}
}
