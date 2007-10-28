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
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URL;
import java.util.EventObject;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IResolvedInstallableUnit;
import org.osgi.framework.ServiceReference;

public class MetadataCache extends URLMetadataRepository {

	static final private String REPOSITORY_NAME = "Agent Metadata Cache"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = MetadataCache.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);

	transient private ServiceReference busReference;
	transient private ProvisioningEventBus bus;

	// These are always created with file: URLs.  At least for now...
	public MetadataCache(URL repoPath) throws RepositoryCreationException {
		super(REPOSITORY_NAME, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), repoPath, null, null);
		content = getActualLocation(location);
		new SimpleMetadataRepositoryFactory().load(location);
		// Set property indicating that the metadata cache is an implementation detail.
		getModifiableProperties().setProperty(IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());

		// TODO: We should check for writing permission here, otherwise it may be too late
		busReference = Activator.getContext().getServiceReference(ProvisioningEventBus.class.getName());
		bus = (ProvisioningEventBus) Activator.getContext().getService(busReference);
		bus.addListener(new ProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) { //TODO This dependency on InstallableUnitEvent is not great
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.isPre())
						return;
					//TODO what about uninstall??
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

	public MetadataCache() {
		super();
	}

	protected void persist() {
		if (!getContentURL().getProtocol().equals("file"))
			throw new IllegalStateException("only file: URLs are supported for the metadata cache");
		File contentFile = new File(getContentURL().getFile());
		if (!contentFile.getParentFile().exists() && !contentFile.getParentFile().mkdirs())
			throw new RuntimeException("can't persist the metadata cache");
		try {
			OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(contentFile, false));;
			MetadataRepositoryIO.write(this, outputStream);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("can't persist the metadata cache");
		}
	}

}
