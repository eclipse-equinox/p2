/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.rollback;

import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.IUTransformationHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.osgi.framework.Version;

public class FormerState {
	public static final String IUPROP_PREFIX = "---IUPROPERTY---"; //$NON-NLS-1$
	public static final String IUPROP_POSTFIX = "---IUPROPERTYKEY---"; //$NON-NLS-1$
	URL location = null;

	Hashtable generatedIUs = new Hashtable(); //key profile id, value the iu representing this profile

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
			IMetadataRepository repository = manager.createRepository(location, "Agent rollback repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY); //$NON-NLS-1$
			repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return repository;
		} catch (ProvisionException e) {
			LogHelper.log(e);
		}
		throw new IllegalStateException("Unable to open or create Agent's rollback repository"); //$NON-NLS-1$
	}

	IInstallableUnit profileToIU(IProfile profile) {
		InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
		result.setProperty(IInstallableUnit.PROP_TYPE_PROFILE, Boolean.TRUE.toString());
		result.setId(profile.getProfileId());
		result.setVersion(new Version(0, 0, 0, Long.toString(System.currentTimeMillis())));
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

		//TODO Do we need to mark profile with a special marker
		return MetadataFactory.createInstallableUnit(result);
	}
	//	private copyProperty(Profile p) {
	//		Map profileProperties = p.getValues();
	//	}
}
