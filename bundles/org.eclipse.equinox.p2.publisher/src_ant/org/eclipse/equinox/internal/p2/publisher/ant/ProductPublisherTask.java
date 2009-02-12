/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.ant;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;

public class ProductPublisherTask extends AbstractPublishTask {

	private String flavor;
	private String productFile;
	private String executables;
	private String source;

	public void execute() throws BuildException {
		try {
			initializeRepositories(getInfo());
		} catch (ProvisionException e) {
			throw new BuildException("Unable to configure repositories", e); //$NON-NLS-1$
		}

		IProductDescriptor productDescriptor = null;
		try {
			productDescriptor = new ProductFile(productFile);
		} catch (Exception e) {
			if (productFile == null)
				throw new IllegalArgumentException("unable to load product file"); //$NON-NLS-1$
		}

		if (flavor == null || flavor.startsWith(ANT_PROPERTY_PREFIX))
			flavor = "tooling"; //$NON-NLS-1$

		IPublisherAction action = new ProductAction(source, productDescriptor, flavor, executables != null ? new File(executables) : null);
		new Publisher(getInfo()).publish(new IPublisherAction[] {action}, new NullProgressMonitor());
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public void setProductFile(String productFile) {
		this.productFile = productFile;
	}

	public void setExecutables(String executables) {
		this.executables = executables;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
