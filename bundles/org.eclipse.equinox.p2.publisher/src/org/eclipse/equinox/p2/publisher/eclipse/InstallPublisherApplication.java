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

import java.util.ArrayList;
import org.eclipse.equinox.p2.publisher.*;
import org.osgi.framework.Version;

public class InstallPublisherApplication extends AbstractPublisherApplication {

	protected String id;
	protected Version version = new Version("1.0.0"); //$NON-NLS-1$
	protected String name;
	protected String flavor;
	protected String[] topLevel;
	protected boolean start;
	protected String[] rootExclusions;

	public InstallPublisherApplication() {
	}

	protected void processFlag(String arg, PublisherInfo info) {
		super.processFlag(arg, info);

		if (arg.equalsIgnoreCase("-startAll")) //$NON-NLS-1$
			start = true;
	}

	protected void processParameter(String arg, String parameter, PublisherInfo info) {
		super.processParameter(arg, parameter, info);

		if (arg.equalsIgnoreCase("-id")) //$NON-NLS-1$
			id = parameter;

		if (arg.equalsIgnoreCase("-version")) //$NON-NLS-1$
			version = new Version(parameter);

		if (arg.equalsIgnoreCase("-name")) //$NON-NLS-1$
			name = parameter;

		if (arg.equalsIgnoreCase("-flavor")) //$NON-NLS-1$
			flavor = parameter;

		if (arg.equalsIgnoreCase("-top")) //$NON-NLS-1$
			topLevel = AbstractPublisherAction.getArrayFromString(parameter, ",");

		if (arg.equalsIgnoreCase("-rootExclusions")) //$NON-NLS-1$
			rootExclusions = AbstractPublisherAction.getArrayFromString(parameter, ",");
	}

	protected IPublisherAction[] createActions() {
		ArrayList result = new ArrayList();
		result.add(createEclipseInstallAction());
		return (IPublisherAction[]) result.toArray(new IPublisherAction[result.size()]);
	}

	private IPublisherAction createEclipseInstallAction() {
		String[] exclusions = getBaseExclusions();
		if (rootExclusions != null) {
			String[] result = new String[exclusions.length + rootExclusions.length];
			System.arraycopy(exclusions, 0, result, 0, exclusions.length);
			System.arraycopy(rootExclusions, 0, result, exclusions.length, rootExclusions.length);
			exclusions = result;
		}
		return new EclipseInstallAction(source, id, version, name, flavor, topLevel, exclusions, start);
	}

	protected String[] getBaseExclusions() {
		return new String[] {"plugins", "features", "configuration", "p2", "artifacts.xml"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
}
