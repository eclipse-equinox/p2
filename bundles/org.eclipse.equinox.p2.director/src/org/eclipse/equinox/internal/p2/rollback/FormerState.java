/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.rollback;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.osgi.framework.Version;

public class FormerState {

	public static final String IUPROP_PREFIX = "---IUPROPERTY---"; //$NON-NLS-1$
	public static final String IUPROP_POSTFIX = "---IUPROPERTYKEY---"; //$NON-NLS-1$
	private static long lastTimestamp;
	URL location = null;

	Hashtable generatedIUs = new Hashtable(); //key profile id, value the iu representing this profile

	private synchronized static long uniqueTimestamp() {
		long timewaited = 0;
		long timestamp = System.currentTimeMillis();
		while (timestamp == lastTimestamp) {
			if (timewaited > 1000)
				throw new IllegalStateException("uniquetimestamp failed"); //$NON-NLS-1$
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// reset interrupted status
				Thread.currentThread().interrupt();
			}
			timewaited += 10;
			timestamp = System.currentTimeMillis();
		}
		lastTimestamp = timestamp;
		return timestamp;
	}

	public FormerState(URL repoLocation) {
		if (repoLocation == null)
			throw new IllegalArgumentException("Repository location can't be null"); //$NON-NLS-1$
		IProvisioningEventBus eventBus = (IProvisioningEventBus) ServiceHelper.getService(DirectorActivator.context, IProvisioningEventBus.SERVICE_NAME);
		location = repoLocation;

		//listen for pre-event. to snapshot the profile
		eventBus.addListener(new SynchronousProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof BeginOperationEvent) {
					BeginOperationEvent event = (BeginOperationEvent) o;
					IInstallableUnit iuForProfile = profileToIU(event.getProfile());
					generatedIUs.put(event.getProfile().getProfileId(), iuForProfile);
				} else if (o instanceof ProfileEvent) {
					ProfileEvent event = (ProfileEvent) o;
					if (event.getReason() == ProfileEvent.CHANGED)
						getRepository().addInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) generatedIUs.get(event.getProfileId())});
					return;
				} else if (o instanceof RollbackOperationEvent) {
					RollbackOperationEvent event = (RollbackOperationEvent) o;
					generatedIUs.remove(event.getProfile().getProfileId());
					return;
				}
				//TODO We need to decide what to do on profile removal				
				//				else if (o instanceof ProfileEvent) {
				//					ProfileEvent pe = (ProfileEvent) o;
				//					if (pe.getReason() == ProfileEvent.REMOVED) {
				//						profileRegistries.remove(pe.getProfile().getProfileId());
				//						persist();
				//					}
				//				}
			}

		});
	}

	IMetadataRepository getRepository() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(DirectorActivator.context, IMetadataRepositoryManager.class.getName());
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		try {
			Map properties = new HashMap(1);
			properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return manager.createRepository(location, "Agent rollback repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties); //$NON-NLS-1$
		} catch (ProvisionException e) {
			LogHelper.log(e);
		}
		throw new IllegalStateException("Unable to open or create Agent's rollback repository"); //$NON-NLS-1$
	}

	public static IInstallableUnit profileToIU(IProfile profile) {
		InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
		result.setProperty(IInstallableUnit.PROP_TYPE_PROFILE, Boolean.TRUE.toString());
		result.setId(profile.getProfileId());
		result.setVersion(new Version(0, 0, 0, Long.toString(uniqueTimestamp())));
		result.setRequiredCapabilities(IUTransformationHelper.toRequirements(profile.query(InstallableUnitQuery.ANY, new Collector(), null).iterator(), false));
		// Save the profile properties
		// TODO we aren't marking these properties in any special way to indicate they came from profile properties.  Should we?
		Map properties = profile.getProperties();
		Iterator iter = properties.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			result.setProperty(key, (String) properties.get(key));
		}
		// Save the IU profile properties
		Iterator allIUs = profile.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
		while (allIUs.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) allIUs.next();
			properties = profile.getInstallableUnitProperties(iu);
			iter = properties.keySet().iterator();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				result.setProperty(IUPROP_PREFIX + iu.getId() + IUPROP_POSTFIX + key, (String) properties.get(key));
			}
		}
		return MetadataFactory.createInstallableUnit(result);
	}

	public static IProfile IUToProfile(IInstallableUnit profileIU, IProfile profile, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
		try {
			return new FormerStateProfile(profileIU, profile, context);
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	public static ProfileChangeRequest generateProfileDeltaChangeRequest(IProfile current, IProfile target) {
		ProfileChangeRequest request = new ProfileChangeRequest(current);

		synchronizeProfileProperties(request, current, target);
		synchronizeMarkedIUs(request, current, target);
		synchronizeAllIUProperties(request, current, target);

		return request;
	}

	private static void synchronizeAllIUProperties(ProfileChangeRequest request, IProfile current, IProfile target) {
		Collection currentIUs = current.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection();
		Collection targetIUs = target.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection();
		List iusToAdd = new ArrayList(targetIUs);
		iusToAdd.remove(currentIUs);

		//additions
		for (Iterator iterator = iusToAdd.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			for (Iterator it = target.getInstallableUnitProperties(iu).entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				request.setInstallableUnitProfileProperty(iu, key, value);
			}
		}

		// updates
		List iusToUpdate = new ArrayList(targetIUs);
		iusToUpdate.remove(iusToAdd);
		for (Iterator iterator = iusToUpdate.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			Map propertiesToSet = new HashMap(target.getInstallableUnitProperties(iu));
			for (Iterator it = current.getInstallableUnitProperties(iu).entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String key = (String) entry.getKey();
				String newValue = (String) propertiesToSet.get(key);
				if (newValue == null) {
					request.removeInstallableUnitProfileProperty(iu, key);
				} else if (newValue.equals(entry.getValue()))
					propertiesToSet.remove(key);
			}

			for (Iterator it = propertiesToSet.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				request.setInstallableUnitProfileProperty(iu, key, value);
			}
		}
	}

	private static void synchronizeMarkedIUs(ProfileChangeRequest request, IProfile current, IProfile target) {
		IInstallableUnit[] currentPlannerMarkedIUs = SimplePlanner.findPlannerMarkedIUs(current);
		IInstallableUnit[] targetPlannerMarkedIUs = SimplePlanner.findPlannerMarkedIUs(target);

		//additions
		List markedIUsToAdd = new ArrayList(Arrays.asList(targetPlannerMarkedIUs));
		markedIUsToAdd.removeAll(Arrays.asList(currentPlannerMarkedIUs));
		request.addInstallableUnits((IInstallableUnit[]) markedIUsToAdd.toArray(new IInstallableUnit[markedIUsToAdd.size()]));

		// removes
		List markedIUsToRemove = new ArrayList(Arrays.asList(currentPlannerMarkedIUs));
		markedIUsToRemove.removeAll(Arrays.asList(targetPlannerMarkedIUs));
		request.removeInstallableUnits((IInstallableUnit[]) markedIUsToRemove.toArray(new IInstallableUnit[markedIUsToRemove.size()]));
	}

	private static void synchronizeProfileProperties(ProfileChangeRequest request, IProfile current, IProfile target) {
		Map profilePropertiesToSet = new HashMap(target.getProperties());
		for (Iterator it = current.getProperties().entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String key = (String) entry.getKey();

			String newValue = (String) profilePropertiesToSet.get(key);
			if (newValue == null) {
				request.removeProfileProperty(key);
			} else if (newValue.equals(entry.getValue()))
				profilePropertiesToSet.remove(key);
		}

		for (Iterator it = profilePropertiesToSet.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			request.setProfileProperty(key, value);
		}
	}

	public static class FormerStateProfile implements IProfile {

		private String profileId;
		private HashMap profileProperties = new HashMap();
		private HashMap iuProfileProperties = new HashMap();
		private Set ius = new HashSet();

		public FormerStateProfile(IInstallableUnit profileIU, IProfile profile, ProvisioningContext context) throws ProvisionException {

			String profileTypeProperty = profileIU.getProperty(IInstallableUnit.PROP_TYPE_PROFILE);
			if (profileTypeProperty == null || !Boolean.valueOf(profileTypeProperty).booleanValue())
				throw new ProvisionException(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, "Not a profile type IU"));

			profileId = profileIU.getId();
			for (Iterator it = profileIU.getProperties().entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String key = (String) entry.getKey();
				if (key.startsWith(IUPROP_PREFIX)) {
					int postIndex = key.indexOf(FormerState.IUPROP_POSTFIX, FormerState.IUPROP_PREFIX.length());
					String iuId = key.substring(FormerState.IUPROP_PREFIX.length(), postIndex);
					Map iuProperties = (Map) iuProfileProperties.get(iuId);
					if (iuProperties == null) {
						iuProperties = new HashMap();
						iuProfileProperties.put(iuId, iuProperties);
					}
					String iuPropertyKey = key.substring(postIndex + FormerState.IUPROP_POSTFIX.length());
					iuProperties.put(iuPropertyKey, entry.getValue());
				} else {
					profileProperties.put(key, entry.getValue());
				}
			}
			profileProperties.remove(IInstallableUnit.PROP_TYPE_PROFILE);

			List extraIUs = new ArrayList(profile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection());
			extraIUs.add(profileIU);

			IInstallableUnit[] availableIUs = SimplePlanner.gatherAvailableInstallableUnits((IInstallableUnit[]) extraIUs.toArray(new IInstallableUnit[extraIUs.size()]), context.getMetadataRepositories(), context, new NullProgressMonitor());

			Dictionary snapshotSelectionContext = SimplePlanner.createSelectionContext(profileProperties);
			IInstallableUnit[] allIUs = new IInstallableUnit[] {profileIU};
			Slicer slicer = new Slicer(allIUs, availableIUs, snapshotSelectionContext);
			IQueryable slice = slicer.slice(allIUs, new NullProgressMonitor());
			if (slice == null)
				throw new ProvisionException(slicer.getStatus());

			IProjector projector = ProjectorFactory.create(slice, snapshotSelectionContext);
			projector.encode(allIUs, new NullProgressMonitor());
			IStatus s = projector.invokeSolver(new NullProgressMonitor());

			if (s.getSeverity() == IStatus.ERROR) {
				//log the error from the new solver so it is not lost
				LogHelper.log(s);
				if (!"true".equalsIgnoreCase(context == null ? null : context.getProperty("org.eclipse.equinox.p2.disable.error.reporting"))) {
					//We invoke the old resolver to get explanations for now
					IStatus oldResolverStatus = new NewDependencyExpander(allIUs, null, availableIUs, snapshotSelectionContext, false).expand(new NullProgressMonitor());
					if (!oldResolverStatus.isOK())
						s = oldResolverStatus;
				}
				throw new ProvisionException(s);
			}
			ius.addAll(projector.extractSolution());
			ius.remove(profileIU);
		}

		public Map getInstallableUnitProperties(IInstallableUnit iu) {
			Map iuProperties = (Map) iuProfileProperties.get(iu.getId());
			if (iuProperties == null) {
				return Collections.EMPTY_MAP;
			}
			return Collections.unmodifiableMap(iuProperties);
		}

		public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
			return (String) getInstallableUnitProperties(iu).get(key);
		}

		public Map getLocalProperties() {
			return Collections.unmodifiableMap(profileProperties);
		}

		public String getLocalProperty(String key) {
			return (String) profileProperties.get(key);
		}

		public IProfile getParentProfile() {
			return null;
		}

		public String getProfileId() {
			return profileId;
		}

		public Map getProperties() {
			return Collections.unmodifiableMap(profileProperties);
		}

		public String getProperty(String key) {
			return (String) profileProperties.get(key);
		}

		public String[] getSubProfileIds() {
			return null;
		}

		public long getTimestamp() {
			return 0;
		}

		public boolean hasSubProfiles() {
			return false;
		}

		public boolean isRootProfile() {
			return true;
		}

		public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
			return query.perform(ius.iterator(), collector);
		}

		public Collector available(Query query, Collector collector, IProgressMonitor monitor) {
			return query(query, collector, monitor);
		}
	}
}
