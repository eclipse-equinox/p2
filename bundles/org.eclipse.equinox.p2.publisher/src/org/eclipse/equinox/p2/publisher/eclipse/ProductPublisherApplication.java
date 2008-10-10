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
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.eclipse.equinox.p2.publisher.*;

public class ProductPublisherApplication extends AbstractPublisherApplication {

	private String product;
	private String executables;
	private String flavor;

	public ProductPublisherApplication() {
	}

	protected IPublisherAction[] createActions() {
		ArrayList result = new ArrayList();
		result.add(createProductAction());
		return (IPublisherAction[]) result.toArray(new IPublisherAction[result.size()]);
	}

	private IPublisherAction createProductAction() {
		return new ProductAction(source, product, flavor, new File(executables));
	}

	protected void processParameter(String arg, String parameter, PublisherInfo info) throws URISyntaxException {
		super.processParameter(arg, parameter, info);

		if (arg.equalsIgnoreCase("-productFile")) //$NON-NLS-1$
			product = parameter;
		if (arg.equalsIgnoreCase("-executables")) //$NON-NLS-1$
			executables = parameter;
		if (arg.equalsIgnoreCase("-flavor")) //$NON-NLS-1$
			flavor = parameter;
	}

}
