/*******************************************************************************
 *  Copyright (c) 2010, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.publisher.compatibility;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.osgi.service.application.*;

/**
 * @since 1.1
 */
public class GeneratorApplication implements IApplication {
	protected static final String APP_ID = "app.id"; //$NON-NLS-1$

	public static final String UPDATE_SITE_APPLICATION = "org.eclipse.equinox.p2.publisher.UpdateSitePublisher"; //$NON-NLS-1$
	public static final String INSTALL_APPLICATION = "org.eclipse.equinox.p2.publisher.InstallPublisher"; //$NON-NLS-1$
	public static final String FEATURES_BUNDLES_APPLICATION = "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher"; //$NON-NLS-1$

	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	public void stop() {
		// TODO Auto-generated method stub
	}

	public Object run(String[] arguments) {
		Map<String, Object> argumentMap = parseArguments(arguments);
		return launchApplication(argumentMap);
	}

	protected Object launchApplication(Map<String, Object> applicationMap) {
		String applicationId = (String) applicationMap.get(APP_ID);
		String filter = "(service.pid=" + applicationId + ")"; //$NON-NLS-1$//$NON-NLS-2$
		ApplicationDescriptor descriptor = ServiceHelper.getService(Activator.getContext(), ApplicationDescriptor.class, filter);
		try {
			ApplicationHandle handle = descriptor.launch(applicationMap);
			return handle.getExitValue(0);
		} catch (ApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private Map<String, Object> parseArguments(String[] arguments) {
		Map<String, Object> applicationMap = new HashMap<String, Object>();
		Map<String, String> args = new HashMap<String, String>(arguments.length);
		for (int i = 0; i < arguments.length; i++) {
			if (i == arguments.length - 1 || arguments[i + 1].startsWith(IGeneratorConstants.DASH))
				args.put(arguments[i], null);
			else
				args.put(arguments[i], arguments[++i]);
		}

		//adjust the short forms
		if (args.containsKey(IGeneratorConstants.AR))
			args.put(IGeneratorConstants.ARTIFACT_REPO, args.remove(IGeneratorConstants.AR));
		if (args.containsKey(IGeneratorConstants.MR))
			args.put(IGeneratorConstants.METADATA_REPO, args.remove(IGeneratorConstants.MR));
		if (args.containsKey(IGeneratorConstants.PA))
			args.put(IGeneratorConstants.PUBLISH_ARTIFACTS, args.remove(IGeneratorConstants.PA));
		if (args.containsKey(IGeneratorConstants.PAR))
			args.put(IGeneratorConstants.PUBLISH_ATIFACT_REPOSITORY, args.remove(IGeneratorConstants.PAR));

		if (args.containsKey(IGeneratorConstants.ROOT)) {
			String rootId = args.remove(IGeneratorConstants.ROOT);
			args.put(IGeneratorConstants.IU, rootId);
			args.put(IGeneratorConstants.ID, rootId);
		}

		if (args.containsKey(IGeneratorConstants.ROOT_VERSION))
			args.put(IGeneratorConstants.VERSION, args.remove(IGeneratorConstants.ROOT_VERSION));

		String source = null;
		// -inplace and -updateSite become -source, and imply -append
		if (args.containsKey(IGeneratorConstants.UPDATE_SITE))
			source = args.remove(IGeneratorConstants.UPDATE_SITE);
		if (args.containsKey(IGeneratorConstants.INPLACE))
			source = args.remove(IGeneratorConstants.INPLACE);
		if (source != null) {
			args.put(IGeneratorConstants.SOURCE, source);
			args.put(IGeneratorConstants.APPEND, null);

			//if not specified, repo locations are based on source
			String repoLocation = URIUtil.toUnencodedString(new File(source).toURI());
			if (!args.containsKey(IGeneratorConstants.ARTIFACT_REPO))
				args.put(IGeneratorConstants.ARTIFACT_REPO, repoLocation);
			if (!args.containsKey(IGeneratorConstants.METADATA_REPO))
				args.put(IGeneratorConstants.METADATA_REPO, repoLocation);
		}

		File base = new File(args.get(IGeneratorConstants.SOURCE));
		File configuration = new File(base, "configuration"); //$NON-NLS-1$
		if (configuration.exists()) {
			applicationMap.put(APP_ID, INSTALL_APPLICATION);
		} else if (args.containsKey(IGeneratorConstants.SITE)) {
			applicationMap.put(APP_ID, UPDATE_SITE_APPLICATION);
		} else if (args.containsKey(IGeneratorConstants.CONFIG)) {
			applicationMap.put(APP_ID, INSTALL_APPLICATION);
		} else {
			applicationMap.put(APP_ID, FEATURES_BUNDLES_APPLICATION);
		}

		applicationMap.put(IApplicationContext.APPLICATION_ARGS, flattenMap(args));
		return applicationMap;
	}

	private String[] flattenMap(Map<String, String> map) {
		ArrayList<String> list = new ArrayList<String>(map.size());
		for (Iterator<String> iterator = map.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			String value = map.get(key);
			list.add(key);
			if (value != null)
				list.add(value);
		}
		return list.toArray(new String[list.size()]);
	}
}
