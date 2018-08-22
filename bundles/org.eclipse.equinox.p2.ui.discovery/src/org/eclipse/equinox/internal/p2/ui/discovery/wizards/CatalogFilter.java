/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
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

package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

/**
 * @author David Green
 */
public abstract class CatalogFilter {

	public abstract boolean select(CatalogItem item);

}
