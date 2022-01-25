/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - access activator static singelton in a safe way
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.Optional;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DirectorActivator implements BundleActivator {
	public static final String PI_DIRECTOR = "org.eclipse.equinox.p2.director"; //$NON-NLS-1$
	public static volatile Optional<BundleContext> context = Optional.empty();

	@Override
	public void start(BundleContext ctx) throws Exception {
		context = Optional.of(ctx);
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		DirectorActivator.context = Optional.empty();
	}

}
