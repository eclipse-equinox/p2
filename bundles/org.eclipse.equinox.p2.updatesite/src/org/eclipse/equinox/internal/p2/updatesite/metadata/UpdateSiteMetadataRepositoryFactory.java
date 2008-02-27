/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.Messages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class UpdateSiteMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	public IMetadataRepository create(URL location, String name, String type) {
		return null;
	}

	private IMetadataRepository validateAndLoad(URL location, IProgressMonitor monitor, boolean doLoad) throws ProvisionException {
		if (!location.getPath().endsWith("site.xml")) { //$NON-NLS-1$
			String msg = NLS.bind(Messages.UpdateSiteMetadataRepositoryFactory_InvalidRepositoryLocation, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, null));
		}
		URL localRepositoryURL = null;
		InputStream is = null;
		Checksum checksum = new CRC32();

		try {
			String stateDirName = Integer.toString(location.toExternalForm().hashCode());
			File bundleData = Activator.getBundleContext().getDataFile(null);
			File stateDir = new File(bundleData, stateDirName);
			localRepositoryURL = stateDir.toURL();
			is = new CheckedInputStream(new BufferedInputStream(location.openStream()), checksum);

			if (doLoad)
				return new UpdateSiteMetadataRepository(location, localRepositoryURL, is, checksum);
		} catch (MalformedURLException e) {
			String msg = NLS.bind(Messages.UpdateSiteMetadataRepositoryFactory_InvalidRepositoryLocation, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.UpdateSiteMetadataRepositoryFactory_ErrorReadingSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			safeClose(is);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#validate(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus validate(URL location, IProgressMonitor monitor) {
		try {
			validateAndLoad(location, monitor, false);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IMetadataRepository load(URL location, IProgressMonitor monitor) throws ProvisionException {
		return validateAndLoad(location, monitor, true);
	}

	private void safeClose(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException e) {
			//ignore
		}
	}
}
