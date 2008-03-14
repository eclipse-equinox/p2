/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.publisher.actions.EclipseInstallAction;

public class InstallPublisherApplication extends AbstractPublisherApplication {

	protected String id;
	protected String version = "1.0.0"; //$NON-NLS-1$
	protected String name;
	protected String flavor;
	protected String[] topLevel;
	protected String[] configurations;

	public InstallPublisherApplication() {
	}

	protected void processParameter(String arg, String parameter, PublisherInfo info) {
		super.processParameter(arg, parameter, info);

		if (arg.equalsIgnoreCase("-id")) //$NON-NLS-1$
			id = parameter;

		if (arg.equalsIgnoreCase("-version")) //$NON-NLS-1$
			version = parameter;

		if (arg.equalsIgnoreCase("-name")) //$NON-NLS-1$
			name = parameter;

		if (arg.equalsIgnoreCase("-flavor")) //$NON-NLS-1$
			flavor = parameter;

		if (arg.equalsIgnoreCase("-top")) //$NON-NLS-1$
			topLevel = AbstractPublishingAction.getArrayFromString(parameter, ",");

		if (arg.equalsIgnoreCase("-configs")) //$NON-NLS-1$
			configurations = AbstractPublishingAction.getArrayFromString(parameter, ",");
	}

	protected IPublishingAction[] createActions() {
		ArrayList result = new ArrayList();
		result.add(createEclipseInstallAction());
		return (IPublishingAction[]) result.toArray(new IPublishingAction[result.size()]);
	}

	private IPublishingAction createEclipseInstallAction() {
		String[] exclusions = {"plugins", "features", "configuration"};
		return new EclipseInstallAction(source, id, version, name, flavor, topLevel, configurations, exclusions);
	}
}
