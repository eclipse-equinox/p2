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

import java.util.Collection;
import java.util.Collections;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * Element representing a single IU change (added, removed, or updated) within
 * an installation-history diff. Used by the diff tree in
 * {@code RevertProfilePage}.
 *
 * @since 2.9
 */
public class HistoryDiffElement extends ProvElement implements IIUElement {

	public enum ChangeType {
		ADDED, REMOVED, UPDATED
	}

	private final IInstallableUnit iu;

	private final IInstallableUnit previousIU;

	private final ChangeType changeType;

	public HistoryDiffElement(Object parent, IInstallableUnit iu, IInstallableUnit previousIU, ChangeType changeType) {
		super(parent);
		this.iu = iu;
		this.previousIU = previousIU;
		this.changeType = changeType;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public IInstallableUnit getPreviousIU() {
		return previousIU;
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
		String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		return name != null ? name : iu.getId();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IInstallableUnit.class) {
			return (T) iu;
		}
		return super.getAdapter(adapter);
	}

	@Override
	public IInstallableUnit getIU() {
		return iu;
	}

	@Override
	public boolean shouldShowSize() {
		return false;
	}

	@Override
	public boolean shouldShowVersion() {
		return true;
	}

	@Override
	public long getSize() {
		return -1L; // SIZE_UNKNOWN
	}

	@Override
	public void computeSize(IProgressMonitor monitor) {
		// not needed
	}

	@Override
	public Collection<IRequirement> getRequirements() {
		return Collections.unmodifiableCollection(iu.getRequirements());
	}

	@Override
	public boolean shouldShowChildren() {
		return false;
	}

	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}
}
