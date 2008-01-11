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
package org.eclipse.equinox.spi.p2.metadata.repository;

import java.net.URL;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

public interface IMetadataRepositoryFactory {

	public IMetadataRepository load(URL location) throws ProvisionException;

	public IMetadataRepository create(URL location, String name, String type) throws ProvisionException;
}
