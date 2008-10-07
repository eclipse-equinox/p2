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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * Element wrapper class for an IU that shows all uncategorized IU's.
 * 
 * @since 3.4
 */
public class UncategorizedCategoryElement extends CategoryElement {

	public UncategorizedCategoryElement(Object parent, IInstallableUnit iu) {
		super(parent, iu);
	}
}
