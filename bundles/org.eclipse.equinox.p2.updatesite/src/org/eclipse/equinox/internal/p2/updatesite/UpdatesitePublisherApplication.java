/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import org.eclipse.equinox.p2.publisher.AbstractPublisherApplication;
import org.eclipse.equinox.p2.publisher.IPublisherAction;

/**
 * <p>
 * This application generates meta-data/artifact repositories from a local update site.
 * The -source <localdir> parameter must specify the top-level directory containing the update site.
 * </p>
 */
public class UpdatesitePublisherApplication extends AbstractPublisherApplication {

	public UpdatesitePublisherApplication() {
		// nothing todo
	}

	protected IPublisherAction[] createActions() {
		return new IPublisherAction[] {new LocalUpdateSiteAction(source)};
	}
}
