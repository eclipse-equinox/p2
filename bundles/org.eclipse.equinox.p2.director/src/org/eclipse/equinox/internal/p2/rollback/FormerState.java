/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.rollback;

import java.util.EventObject;
import java.util.Hashtable;
import org.eclipse.equinox.internal.p2.director.IUTransformationHelper;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.osgi.framework.Version;

public class FormerState {
	IMetadataRepository storage = null;

	Hashtable generatedIUs = new Hashtable(); //key profile id, value the iu representing this profile

	public FormerState(ProvisioningEventBus bus, IMetadataRepository repo) {
		if (bus == null || repo == null) {
			throw new IllegalArgumentException("bus and storage can' be null"); //$NON-NLS-1$
		}
		storage = repo;

		//listen for pre-event. to memorize the state of the profile
		bus.addListener(new SynchronousProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof BeginOperationEvent) {
					BeginOperationEvent event = (BeginOperationEvent) o;
					IInstallableUnit iuForProfile = profileToIU(event.getProfile());
					generatedIUs.put(event.getProfile().getProfileId(), iuForProfile);
				} else if (o instanceof CommitOperationEvent) {
					CommitOperationEvent event = (CommitOperationEvent) o;
					storage.addInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) generatedIUs.get(event.getProfile().getProfileId())});
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

	IInstallableUnit profileToIU(Profile toConvert) {
		InstallableUnit result = new InstallableUnit();
		result.setProperty(IInstallableUnitConstants.PROFILE_IU_KEY, Boolean.TRUE.toString());
		result.setId(toConvert.getProfileId());
		result.setVersion(new Version(0, 0, 0, Long.toString(System.currentTimeMillis())));
		result.setRequiredCapabilities(IUTransformationHelper.toRequirements(toConvert.getInstallableUnits(), false));
		//TODO Need to do the properties in the profile
		//TODO Do we need to mark profile with a special marker
		return result;
	}

	//	private copyProperty(Profile p) {
	//		Map profileProperties = p.getValues();
	//	}
}
