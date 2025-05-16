/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery;

/**
 * A policy defines what is permitted.
 *
 * @author David Green
 */
public class Policy {

	/**
	 * Define system property <code>org.eclipse.mylyn.internal.discovery.core.model.Policy.permissive=true</code> to
	 * allow for categories to be permitted by anyone. For testing purposes.
	 */
	private static final boolean PERMISSIVE = Boolean.getBoolean(Policy.class.getName() + ".permissive"); //$NON-NLS-1$

	private static final Policy DEFAULT = new Policy(false);

	private final boolean permitCategories;

	public Policy(boolean permitCategories) {
		this.permitCategories = permitCategories;
	}

	public boolean isPermitCategories() {
		return permitCategories || PERMISSIVE;
	}

	public static Policy defaultPolicy() {
		return DEFAULT;
	}
}
