/*******************************************************************************
 * Copyright (c) 2007, 2008 aQute, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * aQute - initial implementation and ideas 
 * IBM Corporation - initial adaptation to Equinox provisioning use
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.directorywatcher;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigFileListener extends DirectoryChangeListener {
	public final static String ALIAS_KEY = ".alias_factory_pid";

	private Map seenFiles = new HashMap();

	public boolean added(File file) {
		// load the input file into a hashtable of settings...
		Properties temp = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			try {
				temp.load(in);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			// TODO proper logging etc here
			e.printStackTrace();
		}
		Hashtable settings = new Hashtable();
		settings.putAll(temp);

		String pid[] = parsePid(file.getName());
		if (pid[1] != null)
			settings.put(ALIAS_KEY, pid[1]);
		Configuration config = getConfiguration(pid[0], pid[1]);
		if (config == null)
			return false;
		if (config.getBundleLocation() != null)
			config.setBundleLocation(null);
		try {
			config.update(settings);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		seenFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	public boolean changed(File file) {
		return added(file);
	}

	private Configuration getConfiguration(String pid, String factoryPid) {
		ConfigurationAdmin cm = Activator.getConfigAdmin();
		if (cm == null)
			return null;

		try {
			if (factoryPid != null) {
				Configuration configs[] = null;
				try {
					configs = cm.listConfigurations("(" + ALIAS_KEY + "=" + factoryPid + ")");
				} catch (InvalidSyntaxException e) {
					return null;
				}
				if (configs == null || configs.length == 0)
					return cm.createFactoryConfiguration(pid, null);
				else
					return configs[0];
			} else
				return cm.getConfiguration(pid, null);
		} catch (IOException e) {
			return null;
		}
	}

	public boolean isInterested(File file) {
		return file.getName().endsWith("");
	}

	public Long getSeenFile(File file) {
		return (Long) seenFiles.get(file);
	}

	private String[] parsePid(String path) {
		String pid = path.substring(0, path.length() - 4);
		int n = pid.indexOf('-');
		if (n > 0) {
			String factoryPid = pid.substring(n + 1);
			pid = pid.substring(0, n);
			return new String[] {pid, factoryPid};
		} else
			return new String[] {pid, null};
	}

	public boolean removed(File file) {
		String pid[] = parsePid(file.getName());
		Configuration config = getConfiguration(pid[0], pid[1]);
		if (config == null)
			return false;
		try {
			config.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		seenFiles.remove(file);
		return true;
	}

	public void startPoll() {
		// TODO Auto-generated method stub
	}

	public void stopPoll() {
		// TODO Auto-generated method stub
	}

}
