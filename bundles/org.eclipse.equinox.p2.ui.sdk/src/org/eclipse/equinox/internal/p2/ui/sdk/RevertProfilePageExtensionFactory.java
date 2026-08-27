/*******************************************************************************
 * Copyright (c) 2011, 2026 Sonatype, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.equinox.p2.ui.RevertProfilePage;

public class RevertProfilePageExtensionFactory implements IExecutableExtensionFactory {

	@Override
	public Object create() {
		return new RevertProfilePage();
	}
}
