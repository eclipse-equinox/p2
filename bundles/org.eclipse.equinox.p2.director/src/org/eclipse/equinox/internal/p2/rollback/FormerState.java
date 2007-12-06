/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.rollback;

import java.net.URL;
import java.util.EventObject;
import java.util.Hashtable;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.IUTransformationHelper;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.osgi.framework.Version;

public class FormerState {
	URL location = null;

	Hashtable generatedIUs = new Hashtable(); //key profile id, value the iu representing this profile

	public FormerState(URL repoLocation) {
		if (repoLocation == null)
			throw new IllegalArgumentException("Repository location can't be null"); //$NON-NLS-1$
		ProvisioningEventBus eventBus = (ProvisioningEventBus) ServiceHelper.getService(DirectorActivator.context, ProvisioningEventBus.class.getName());
		location = repoLocation;

		//listen for pre-event. to memorize the state of the profile
		eventBus.addListener(new SynchronousProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof BeginOperationEvent) {
					BeginOperationEvent event = (BeginOperationEvent) o;
					IInstallableUnit iuForProfile = profileToIU(event.getProfile());
					generatedIUs.put(event.getProfile().getProfileId(), iuForProfile);
				} else if (o instanceof CommitOperationEvent) {
					CommitOperationEvent event = (CommitOperationEvent) o;
					getRepository().addInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) generatedIUs.get(event.getProfile().getProfileId())});
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

	AbstractMetadataRepository getRepository() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(DirectorActivator.context, IMetadataRepositoryManager.class.getName());
		return (AbstractMetadataRepository) manager.loadRepository(location, null);
	}

	IInstallableUnit profileToIU(Profile toConvert) {
		InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
		result.setProperty(IInstallableUnit.PROP_PROFILE_IU_KEY, Boolean.TRUE.toString());
		result.setId(toConvert.getProfileId());
		result.setVersion(new Version(0, 0, 0, Long.toString(System.currentTimeMillis())));
		result.setRequiredCapabilities(IUTransformationHelper.toRequirements(toConvert.getInstallableUnits(), false));
		//TODO Need to do the properties in the profile
		//TODO Do we need to mark profile with a special marker
		return MetadataFactory.createInstallableUnit(result);
	}

	//	private copyProperty(Profile p) {
	//		Map profileProperties = p.getValues();
	//	}
}
