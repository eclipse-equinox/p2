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
import java.util.ArrayList;
import org.eclipse.equinox.p2.publisher.*;

/**
 * <p>
 * This application generates meta-data and artifact repositories from a set of features and bundles.
 * If -source <localdir> parameter is given, it specifies the directory under which to find the features 
 * and bundles (in the standard "features" and "plugins" sub-directories).
 * </p><p>
 * Optionally, the -features <csv of file locations> and -bundles <csv of file locations> arguments can 
 * be specified.  If given, these override the defaults derived from a supplied -source parameter.
 * </p>
 */
public class FeaturesAndBundlesPublisherApplication extends AbstractPublisherApplication {

	protected File[] features = null;
	protected File[] bundles = null;

	public FeaturesAndBundlesPublisherApplication() {
		// nothing to do
	}

	protected void processParameter(String arg, String parameter, PublisherInfo pinfo) {
		super.processParameter(arg, parameter, pinfo);

		if (arg.equalsIgnoreCase("-features")) //$NON-NLS-1$
			features = createFiles(parameter);

		if (arg.equalsIgnoreCase("-bundles")) //$NON-NLS-1$
			bundles = createFiles(parameter);
	}

	private File[] createFiles(String parameter) {
		String[] filespecs = AbstractPublisherAction.getArrayFromString(parameter, ","); //$NON-NLS-1$
		File[] result = new File[filespecs.length];
		for (int i = 0; i < filespecs.length; i++)
			result[i] = new File(filespecs[i]);
		return result;
	}

	protected IPublisherAction[] createActions() {
		ArrayList result = new ArrayList();
		if (features == null)
			features = new File[] {new File(source, "features")}; //$NON-NLS-1$
		result.add(new FeaturesAction(features));
		if (bundles == null)
			bundles = new File[] {new File(source, "plugins")}; //$NON-NLS-1$
		result.add(new BundlesAction(bundles));
		return (IPublisherAction[]) result.toArray(new IPublisherAction[result.size()]);
	}
}
