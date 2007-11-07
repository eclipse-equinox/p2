/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.installregistry;

import java.io.*;
import java.net.URL;
import java.util.EventObject;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.metadata.repository.*;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IResolvedInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.osgi.framework.ServiceReference;

public class MetadataCache extends URLMetadataRepository {

	static final private String REPOSITORY_NAME = "Agent Metadata Cache"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = MetadataCache.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);

	transient private ServiceReference busReference;
	transient private ProvisioningEventBus bus;

	public MetadataCache() {
		super();
	}

	public static MetadataCache getCacheInstance(MetadataRepositoryManager manager) {
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
		URL url = (agentLocation != null ? agentLocation.getMetadataRepositoryURL() : null);
		URL content = getActualLocation(url);
		IMetadataRepository repository = manager.loadRepository(content, null);
		if (repository == null || !(repository instanceof MetadataCache)) {
			repository = new MetadataCache(url);
			((MetadataCache) repository).initializeAfterLoad(url);
			manager.addRepository(repository);
		}
		return (MetadataCache) repository;
	}

	// These are always created with file: URLs.  At least for now...
	public MetadataCache(URL cacheLocation) {
		super(REPOSITORY_NAME, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), cacheLocation, null, null);
		content = getActualLocation(location);
		// Set property indicating that the metadata cache is an implementation detail.
		getModifiableProperties().put(IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());
	}

	public void initializeAfterLoad(URL repoLocation) {
		super.initializeAfterLoad(repoLocation);

		// TODO: We should check for writing permission here, otherwise it may be too late
		busReference = EngineActivator.getContext().getServiceReference(ProvisioningEventBus.class.getName());
		bus = (ProvisioningEventBus) EngineActivator.getContext().getService(busReference);
		bus.addListener(new ProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) { //TODO This dependency on InstallableUnitEvent is not great
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.isPre())
						return;
					// TODO: what about uninstall??
					if (event.isPost() && event.getResult().isOK() && event.isInstall()) {
						IResolvedInstallableUnit installedIU = event.getOperand().second();
						if (installedIU != null)
							units.add(installedIU.getOriginal());
						return;
					}
				}
				if (o instanceof CommitOperationEvent)
					persist();
				if (o instanceof RollbackOperationEvent)
					new SimpleMetadataRepositoryFactory().restore(MetadataCache.this, location);
			}
		});
	}

	protected void persist() {
		if (!getContentURL().getProtocol().equals("file"))
			throw new IllegalStateException("only file: URLs are supported for the metadata cache");
		File contentFile = new File(getContentURL().getFile());
		if (!contentFile.getParentFile().exists() && !contentFile.getParentFile().mkdirs())
			throw new RuntimeException("can't persist the metadata cache");
		try {
			OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(contentFile, false));;
			new MetadataRepositoryIO().write(this, outputStream);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("can't persist the metadata cache");
		}
	}

}
