/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public interface IInstallableUnitFragment extends IInstallableUnit {

	public static final ProvidedCapability FRAGMENT_CAPABILITY = new ProvidedCapability(IU_KIND_NAMESPACE, "iu.fragment", new Version(1, 0, 0)); //$NON-NLS-1$

	public abstract String getHostId();

	public abstract VersionRange getHostVersionRange();

}