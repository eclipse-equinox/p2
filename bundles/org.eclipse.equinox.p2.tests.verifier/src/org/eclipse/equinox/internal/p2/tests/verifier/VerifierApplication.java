/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB - Ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.tests.verifier;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.ProfilePreferences;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration.MigrationWizard;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.resource.*;

/**
 * Application which verifies an install.
 *
 * @since 1.0
 */
@SuppressWarnings("restriction")
public class VerifierApplication implements IApplication {

	private static final File DEFAULT_PROPERTIES_FILE = new File("verifier.properties"); //$NON-NLS-1$
	private static final String ARG_PROPERTIES = "-verifier.properties"; //$NON-NLS-1$
	private IProvisioningAgent agent;
	private Properties properties = null;
	private List<String> ignoreResolved = null;

	/*
	 * Create and return an error status with the given message.
	 */
	private static IStatus createError(String message) {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		processArguments(args);

		agent = ServiceHelper.getService(Activator.getBundleContext(), IProvisioningAgent.class);

		IStatus result = verify();
		if (!result.isOK()) {
			//			PrintWriter out = new PrintWriter(new FileWriter(new File("c:/tmp/dropins-debug.txt")));
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(System.err))) {
				out.println("Error from dropin verifier application: " + result.getMessage()); //$NON-NLS-1$
				Throwable t = result.getException();
				if (t != null)
					t.printStackTrace(out);
			}
			LogHelper.log(result);
		}
		return result.isOK() ? IApplication.EXIT_OK : Integer.valueOf(13);
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
		try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
			result.load(input);
			return result;
		}
	}

	@Override
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
			ignoreResolved = new ArrayList<>();
			String list = properties.getProperty("ignore.unresolved");
			if (list == null)
				return true;
			for (StringTokenizer tokenizer = new StringTokenizer(list, ","); tokenizer.hasMoreTokens();)
				ignoreResolved.add(tokenizer.nextToken().trim());
		}
		for (String string : ignoreResolved) {
			if (bundle.equals(string))
				return false;
		}
		return true;
	}

	/*
	 * Check to ensure all of the bundles in the system are resolved.
	 *
	 * Copied and modified from EclipseStarter#logUnresolvedBundles.
	 * This method prints out all the reasons while asking the resolver directly
	 * will only print out the first reason.
	 */
	private IStatus checkResolved() {
		List<IStatus> allProblems = new ArrayList<>();

		List<Bundle> unresolved = new ArrayList<>();
		for (Bundle b : Activator.getBundleContext().getBundles()) {
			BundleRevision revision = b.adapt(BundleRevision.class);
			if (revision != null && revision.getWiring() == null) {
				unresolved.add(b);
			}
		}

		ResolutionReport report = getResolutionReport(unresolved);
		Map<Resource, List<ResolutionReport.Entry>> entries = report.getEntries();
		Collection<Resource> unresolvedResources = new HashSet<>(entries.keySet());
		Collection<Resource> leafResources = new HashSet<>();
		for (Map.Entry<Resource, List<ResolutionReport.Entry>> resourceEntry : entries.entrySet()) {
			for (ResolutionReport.Entry reportEntry : resourceEntry.getValue()) {
				if (!reportEntry.getType().equals(ResolutionReport.Entry.Type.UNRESOLVED_PROVIDER)) {
					leafResources.add(resourceEntry.getKey());
					unresolvedResources.remove(resourceEntry.getKey());
				}
			}
		}
		// first lets look for missing leaf constraints (bug 114120)
		for (Resource leafResource : leafResources) {
			BundleRevision revision = (BundleRevision) leafResource;
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, revision.getBundle().getLocation()) + '\n';
			message += report.getResolutionReportMessage(leafResource);
			allProblems.add(createError(message));
		}
		// now report all others
		for (Resource unresolvedResource : unresolvedResources) {
			BundleRevision revision = (BundleRevision) unresolvedResource;
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, revision.getBundle().getLocation()) + '\n';
			message += report.getResolutionReportMessage(unresolvedResource);
			allProblems.add(createError(message));
		}

		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, "Problems checking resolved bundles.", null); //$NON-NLS-1$
		for (IStatus status : allProblems)
			result.add(status);
		return result;
	}

	private static ResolutionReport getResolutionReport(Collection<Bundle> bundles) {
		BundleContext context = Activator.getBundleContext();
		DiagReportListener reportListener = new DiagReportListener(bundles);
		ServiceRegistration<ResolverHookFactory> hookReg = context.registerService(ResolverHookFactory.class, reportListener, null);
		try {
			Bundle systemBundle = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
			FrameworkWiring frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
			frameworkWiring.resolveBundles(bundles);
			return reportListener.getReport();
		} finally {
			hookReg.unregister();
		}
	}

	private static class DiagReportListener implements ResolverHookFactory {
		private final Collection<BundleRevision> targetTriggers = new ArrayList<>();

		public DiagReportListener(Collection<Bundle> bundles) {
			for (Bundle bundle : bundles) {
				BundleRevision revision = bundle.adapt(BundleRevision.class);
				if (revision != null && revision.getWiring() == null) {
					targetTriggers.add(revision);
				}
			}

		}

		volatile ResolutionReport report = null;

		class DiagResolverHook implements ResolverHook, ResolutionReport.Listener {

			@Override
			public void handleResolutionReport(ResolutionReport handleReport) {
				DiagReportListener.this.report = handleReport;
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				// nothing
			}

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				// nothing
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				// nothing
			}

			@Override
			public void end() {
				// nothing
			}

		}

		@Override
		public ResolverHook begin(Collection<BundleRevision> triggers) {
			if (triggers.containsAll(targetTriggers)) {
				return new DiagResolverHook();
			}
			return null;
		}

		ResolutionReport getReport() {
			return report;
		}
	}

	/*
	 * Ensure we have a profile registry and can access the SELF profile.
	 */
	private IStatus checkProfileRegistry() {
		IProfileRegistry registry = agent.getService(IProfileRegistry.class);
		if (registry == null)
			return createError("Profile registry service not available."); //$NON-NLS-1$
		IProfile profile = registry.getProfile(IProfileRegistry.SELF);
		if (profile == null)
			return createError("SELF profile not available in profile registry."); //$NON-NLS-1$
		if (properties.get("checkPresenceOfVerifier") != null && !Boolean.FALSE.toString().equals(properties.get("checkPresenceOfVerifier"))) {
			IQueryResult<IInstallableUnit> results = profile.query(QueryUtil.createIUQuery(Activator.PLUGIN_ID), null);
			if (results.isEmpty())
				return createError(NLS.bind("IU for {0} not found in SELF profile.", Activator.PLUGIN_ID)); //$NON-NLS-1$
		}
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

		temp = hasProfileFlag();
		if (!temp.isOK())
			result.merge(temp);

		temp = checkAbsenceOfBundles();
		if (!temp.isOK())
			result.merge(temp);

		temp = checkPresenceOfBundles();
		if (!temp.isOK())
			result.merge(temp);

		temp = checkSystemProperties();
		if (!temp.isOK())
			result.merge(temp);

		temp = checkMigrationWizard();
		if (!temp.isOK())
			result.merge(temp);

		assumeMigrated();

		handleWizardCancellation();
		return result;
	}

	private void handleWizardCancellation() {
		if (properties.getProperty("checkMigration.cancelAnswer") == null)
			return;
		new Display();
		IProfileRegistry reg = agent.getService(IProfileRegistry.class);
		IProfile profile = reg.getProfile(IProfileRegistry.SELF);

		MigrationWizard wizardPage = new MigrationWizard(profile, Collections.emptyList(), new URI[0], false);
		int cancelAnswer = Integer.parseInt(properties.getProperty("checkMigration.cancelAnswer"));
		wizardPage.rememberCancellationDecision(cancelAnswer);
		// The preferences are saved by:
		// org.eclipse.equinox.internal.p2.engine.ProfilePreferences.SaveJob
		// This job is scheduled with a delay: saveJob.schedule(SAVE_SCHEDULE_DELAY).
		// We'd best wait for that job to complete before existing.
		try {
			Job.getJobManager().join(ProfilePreferences.PROFILE_SAVE_JOB_FAMILY, null);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}
	}

	private IStatus checkSystemProperties() {
		final String ABSENT_SYS_PROPERTY = "not.sysprop.";
		final String PRESENT_SYS_PROPERTY = "sysprop.";
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, "System properties validation", null);

		Set<Entry<Object, Object>> entries = properties.entrySet();
		for (Entry<Object, Object> entry : entries) {
			String key = (String) entry.getKey();
			if (key.startsWith(ABSENT_SYS_PROPERTY)) {
				String property = key.substring(ABSENT_SYS_PROPERTY.length());
				if (System.getProperty(property) != null)
					result.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Property " + property + " should not be set."));
			}
			if (key.startsWith(PRESENT_SYS_PROPERTY)) {
				String property = key.substring(PRESENT_SYS_PROPERTY.length());
				String foundValue = System.getProperty(property);
				if (!entry.getValue().equals(foundValue))
					result.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Property " + property + " should be set to " + entry.getValue() + " and is set to " + foundValue + "."));
			}
		}
		if (result.getChildren().length == 0)
			return Status.OK_STATUS;
		return result;
	}

	private IStatus checkAbsenceOfBundles() {
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, "Some bundles should not be there", null);
		String unexpectedBundlesString = properties.getProperty("unexpectedBundleList");
		if (unexpectedBundlesString == null)
			return Status.OK_STATUS;
		String[] unexpectedBundles = unexpectedBundlesString.split(",");
		for (String bsn : unexpectedBundles) {
			if (containsBundle(bsn)) {
				result.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, bsn + " should not have been found in the install"));
			}
		}
		if (result.getChildren().length == 0)
			return Status.OK_STATUS;
		return result;
	}

	private boolean containsBundle(final String bsn) {
		FrameworkWiring fWiring = Activator.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
		Collection<BundleCapability> existing = fWiring.findProviders(new Requirement() {

			@Override
			public String getNamespace() {
				return IdentityNamespace.IDENTITY_NAMESPACE;
			}

			@Override
			public Map<String, String> getDirectives() {
				return Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + bsn + ")");
			}

			@Override
			public Map<String, Object> getAttributes() {
				return Collections.EMPTY_MAP;
			}

			@Override
			public Resource getResource() {
				return null;
			}
		});
		return !existing.isEmpty();
	}

	private IStatus checkPresenceOfBundles() {
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, "Some bundles should not be there", null);
		String expectedBundlesString = properties.getProperty("expectedBundleList");
		if (expectedBundlesString == null)
			return Status.OK_STATUS;
		String[] expectedBundles = expectedBundlesString.split(",");
		for (String bsn : expectedBundles) {
			if (!containsBundle(bsn)) {
				result.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, bsn + " is missing from the install"));
			}
		}
		if (result.getChildren().length == 0)
			return Status.OK_STATUS;
		return result;
	}

	private IStatus hasProfileFlag() {
		if (properties.getProperty("checkProfileResetFlag") == null || "false".equals(properties.getProperty("checkProfileResetFlag")))
			return Status.OK_STATUS;
		//Make sure that the profile is already loaded
		IProfileRegistry reg = agent.getService(IProfileRegistry.class);
		IProfile profile = reg.getProfile(IProfileRegistry.SELF);
		String profileId = profile.getProfileId();

		long history[] = reg.listProfileTimestamps(profileId);
		long lastTimestamp = history[history.length - 1];
		if (IProfile.STATE_SHARED_INSTALL_VALUE_NEW.equals(reg.getProfileStateProperties(profileId, lastTimestamp).get(IProfile.STATE_PROP_SHARED_INSTALL))) {
			return Status.OK_STATUS;
		}
		if (history.length == 1) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The flag indicating that a profile has been reset is incorrectly setup");
		}

		long previousToLastTimestamp = history[history.length - 2];
		if (IProfile.STATE_SHARED_INSTALL_VALUE_NEW.equals(reg.getProfileStateProperties(profileId, previousToLastTimestamp).get(IProfile.STATE_PROP_SHARED_INSTALL))) {
			return Status.OK_STATUS;
		}

		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The flag indicating that a profile has been reset is incorrectly setup");
	}

	private IStatus checkMigrationWizard() {
		if (properties.getProperty("checkMigrationWizard") == null)
			return Status.OK_STATUS;

		IProfileRegistry reg = agent.getService(IProfileRegistry.class);
		IProfile profile = reg.getProfile(IProfileRegistry.SELF);

		//Fake the opening of the wizard
		MigrationWizardTestHelper migrationSupport = new MigrationWizardTestHelper();
		migrationSupport.performMigration(agent, reg, profile);

		boolean wizardExpectedToOpen = Boolean.parseBoolean(properties.getProperty("checkMigrationWizard.open"));
		if (migrationSupport.wizardOpened == wizardExpectedToOpen)
			return Status.OK_STATUS;

		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The migration wizard did " + (wizardExpectedToOpen ? "not" : "") + " open");
	}

	private void assumeMigrated() {
		if (properties.getProperty("checkMigrationWizard.simulate.reinstall") == null)
			return;

		new MigrationWizardTestHelper().rememberMigrationCompleted();
	}
}
