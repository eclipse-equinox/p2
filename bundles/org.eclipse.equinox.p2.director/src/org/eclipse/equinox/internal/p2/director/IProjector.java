/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * Created as part of bug 303936. Created an interface so we could have 2 different projector implementations
 * depending on what the user wants.
 * 
 * @since 1.0.6
 */
public interface IProjector {

	public abstract void encode(IInstallableUnit[] ius, IProgressMonitor monitor);

	public abstract IStatus invokeSolver(IProgressMonitor monitor);

	public abstract Collection extractSolution();

}