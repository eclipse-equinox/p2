/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.extensionlocation.Constants;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
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

	public class ListCollector extends Collector {
		public List getList() {
			return super.getList();
		}
	}

	private static final String CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
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
			repositoryMap.put(repository.getLocation().toExternalForm(), repository);
		}
	}

	/*
	 * Synchronize the profile with the list of metadata repositories.
	 */
	public IStatus synchronize(IProgressMonitor monitor) {
		readTimestamps();
		if (isUpToDate())
			return Status.OK_STATUS;

		IStatus status = synchronizeCacheExtensions();
		if (!status.isOK())
			return status;

		ProvisioningContext context = getContext();
		ProfileChangeRequest request = createProfileChangeRequest(context);

		if (request == null) {
			if (Tracing.DEBUG_RECONCILER)
				Tracing.debug("[reconciler][plan] Empty request");
			return Status.OK_STATUS;
		}
		debug(request);

		SubMonitor sub = SubMonitor.convert(monitor, 100);
		try {
			//create the provisioning plan
			ProvisioningPlan plan = createProvisioningPlan(request, context, sub.newChild(50));
			debug(request, plan);
			status = plan.getStatus();
			if (status.getSeverity() == IStatus.ERROR || plan.getStatus().getSeverity() == IStatus.CANCEL || plan.getOperands().length == 0)
				return status;

			//invoke the engine to perform installs/uninstalls
			IStatus engineResult = executePlan(plan, context, sub.newChild(50));

			if (!engineResult.isOK())
				return engineResult;
			writeTimestamps();

			applyConfiguration();

			return status;
		} finally {
			sub.done();
		}
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
				timestamps.save(os, "Timestamps for " + profile.getProfileId()); //$NON-NLS-1$
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
		//Backward compatibility to be removed post M7
		if (profile.query(new InstallableUnitQuery("org.eclipse.equinox.p2.dropins"), new Collector(), null).size() > 0)
			return false;
		//End of backward compatibility to be removed post M7
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
				repoURLs.add(new URL((String) iterator.next()));
			} catch (MalformedURLException e) {
				//ignore
			}
		}
		ProvisioningContext result = new ProvisioningContext((URL[]) repoURLs.toArray(new URL[repoURLs.size()]));
		result.setArtifactRepositories(new URL[0]);
		return result;
	}

	private IStatus synchronizeCacheExtensions() {
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
				IArtifactRepository repository = Activator.loadArtifactRepository(new URL(repositoryId), null);

				if (repository instanceof IFileArtifactRepository) {
					currentExtensions.add(repositoryId);
					buffer.append(repositoryId);
					if (it.hasNext())
						buffer.append(PIPE);
				}
			} catch (ProvisionException e) {
				// ignore
			} catch (MalformedURLException e) {
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
			return Status.OK_STATUS;

		Operand operand = new PropertyOperand(CACHE_EXTENSIONS, previousExtensionsProperty, currentExtensionsProperty);

		return executeOperands(new ProvisioningContext(new URL[0]), new Operand[] {operand}, null);
	}

	public ProfileChangeRequest createProfileChangeRequest(ProvisioningContext context) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);

		boolean resolve = Boolean.valueOf(profile.getProperty("org.eclipse.equinox.p2.resolve")).booleanValue();
		if (resolve)
			request.removeProfileProperty("org.eclipse.equinox.p2.resolve");

		List toAdd = new ArrayList();
		List toRemove = new ArrayList();

		//Backward compatibility
		Collector collect = profile.query(new InstallableUnitQuery("org.eclipse.equinox.p2.dropins"), new Collector(), null); //$NON-NLS-1$
		toRemove.addAll(collect.toCollection());
		//End of backward compatibility

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
				if (Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_TYPE_GROUP)).booleanValue())
					request.setInstallableUnitProfileProperty(iu, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
				// mark all IUs with special property
				request.setInstallableUnitProfileProperty(iu, PROP_FROM_DROPINS, Boolean.TRUE.toString());
				request.setInstallableUnitInclusionRules(iu, PlannerHelper.createOptionalInclusionRule(iu));
				request.setInstallableUnitProfileProperty(iu, IInstallableUnit.PROP_PROFILE_LOCKED_IU, Integer.toString(IInstallableUnit.LOCK_UNINSTALL));
				toAdd.add(iu);

				if (!foundIUsToAdd && !availableProfileIUs.contains(iu)) {
					foundIUsToAdd = true;
				}
			}
		}

		// get all IUs from profile with marked property (existing)
		Collector dropinIUs = profile.query(new IUProfilePropertyQuery(profile, PROP_FROM_DROPINS, Boolean.toString(true)), new Collector(), null);
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
				request.removeInstallableUnitProfileProperty(iu, IInstallableUnit.PROP_PROFILE_LOCKED_IU);
				continue;
			}
			// remove the IUs that are in the intersection between the 2 sets
			if (all.contains(iu))
				toAdd.remove(iu);
			else
				toRemove.add(iu);
		}

		if (!foundIUsToAdd && toRemove.isEmpty() && !resolve)
			return null;

		context.setExtraIUs(toAdd);
		request.addInstallableUnits((IInstallableUnit[]) toAdd.toArray(new IInstallableUnit[toAdd.size()]));
		request.removeInstallableUnits((IInstallableUnit[]) toRemove.toArray(new IInstallableUnit[toRemove.size()]));
		return request;
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

	private ProvisioningPlan createProvisioningPlan(ProfileChangeRequest request, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IPlanner.class.getName());
		IPlanner planner = (IPlanner) context.getService(reference);

		try {
			return planner.getProvisioningPlan(request, provisioningContext, monitor);
		} finally {
			context.ungetService(reference);
		}
	}

	private IStatus executePlan(ProvisioningPlan plan, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		Operand[] operands = plan.getOperands();
		return executeOperands(provisioningContext, operands, monitor);
	}

	private IStatus executeOperands(ProvisioningContext provisioningContext, Operand[] operands, IProgressMonitor monitor) {
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(IEngine.class.getName());
		IEngine engine = (IEngine) context.getService(reference);
		try {
			PhaseSet phaseSet = DefaultPhaseSet.createDefaultPhaseSet(DefaultPhaseSet.PHASE_CHECK_TRUST);
			IStatus engineResult = engine.perform(profile, phaseSet, operands, provisioningContext, monitor);
			return engineResult;
		} finally {
			context.ungetService(reference);
		}
	}

	/*
	 * Write out the configuration file.
	 */
	private void applyConfiguration() {
		if (isReconciliationApplicationRunning())
			return;
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(Configurator.class.getName());
		Configurator configurator = (Configurator) context.getService(reference);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unexpected failure applying configuration", e)); //$NON-NLS-1$
		} finally {
			context.ungetService(reference);
		}
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

	private void debug(ProfileChangeRequest request) {
		if (!Tracing.DEBUG_RECONCILER) {
			return;
		}
		final String PREFIX = "[reconciler][request] "; //$NON-NLS-1$
		IInstallableUnit[] toAdd = request.getAddedInstallableUnits();
		if (toAdd == null || toAdd.length == 0) {
			Tracing.debug(PREFIX + "No installable units to add."); //$NON-NLS-1$
		} else {
			for (int i = 0; i < toAdd.length; i++) {
				Tracing.debug(PREFIX + "Adding IU: " + toAdd[i].getId() + ' ' + toAdd[i].getVersion()); //$NON-NLS-1$
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
	}

	private void debug(ProfileChangeRequest request, ProvisioningPlan plan) {
		if (!Tracing.DEBUG_RECONCILER)
			return;
		final String PREFIX = "[reconciler][plan] "; //$NON-NLS-1$
		//get the request
		List toAdd = new ArrayList();
		toAdd.addAll(Arrays.asList(request.getAddedInstallableUnits()));
		List toRemove = new ArrayList();
		toRemove.addAll(Arrays.asList(request.getRemovedInstallableUnits()));

		//remove from the request everything what is in the plan
		Operand[] ops = plan.getOperands();
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuo = (InstallableUnitOperand) ops[i];
				if (iuo.first() == null && iuo.second() != null) {
					toAdd.remove(iuo.second());
				}
				if (iuo.first() != null && iuo.second() == null) {
					toRemove.remove(iuo.first());
				}
			}
		}
		//if anything is left in the request, then something is wrong with the plan
		if (toAdd.size() == 0 && toRemove.size() == 0) {
			Tracing.debug(PREFIX + "Plan matches the request"); //$NON-NLS-1$
		}
		if (toAdd.size() != 0) {
			Tracing.debug(PREFIX + "Some units will not be installed, because they are already installed or there are dependency issues:"); //$NON-NLS-1$
			for (int i = 0; i < toAdd.size(); i++) {
				Tracing.debug(PREFIX + toAdd.get(i));
			}
		}
		if (toRemove.size() != 0) {
			Tracing.debug(PREFIX + "Some units will not be uninstalled:"); //$NON-NLS-1$
			for (int i = 0; i < toRemove.size(); i++) {
				Tracing.debug(PREFIX + toRemove.get(i));
			}
		}
	}
}
