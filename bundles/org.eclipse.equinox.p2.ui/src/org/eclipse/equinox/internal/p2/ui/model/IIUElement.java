/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * Interface for elements that represent IU's.
 * 
 * @since 3.4
 */
public interface IIUElement {

	public IInstallableUnit getIU();

	public boolean shouldShowSize();

	public boolean shouldShowVersion();

	public long getSize();

	public void computeSize(IProgressMonitor monitor);

	public Collection<IRequirement> getRequirements();

	public Object getParent(Object obj);

	public boolean shouldShowChildren();
}
