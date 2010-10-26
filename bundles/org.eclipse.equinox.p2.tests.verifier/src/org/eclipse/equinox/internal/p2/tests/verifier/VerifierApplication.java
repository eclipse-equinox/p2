/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.tests.verifier;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorMsg;
import org.eclipse.core.runtime.internal.adaptor.MessageHelper;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Application which verifies an install.
 * 
 * @since 1.0
 */
public class VerifierApplication implements IApplication {

	private static final File DEFAULT_PROPERTIES_FILE = new File("verifier.properties"); //$NON-NLS-1$
	private static final String ARG_PROPERTIES = "-verifier.properties"; //$NON-NLS-1$
	private IProvisioningAgent agent;
	private Properties properties = null;
	private List ignoreResolved = null;

	/*
	 * Create and return an error status with the given message.
	 */
	private static IStatus createError(String message) {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		processArguments(args);

		agent = (IProvisioningAgent) ServiceHelper.getService(Activator.getBundleContext(), IProvisioningAgent.SERVICE_NAME);

		IStatus result = verify();
		if (!result.isOK()) {
			//			PrintWriter out = new PrintWriter(new FileWriter(new File("c:/tmp/dropins-debug.txt")));
			PrintWriter out = new PrintWriter(new OutputStreamWriter(System.err));
			out.println("Error from dropin verifier application: " + result.getMessage()); //$NON-NLS-1$
			Throwable t = result.getException();
			if (t != null)
				t.printStackTrace(out);
			out.close();
			LogHelper.log(result);
		}
		return result.isOK() ? IApplication.EXIT_OK : new Integer(13);
	}

	/*
	 * Go through the command-line args and pull out interesting ones
	 * for later consumption.
	 */
	private void processArguments(String[] args) {
		if (args == null)
			return;

		for (int i = 1; i < args.length; i++) {
			if (ARG_PROPERTIES.equals(args[i - 1])) {
				String filename = args[i];
				if (filename.startsWith("-")) //$NON-NLS-1$
					continue;
				try {
					properties = readProperties(new File(filename));
				} catch (IOException e) {
					// TODO
					e.printStackTrace();
					// fall through to load default
				}
				continue;
			}
		}

		// problems loading properties file or none specified so look for a default
		if (properties == null) {
			try {
				if (DEFAULT_PROPERTIES_FILE.exists())
					properties = readProperties(DEFAULT_PROPERTIES_FILE);
			} catch (IOException e) {
				// TODO 
				e.printStackTrace();
			}
		}
		if (properties == null)
			properties = new Properties();
	}

	/*
	 * Read and return a properties file at the given location.
	 */
	private Properties readProperties(File file) throws IOException {
		Properties result = new Properties();
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			result.load(input);
			return result;
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// nothing to do
	}

	/*
	 * Return a boolean value indicating whether or not the bundle with the given symbolic name
	 * should be considered when looking at bundles which are not resolved in the system.
	 * TODO the call to this method was removed. we should add it back
	 */
	protected boolean shouldCheckResolved(String bundle) {
		if (ignoreResolved == null) {
			ignoreResolved = new ArrayList();
			String list = properties.getProperty("ignore.unresolved");
			if (list == null)
				return true;
			for (StringTokenizer tokenizer = new StringTokenizer(list, ","); tokenizer.hasMoreTokens();)
				ignoreResolved.add(tokenizer.nextToken().trim());
		}
		for (Iterator iter = ignoreResolved.iterator(); iter.hasNext();) {
			if (bundle.equals(iter.next()))
				return false;
		}
		return true;
	}

	private List getAllBundles() {
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getBundleContext(), PlatformAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) ServiceHelper.getService(Activator.getBundleContext(), PackageAdmin.class.getName());
		State state = platformAdmin.getState(false);
		List result = new ArrayList();

		BundleDescription[] bundles = state.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription bundle = bundles[i];
			Bundle[] versions = packageAdmin.getBundles(bundle.getSymbolicName(), bundle.getVersion().toString());
			for (int j = 0; j < versions.length; j++)
				result.add(versions[j]);
		}
		return result;
	}

	/*
	 * Check to ensure all of the bundles in the system are resolved.
	 * 
	 * Copied and modified from EclipseStarter#logUnresolvedBundles.
	 * This method prints out all the reasons while asking the resolver directly
	 * will only print out the first reason.
	 */
	private IStatus checkResolved() {
		List allProblems = new ArrayList();
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getBundleContext(), PlatformAdmin.class.getName());
		State state = platformAdmin.getState(false);
		StateHelper stateHelper = platformAdmin.getStateHelper();

		// first lets look for missing leaf constraints (bug 114120)
		VersionConstraint[] leafConstraints = stateHelper.getUnsatisfiedLeaves(state.getBundles());
		// hash the missing leaf constraints by the declaring bundles
		Map missing = new HashMap();
		for (int i = 0; i < leafConstraints.length; i++) {
			// only include non-optional and non-dynamic constraint leafs
			if (leafConstraints[i] instanceof BundleSpecification && ((BundleSpecification) leafConstraints[i]).isOptional())
				continue;
			if (leafConstraints[i] instanceof ImportPackageSpecification) {
				if (ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) leafConstraints[i]).getDirective(Constants.RESOLUTION_DIRECTIVE)))
					continue;
				if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(((ImportPackageSpecification) leafConstraints[i]).getDirective(Constants.RESOLUTION_DIRECTIVE)))
					continue;
			}
			BundleDescription bundleDesc = leafConstraints[i].getBundle();
			ArrayList constraints = (ArrayList) missing.get(bundleDesc);
			if (constraints == null) {
				constraints = new ArrayList();
				missing.put(bundleDesc, constraints);
			}
			constraints.add(leafConstraints[i]);
		}

		// found some bundles with missing leaf constraints; log them first 
		if (missing.size() > 0) {
			for (Iterator iter = missing.keySet().iterator(); iter.hasNext();) {
				BundleDescription description = (BundleDescription) iter.next();
				String generalMessage = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, description.getLocation());
				ArrayList constraints = (ArrayList) missing.get(description);
				for (Iterator inner = constraints.iterator(); inner.hasNext();) {
					String message = generalMessage + " Reason: " + MessageHelper.getResolutionFailureMessage((VersionConstraint) inner.next()); //$NON-NLS-1$
					allProblems.add(createError(message));
				}
			}
		}

		// There may be some bundles unresolved for other reasons, causing the system to be unresolved
		// log all unresolved constraints now
		List allBundles = getAllBundles();
		for (Iterator i = allBundles.iterator(); i.hasNext();) {
			Bundle bundle = (Bundle) i.next();
			if (bundle.getState() == Bundle.INSTALLED) {
				String generalMessage = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, bundle);
				BundleDescription description = state.getBundle(bundle.getBundleId());
				// for some reason, the state does not know about that bundle
				if (description == null)
					continue;
				VersionConstraint[] unsatisfied = stateHelper.getUnsatisfiedConstraints(description);
				if (unsatisfied.length > 0) {
					// the bundle wasn't resolved due to some of its constraints were unsatisfiable
					for (int j = 0; j < unsatisfied.length; j++)
						allProblems.add(createError(generalMessage + " Reason: " + MessageHelper.getResolutionFailureMessage(unsatisfied[j]))); //$NON-NLS-1$
				} else {
					ResolverError[] resolverErrors = state.getResolverErrors(description);
					for (int j = 0; j < resolverErrors.length; j++) {
						if (shouldAdd(resolverErrors[j])) {
							allProblems.add(createError(generalMessage + " Reason: " + resolverErrors[j].toString())); //$NON-NLS-1$
						}
					}
				}
			}
		}
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, "Problems checking resolved bundles.", null); //$NON-NLS-1$
		for (Iterator iter = allProblems.iterator(); iter.hasNext();)
			result.add((IStatus) iter.next());
		return result;
	}

	/*
	 * Return a boolean value indicating whether or not the given resolver error should be 
	 * added to our results.
	 */
	private boolean shouldAdd(ResolverError error) {
		// ignore EE problems? default value is true
		String prop = properties.getProperty("ignore.ee"); //$NON-NLS-1$
		boolean ignoreEE = prop == null || Boolean.valueOf(prop).booleanValue();
		if (ResolverError.MISSING_EXECUTION_ENVIRONMENT == error.getType() && ignoreEE)
			return false;
		return true;
	}

	/*
	 * Ensure we have a profile registry and can access the SELF profile.
	 */
	private IStatus checkProfileRegistry() {
		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null)
			return createError("Profile registry service not available."); //$NON-NLS-1$
		IProfile profile = registry.getProfile(IProfileRegistry.SELF);
		if (profile == null)
			return createError("SELF profile not available in profile registry."); //$NON-NLS-1$
		IQueryResult results = profile.query(QueryUtil.createIUQuery(Activator.PLUGIN_ID), null);
		if (results.isEmpty())
			return createError(NLS.bind("IU for {0} not found in SELF profile.", Activator.PLUGIN_ID)); //$NON-NLS-1$
		return Status.OK_STATUS;
	}

	/*
	 * Perform all of the verification checks.
	 */
	public IStatus verify() {
		String message = "Problems occurred during verification."; //$NON-NLS-1$
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, message, null);

		// ensure all the bundles are resolved
		IStatus temp = checkResolved();
		if (!temp.isOK())
			result.merge(temp);

		// ensure we have a profile registry
		temp = checkProfileRegistry();
		if (!temp.isOK())
			result.merge(temp);

		return result;
	}

}
