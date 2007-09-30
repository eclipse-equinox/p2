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
package org.eclipse.equinox.p2.query;

import java.util.Iterator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.osgi.service.resolver.VersionRange;

public interface IQueryable {

	IInstallableUnit[] query(String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor progress);

	Iterator getIterator(String id, VersionRange range, RequiredCapability[] requirements, boolean and);

}
