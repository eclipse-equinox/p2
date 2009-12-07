/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.extensionlocation.Constants;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.GroupQuery;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Synchronizes a profile with a set of repositories.
 */
public class ProfileSynchronizer {
	private static final String RECONCILER_APPLICATION_ID = "org.eclipse.equinox.p2.reconciler.application"; //$NON-NLS-1$
	private static final String TIMESTAMPS_FILE_PREFIX = "timestamps"; //$NON-NLS-1$
	private static final String PROFILE_TIMESTAMP = "PROFILE"; //$NON-NLS-1$
	private static final String NO_TIMESTAMP = "-1"; //$NON-NLS-1$
	private static final String PROP_FROM_DROPINS = "org.eclipse.equinox.p2.reconciler.dropins"; //$NON-NLS-1$

	private static final String CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	private static final String EXPLANATION = "org.eclipse.equinox.p2.director.explain"; //$NON-NLS-1$
	final IProfile profile;

	final Map repositoryMap;
	private Properties timestamps;

	/*
	 * Constructor for the class.
	 */
	public ProfileSynchronizer(IProfile profile, Collection repositories) {
		this.profile = profile;
		this.repositoryMap = new HashMap();
		for (Iterator it = repositories.iterator(); it.hasNext();) {
			IMetadataRepository repository = (IMetadataRepository) it.next();
			repositoryMap.put(repository.getLocation().toString(), repository);
		}
	}

	/*
	 * Synchronize the profile with the list of metadata repositories.
	 */
	public IStatus synchronize(IProgressMonitor monitor) {
		readTimestamps();
		if (isUpToDate())
			return Status.OK_STATUS;

		ProvisioningContext context = getContext();
		context.setProperty(EXPLANATION, Boolean.FALSE.toString());

		ProfileChangeRequest request = createProfileChangeRequest(context);
		String updatedCacheExtensions = synchronizeCacheExtensions();
		if (request == null) {
			if (updatedCacheExtensions != null) {
				Operand operand = new PropertyOperand(CACHE_EXTENSIONS, null, updatedCacheExtensions);
				IStatus engineResult = executeOperands(new Operand[] {operand}, context, null);
				if (engineResult.getSeverity() != IStatus.ERROR && engineResult.getSeverity() != IStatus.CANCEL)
					writeTimestamps();
				return engineResult;
			}
			return Status.OK_STATUS;
		}
		if (updatedCacheExtensions != null)
			request.setProfileProperty(CACHE_EXTENSIONS, updatedCacheExtensions);

		SubMonitor sub = SubMonitor.convert(monitor, 100);
		try {
			//create the provisioning plan
			IProvisioningPlan plan = createProvisioningPlan(request, context, sub.newChild(50));
			IStatus status = plan.getStatus();
			if (status.getSeverity() == IStatus.ERROR || status.getSeverity() == IStatus.CANCEL)
				return status;

			Operand[] operands = plan.getOperands();
			if (operands.length == 0 || containsOnlyInstallableUnitPropertyOperandAdditions(operands)) {
				writeTimestamps();
				return status;
			}

			//invoke the engine to perform installs/uninstalls
			IStatus engineResult = executePlan(plan, context, sub.newChild(50));
			if (engineResult.getSeverity() == IStatus.ERROR || engineResult.getSeverity() == IStatus.CANCEL)
				return engineResult;

			writeTimestamps();

			return applyConfiguration(false);
		} finally {
			sub.done();
		}
	}

	// This is a special case that occurs where all IUs being installed are not compatible with the profile so
	// the operands will have no affect if executed on the profile.
	private boolean containsOnlyInstallableUnitPropertyOperandAdditions(Operand[] operands) {
		for (int i = 0; i < operands.length; i++) {
			if (!(operands[i] instanceof InstallableUnitPropertyOperand))
				return false;

			InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) operands[i];
			//check if this is a removal or update
			if (iuPropertyOperand.first() != null)
				return false;
		}
		return true;
	}

	private void writeTimestamps() {
		timestamps.clear();
		timestamps.put(PROFILE_TIMESTAMP, Long.toString(profile.getTimestamp()));
		for (Iterator it = repositoryMap.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			IMetadataRepository repository = (IMetadataRepository) entry.getValue();
			Map props = repository.getProperties();
			String timestamp = null;
			if (props != null)
				timestamp = (String) props.get(IRepository.PROP_TIMESTAMP);

			if (timestamp == null)
				timestamp = NO_TIMESTAMP;

			timestamps.put(entry.getKey(), timestamp);
		}

		try {
			File file = Activator.getContext().getDataFile(TIMESTAMPS_FILE_PREFIX + profile.getProfileId().hashCode());
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			try {
				timestamps.store(os, "Timestamps for " + profile.getProfileId()); //$NON-NLS-1$
			} finally {
				if (os != null)
					os.close();
			}
		} catch (FileNotFoundException e) {
			//Ignore
		} catch (IOException e) {
			//Ignore
		}
	}

	private boolean isUpToDate() {
		// the user might want to force a reconciliation
		if ("true".equals(Activator.getContext().getProperty("osgi.checkConfiguration"))) //$NON-NLS-1$//$NON-NLS-2$
			return false;

		String lastKnownProfileTimeStamp = (String) timestamps.remove(PROFILE_TIMESTAMP);
		if (lastKnownProfileTimeStamp == null)
			return false;
		if (!lastKnownProfileTimeStamp.equals(Long.toString(profile.getTimestamp())))
			return false;

		//When we get here the timestamps map only contains information related to repos
		for (Iterator it = repositoryMap.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			IMetadataRepository repository = (IMetadataRepository) entry.getValue();

			Map props = repository.getProperties();
			String currentTimestamp = null;
			if (props != null)
				currentTimestamp = (String) props.get(IRepository.PROP_TIMESTAMP);

			if (currentTimestamp == null)
				currentTimestamp = NO_TIMESTAMP;

			String lastKnownTimestamp = (String) timestamps.remove(entry.getKey());
			//A repo has been added 
			if (lastKnownTimestamp == null)
				return false;
			if (!lastKnownTimestamp.equals(currentTimestamp)) {
				return false;
			}
		}
		//A repo has been removed
		if (timestamps.size() != 0)
			return false;

		return true;
	}

	private void readTimestamps() {
		File file = Activator.getContext().getDataFile(TIMESTAMPS_FILE_PREFIX + profile.getProfileId().hashCode());
		timestamps = new Properties();
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			try {
				timestamps.load(is);
			} finally {
				if (is != null)
					is.close();
			}
		} catch (FileNotFoundException e) {
			//Ignore
		} catch (IOException e) {
			//Ignore
		}
	}

	private ProvisioningContext getContext() {
		ArrayList repoURLs = new ArrayList();
		for (Iterator iterator = repositoryMap.keySet().iterator(); iterator.hasNext();) {
			try {
				repoURLs.add(new URI((String) iterator.next()));
			} catch (URISyntaxException e) {
				//ignore
			}
		}
		ProvisioningContext result = new ProvisioningContext((URI[]) repoURLs.toArray(new URI[repoURLs.size()]));
		result.setArtifactRepositories(new URI[0]);
		return result;
	}

	private String synchronizeCacheExtensions() {
		List currentExtensions = new ArrayList();
		StringBuffer buffer = new StringBuffer();

		List repositories = new ArrayList(repositoryMap.keySet());
		final String OSGiInstallArea = Activator.getOSGiInstallArea().toExternalForm() + Constants.EXTENSION_LOCATION;
		Collections.sort(repositories, new Comparator() {
			public int compare(Object left, Object right) {
				if (OSGiInstallArea.equals(left))
					return -1;
				if (OSGiInstallArea.equals(right))
					return 1;
				return ((String) left).compareTo((String) right);
			}
		});
		for (Iterator it = repositories.iterator(); it.hasNext();) {
			String repositoryId = (String) it.next();
			try {
				IArtifactRepository repository = Activator.loadArtifactRepository(new URI(repositoryId), null);

				if (repository instanceof IFileArtifactRepository) {
					currentExtensions.add(escapePipe(repositoryId));
					buffer.append(repositoryId);
					if (it.hasNext())
						buffer.append(PIPE);
				}
			} catch (ProvisionException e) {
				// ignore
			} catch (URISyntaxException e) {
				// unexpected
				e.printStackTrace();
			}
		}
		String currentExtensionsProperty = (buffer.length() == 0) ? null : buffer.toString();

		List previousExtensions = new ArrayList();
		String previousExtensionsProperty = profile.getProperty(CACHE_EXTENSIONS);
		if (previousExtensionsProperty != null) {
			StringTokenizer tokenizer = new StringTokenizer(previousExtensionsProperty, PIPE);
			while (tokenizer.hasMoreTokens()) {
				previousExtensions.add(tokenizer.nextToken());
			}
		}

		if (previousExtensions.size() == currentExtensions.size() && previousExtensions.containsAll(currentExtensions))
			return null;

		return currentExtensionsProperty;
	}

	/**
	 * Escapes the pipe ('|') character in a URI using the standard URI escape sequence.
	 * This is done because the pipe character is used as the delimiter between locations
	 * in the cache extensions profile property.
	 */
	private String escapePipe(String location) {
		String result = location;
		int pipeIndex;
		while ((pipeIndex = result.indexOf(',')) != -1)
			result = result.substring(0, pipeIndex) + "%7C" + result.substring(pipeIndex + 1); //$NON-NLS-1$
		return result;
	}

	public ProfileChangeRequest createProfileChangeRequest(ProvisioningContext context) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);

		boolean resolve = Boolean.valueOf(profile.getProperty("org.eclipse.equinox.p2.resolve")).booleanValue(); //$NON-NLS-1$
		if (resolve)
			request.removeProfileProperty("org.eclipse.equinox.p2.resolve"); //$NON-NLS-1$

		List toAdd = new ArrayList();
		List toRemove = new ArrayList();

		boolean foundIUsToAdd = false;
		Collection profileIUs = new HashSet(profile.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection());

		// we use IProfile.available(...) here so that we also gather any shared IUs
		Collection availableProfileIUs = new HashSet(profile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection());

		// get all IUs from all our repos (toAdd)
		Collector allIUs = getAllIUsFromRepos();
		for (Iterator iter = allIUs.iterator(); iter.hasNext();) {
			final IInstallableUnit iu = (IInstallableUnit) iter.next();
			// if the IU is already installed in the profile then skip it
			if (!profileIUs.contains(iu)) {
				if (GroupQuery.isGroup(iu))
					request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
				// mark all IUs with special property
				request.setInstallableUnitProfileProperty(iu, PROP_FROM_DROPINS, Boolean.TRUE.toString());
				request.setInstallableUnitInclusionRules(iu, PlannerHelper.createOptionalInclusionRule(iu));
				request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU, Integer.toString(IProfile.LOCK_UNINSTALL));
				toAdd.add(iu);

				// as soon as we find something locally that needs to be installed, then 
				// everything from the parent's dropins must be installed locally as well.
				if (!foundIUsToAdd && !availableProfileIUs.contains(iu)) {
					foundIUsToAdd = true;
				}
			}
		}

		// get all IUs from profile with marked property (existing)
		Collector dropinIUs = profile.query(new IUProfilePropertyQuery(PROP_FROM_DROPINS, Boolean.toString(true)), new Collector(), null);
		Collection all = new HashSet(allIUs.toCollection());
		for (Iterator iter = dropinIUs.iterator(); iter.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iter.next();
			// the STRICT policy is set when we install things via the UI, we use it to differentiate between IUs installed
			// via the dropins and the UI. (dropins are considered optional) If an IU has both properties set it means that
			// it was initially installed via the dropins but then upgraded via the UI. (properties are copied from the old IU
			// to the new IU during an upgrade) In this case we want to remove the "from dropins" property so the upgrade
			// will stick.
			if ("STRICT".equals(profile.getInstallableUnitProperty(iu, "org.eclipse.equinox.p2.internal.inclusion.rules"))) { //$NON-NLS-1$//$NON-NLS-2$
				request.removeInstallableUnitProfileProperty(iu, PROP_FROM_DROPINS);
				request.removeInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU);
				continue;
			}
			// remove the IUs that are in the intersection between the 2 sets
			if (all.contains(iu))
				toAdd.remove(iu);
			else
				toRemove.add(iu);
		}

		if (!foundIUsToAdd && toRemove.isEmpty() && !resolve) {
			if (Tracing.DEBUG_RECONCILER)
				Tracing.debug("[reconciler] Nothing to do."); //$NON-NLS-1$
			return null;
		}

		context.setExtraIUs(toAdd);
		request.addInstallableUnits((IInstallableUnit[]) toAdd.toArray(new IInstallableUnit[toAdd.size()]));
		request.removeInstallableUnits((IInstallableUnit[]) toRemove.toArray(new IInstallableUnit[toRemove.size()]));
		debug(request);
		return request;
	}

	/*
	 * If debugging is turned on, then print out the details for the given profile change request.
	 */
	private void debug(ProfileChangeRequest request) {
		if (!Tracing.DEBUG_RECONCILER)
			return;
		final String PREFIX = "[reconciler] "; //$NON-NLS-1$
		IInstallableUnit[] toAdd = request.getAddedInstallableUnits();
		if (toAdd == null || toAdd.length == 0) {
			Tracing.debug(PREFIX + "No installable units to add."); //$NON-NLS-1$
		} else {
			for (int i = 0; i < toAdd.length; i++) {
				Tracing.debug(PREFIX + "Adding IU: " + toAdd[i].getId() + ' ' + toAdd[i].getVersion()); //$NON-NLS-1$
			}
		}
		Map propsToAdd = request.getInstallableUnitProfilePropertiesToAdd();
		if (propsToAdd == null || propsToAdd.isEmpty()) {
			Tracing.debug(PREFIX + "No IU properties to add."); //$NON-NLS-1$
		} else {
			for (Iterator iter = propsToAdd.keySet().iterator(); iter.hasNext();) {
				Object key = iter.next();
				Tracing.debug(PREFIX + "Adding IU property: " + key + "->" + propsToAdd.get(key)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		IInstallableUnit[] toRemove = request.getRemovedInstallableUnits();
		if (toRemove == null || toRemove.length == 0) {
			Tracing.debug(PREFIX + "No installable units to remove."); //$NON-NLS-1$
		} else {
			for (int i = 0; i < toRemove.length; i++) {
				Tracing.debug(PREFIX + "Removing IU: " + toRemove[i].getId() + ' ' + toRemove[i].getVersion()); //$NON-NLS-1$
			}
		}
		Map propsToRemove = request.getInstallableUnitProfilePropertiesToRemove();
		if (propsToRemove == null || propsToRemove.isEmpty()) {
			Tracing.debug(PREFIX + "No IU properties to remove."); //$NON-NLS-1$
		} else {
			for (Iterator iter = propsToRemove.keySet().iterator(); iter.hasNext();) {
				Object key = iter.next();
				Tracing.debug(PREFIX + "Removing IU property: " + key + "->" + propsToRemove.get(key)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private Collector getAllIUsFromRepos() {
		Collector allRepos = new Collector();
		for (Iterator it = repositoryMap.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			IMetadataRepository repository = (IMetadataRepository) entry.getValue();
			repository.query(InstallableUnitQuery.ANY, allRepos, null).iterator();
		}
		return allRepos;
	}

	private IProvisioningPlan createProvisioningPlan(ProfileChangeRequest request, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IPlanner.SERVICE_NAME);
		IPlanner planner = (IPlanner) context.getService(reference);

		try {
			return planner.getProvisioningPlan(request, provisioningContext, monitor);
		} finally {
			context.ungetService(reference);
		}
	}

	private IStatus executeOperands(Operand[] operands, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IEngine.SERVICE_NAME);
		IEngine engine = (IEngine) context.getService(reference);
		try {
			IPhaseSet phaseSet = engine.createPhaseSetExcluding(new String[] {IPhaseSet.PHASE_COLLECT, IPhaseSet.PHASE_CHECK_TRUST});
			IProvisioningPlan plan = engine.createCustomPlan(profile, operands, provisioningContext);
			return engine.perform(plan, phaseSet, monitor);
		} finally {
			context.ungetService(reference);
		}
	}

	private IStatus executePlan(IProvisioningPlan plan, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IEngine.SERVICE_NAME);
		IEngine engine = (IEngine) context.getService(reference);
		try {
			IPhaseSet phaseSet = engine.createPhaseSetExcluding(new String[] {IPhaseSet.PHASE_COLLECT, IPhaseSet.PHASE_CHECK_TRUST});

			if (plan.getInstallerPlan() != null) {
				IStatus installerPlanStatus = engine.perform(plan.getInstallerPlan(), phaseSet, monitor);
				if (!installerPlanStatus.isOK())
					return installerPlanStatus;

				applyConfiguration(true);
			}
			return engine.perform(plan, phaseSet, monitor);
		} finally {
			context.ungetService(reference);
		}
	}

	/*
	 * Write out the configuration file.
	 */
	private IStatus applyConfiguration(boolean isInstaller) {
		if (!isInstaller && isReconciliationApplicationRunning())
			return Status.OK_STATUS;
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(Configurator.class.getName());
		Configurator configurator = (Configurator) context.getService(reference);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, "Unexpected failure applying configuration", e); //$NON-NLS-1$
		} finally {
			context.ungetService(reference);
		}
		return Status.OK_STATUS;
	}

	static boolean isReconciliationApplicationRunning() {
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
		if (info == null)
			return false;
		String[] args = info.getCommandLineArgs();
		if (args == null)
			return false;
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null && RECONCILER_APPLICATION_ID.equals(args[i].trim()))
				return true;
		}
		return false;
	}
}
