/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;

public class FeaturesAndBundlesPublisherTask extends AbstractPublishTask {
	private ArrayList<Object> features = new ArrayList<Object>();
	private ArrayList<Object> bundles = new ArrayList<Object>();

	public void execute() throws BuildException {
		try {
			initializeRepositories(getInfo());
		} catch (ProvisionException e) {
			throw new BuildException("Unable to configure repositories", e); //$NON-NLS-1$
		}

		File[] f = getLocations(features);
		File[] b = getLocations(bundles);

		ArrayList<IPublisherAction> actions = new ArrayList<IPublisherAction>();
		if (f.length > 0)
			actions.add(new FeaturesAction(f));
		if (b.length > 0)
			actions.add(new BundlesAction(b));

		if (actions.size() > 0)
			new Publisher(getInfo()).publish(actions.toArray(new IPublisherAction[actions.size()]), new NullProgressMonitor());
	}

	private File[] getLocations(List<Object> collection) {
		ArrayList<Object> results = new ArrayList<Object>();
		for (Object obj : collection) {
			if (obj instanceof FileSet) {
				FileSet set = (FileSet) obj;

				DirectoryScanner scanner = set.getDirectoryScanner(getProject());
				String[][] elements = new String[][] {scanner.getIncludedDirectories(), scanner.getIncludedFiles()};
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < elements[i].length; j++) {
						results.add(new File(set.getDir(), elements[i][j]));
					}
				}
			} else if (obj instanceof File) {
				results.add(obj);
			}
		}
		return results.toArray(new File[results.size()]);
	}

	public FileSet createFeatures() {
		FileSet set = new FileSet();
		features.add(set);
		return set;
	}

	public FileSet createBundles() {
		FileSet set = new FileSet();
		bundles.add(set);
		return set;
	}

	public void setSource(String source) {
		super.source = source;
		features.add(new File(source, "features")); //$NON-NLS-1$
		bundles.add(new File(source, "plugins")); //$NON-NLS-1$
	}
}
