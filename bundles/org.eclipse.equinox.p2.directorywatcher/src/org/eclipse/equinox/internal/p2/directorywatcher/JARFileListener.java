/*******************************************************************************
 * Copyright (c) 2007 aQute, IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * aQute - initial implementation and ideas 
 * IBM Corporation - initial adaptation to Equinox provisioning use
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.directorywatcher;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;

public class JARFileListener extends DirectoryChangeListener {
	private boolean refresh = false;
	private Map seenFiles = new HashMap();

	public boolean added(File file) {
		InputStream in;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			return false;
		}
		Bundle bundle;
		try {
			bundle = Activator.getContext().installBundle(file.getAbsolutePath(), in);
		} catch (BundleException e1) {
			return false;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
		}
		refresh = true;
		if (!isFragment(bundle))
			try {
				bundle.start();
			} catch (BundleException e) {
				// TODO ignore for now
			}
		seenFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	private Bundle findBundle(String location) {
		Bundle bundles[] = Activator.getContext().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if (bundle.getLocation().equals(location))
				return bundle;
		}
		return null;
	}

	public boolean changed(File file) {
		Bundle bundle = findBundle(file.getAbsolutePath());
		if (bundle == null)
			//  This is actually a goofy condition since we think this file changed but there
			// is no bundle for it.  Perhaps we found it previously but somehow failed to install 
			// it previously or it was uninstalled or...
			return false;
		InputStream in;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return false;
		}
		try {
			bundle.update(in);
		} catch (BundleException e) {
			return false;
		}
		refresh = true;
		try {
			in.close();
		} catch (IOException e) {
			// ignore
		}
		seenFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	public boolean isInterested(File file) {
		return file.getName().endsWith(".jar");
	}

	public boolean removed(File file) {
		Bundle bundle = findBundle(file.getAbsolutePath());
		if (bundle == null) {
			//  This is actually a goofy condition since we think this file changed but there
			// is no bundle for it.  Perhaps we found it previously but somehow failed to install 
			// it previously or it was uninstalled or... 
			// Anyway, the bundle is gone so say we were successful anyway...
			seenFiles.remove(file);
			return true;
		}
		try {
			bundle.uninstall();
		} catch (BundleException e) {
			return false;
		}
		refresh = true;
		seenFiles.remove(file);
		return true;
	}

	private boolean isFragment(Bundle bundle) {
		PackageAdmin packageAdmin = Activator.getPackageAdmin(10000);
		if (packageAdmin != null)
			return packageAdmin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
		return false;
	}

	public void startPoll() {
		refresh = false;
	}

	public void stopPoll() {
		if (refresh) {
			PackageAdmin packageAdmin = Activator.getPackageAdmin(10000);
			if (packageAdmin != null)
				packageAdmin.refreshPackages(null);
			refresh = false;
		}
	}

	public Long getSeenFile(File file) {
		return (Long) seenFiles.get(file);
	}

}
