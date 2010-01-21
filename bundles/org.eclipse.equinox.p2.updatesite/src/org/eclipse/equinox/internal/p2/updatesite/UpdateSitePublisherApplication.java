/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.net.URISyntaxException;
import org.eclipse.equinox.p2.publisher.*;

/**
 * <p>
 * This application generates meta-data/artifact repositories from a local update site.
 * The -source <localdir> parameter must specify the top-level directory containing the update site.
 * </p>
 */
public class UpdateSitePublisherApplication extends AbstractPublisherApplication {

	private String categoryQualifier = null;
	private String categoryVersion = null;

	public UpdateSitePublisherApplication() {
		// nothing todo
	}

	protected void processParameter(String arg, String parameter, PublisherInfo pinfo) throws URISyntaxException {
		super.processParameter(arg, parameter, pinfo);

		if (arg.equalsIgnoreCase("-categoryQualifier")) //$NON-NLS-1$
			categoryQualifier = parameter;

		if (arg.equalsIgnoreCase("-categoryVersion")) //$NON-NLS-1$
			categoryVersion = parameter;
	}

	protected IPublisherAction[] createActions() {
		LocalUpdateSiteAction action = new LocalUpdateSiteAction(source, categoryQualifier);
		action.setCategoryVersion(categoryVersion);
		return new IPublisherAction[] {action};
	}
}
