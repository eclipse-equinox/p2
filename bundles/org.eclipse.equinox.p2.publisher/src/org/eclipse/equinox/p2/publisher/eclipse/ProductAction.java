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
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
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
		ArrayList result = new ArrayList();
		// if we have a source location then create actions that publishes the bundles 
		// and features as well as the root files, launchers, etc.
		if (source != null) {
			IPublisherAction action = new MergeResultsAction(new IPublisherAction[] {createFeaturesAction(), createBundlesAction()}, IPublisherResult.MERGE_ALL_NON_ROOT);
			result.add(action);
			result.add(createEquinoxLauncherFragmentsAction());
		}
		// products include the executable so add actions to publish them
		result.addAll(createExecutablesActions(info.getConfigurations()));
		// add the actions that just configure things.
		result.add(createJREAction());
		result.add(createConfigCUsAction());
		result.add(createDefaultCUsAction());
		result.add(createRootIUAction());
		return (IPublisherAction[]) result.toArray(new IPublisherAction[result.size()]);
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
		createProductAdvice(product);
		createRootAdvice(product);
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
		version = product.getVersion();
		name = product.getId();

		String[] configSpecs = info.getConfigurations();
		for (int i = 0; i < configSpecs.length; i++)
			info.addAdvice(new ProductFileAdvice(product, configSpecs[i]));
	}

	protected Collection createExecutablesActions(String[] configSpecs) {
		Collection result = new ArrayList(configSpecs.length);
		for (int i = 0; i < configSpecs.length; i++) {
			ExecutablesDescriptor executables = computeExecutables(configSpecs[i]);
			IPublisherAction action = new BrandedExecutableAction(executables, productLocation, flavor, configSpecs[i]);
			result.add(action);
		}
		return result;
	}

	protected ExecutablesDescriptor computeExecutables(String configSpec) {
		// if we know about the executables feature then use it as the source
		File location = getExecutablesFeatureLocation();
		if (location != null) {
			ExecutablesDescriptor result = ExecutablesDescriptor.createExecutablesFromFeature(location, configSpec);
			if (result != null)
				return result;
		}
		// otherwise, assume that we are running against an Eclipse install and do the default thing
		return super.computeExecutables(configSpec);
	}

	private File getExecutablesFeatureLocation() {
		if (executablesFeatureLocation != null)
			return executablesFeatureLocation;
		if (source == null)
			return null;
		// The executables were not explicitly located but the source was so poke around and see if 
		// we can find something
		// TODO implement this
		return null;
	}

	private Collection listElements(List elements, String suffix) {
		ArrayList result = new ArrayList(elements.size());
		for (Iterator i = elements.iterator(); i.hasNext();)
			result.add(((String) i.next()) + suffix);
		return result;
	}

}
