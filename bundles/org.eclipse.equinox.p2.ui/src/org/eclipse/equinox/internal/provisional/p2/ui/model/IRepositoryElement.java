/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.model;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;

/**
 * Interface for elements that represent repositories.
 * 
 * @since 3.4
 */
public interface IRepositoryElement {

	public URL getLocation();

	public String getName();

	public String getDescription();

	public boolean isEnabled();

	public void setEnabled(boolean enabled);

	public IRepository getRepository(IProgressMonitor monitor);
}
