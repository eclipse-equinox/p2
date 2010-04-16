/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.VersionAdvice;

public class ProductPublisherApplication extends AbstractPublisherApplication {

	private String product;
	private String executables;
	private String flavor;

	public ProductPublisherApplication() {
		//hidden
	}

	protected IPublisherAction[] createActions() {
		ArrayList<IPublisherAction> result = new ArrayList<IPublisherAction>();
		result.add(createProductAction());
		return result.toArray(new IPublisherAction[result.size()]);
	}

	private IPublisherAction createProductAction() {
		IProductDescriptor productDescriptor = null;
		try {
			productDescriptor = new ProductFile(product);
		} catch (Exception e) {
			if (product == null)
				throw new IllegalArgumentException("unable to load product file"); //$NON-NLS-1$
		}
		File executablesFeature = executables == null ? null : new File(executables);
		return new ProductAction(source, productDescriptor, flavor, executablesFeature);
	}

	protected void processParameter(String arg, String parameter, PublisherInfo publisherInfo) throws URISyntaxException {
		super.processParameter(arg, parameter, publisherInfo);

		if (arg.equalsIgnoreCase("-productFile")) //$NON-NLS-1$
			product = parameter;
		if (arg.equalsIgnoreCase("-executables")) //$NON-NLS-1$
			executables = parameter;
		if (arg.equalsIgnoreCase("-flavor")) //$NON-NLS-1$
			flavor = parameter;
		if (arg.equalsIgnoreCase("-pluginVersionsAdvice")) { //$NON-NLS-1$
			VersionAdvice versionAdvice = new VersionAdvice();
			versionAdvice.load(IInstallableUnit.NAMESPACE_IU_ID, parameter, null);
			info.addAdvice(versionAdvice);
		}
		if (arg.equalsIgnoreCase("-featureVersionsAdvice")) { //$NON-NLS-1$
			VersionAdvice versionAdvice = new VersionAdvice();
			versionAdvice.load(IInstallableUnit.NAMESPACE_IU_ID, parameter, ".feature.group"); //$NON-NLS-1$
			info.addAdvice(versionAdvice);
		}
	}
}