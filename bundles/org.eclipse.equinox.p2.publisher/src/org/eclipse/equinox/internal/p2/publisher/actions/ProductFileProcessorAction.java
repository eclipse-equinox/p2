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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.features.ProductFile;

/**
 * Create the CUs that represent the configuration information found in a product file.
 * This action does not create the root product IU that pulls it all together.
 */
public class ProductFileProcessorAction extends AbstractPublishingAction {

	private ProductFile product = null;
	protected String[] configSpecs;

	public ProductFileProcessorAction(String productLocation, String flavor, IPublisherInfo info) {
		// create the object and fill in the id and version after parsing the product file
		try {
			product = new ProductFile(productLocation, null);
		} catch (Exception e) {
			//TODO
		}
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		for (int i = 0; i < configSpecs.length; i++)
			generateConfigAdvice(configSpecs[i], info);
		return Status.OK_STATUS;
	}

	private void generateConfigAdvice(String configSpec, IPublisherInfo info) {
		info.addAdvice(new ProductAdvice(product, configSpec));
	}

}
