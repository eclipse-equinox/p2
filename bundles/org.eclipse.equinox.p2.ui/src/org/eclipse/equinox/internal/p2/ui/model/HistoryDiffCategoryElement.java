/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.ProvUIImages;

/**
 * A top-level category node ("Installed", "Updated", "Removed") shown in the
 * installation-history diff tree of {@code RevertProfilePage}. Its children
 * are {@link HistoryDiffElement} instances.
 *
 * @since 2.9
 */
public class HistoryDiffCategoryElement extends ProvElement {

	private final String label;
	private final HistoryDiffElement[] children;
	private final HistoryDiffElement.ChangeType changeType;

	public HistoryDiffCategoryElement(Object parent, String label, HistoryDiffElement.ChangeType changeType,
			HistoryDiffElement[] children) {
		super(parent);
		this.label = label;
		this.changeType = changeType;
		this.children = children;
	}

	@Override
	protected String getImageId(Object obj) {
		return switch (changeType) {
			case ADDED -> ProvUIImages.IMG_ADDED;
			case REMOVED -> ProvUIImages.IMG_REMOVED;
			case UPDATED -> ProvUIImages.IMG_CHANGED;
		};
	}

	@Override
	public String getLabel(Object o) {
		return label + " (" + children.length + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Object[] getChildren(Object o) {
		return children;
	}

	public boolean hasChildren() {
		return children.length > 0;
	}
}
