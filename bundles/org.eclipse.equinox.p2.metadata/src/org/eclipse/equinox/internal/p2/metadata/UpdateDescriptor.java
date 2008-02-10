/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IUpdateDescriptor;
import org.eclipse.osgi.service.resolver.VersionRange;

public class UpdateDescriptor implements IUpdateDescriptor {
	private String description;
	private String id;
	private VersionRange range;
	private int severity;

	public UpdateDescriptor(String id, VersionRange range, int severity, String description) {
		this.id = id;
		this.range = range;
		this.severity = severity;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public VersionRange getRange() {
		return range;
	}

	public int getSeverity() {
		return severity;
	}

	public boolean isUpdateOf(IInstallableUnit iu) {
		if (!id.equals(iu.getId()))
			return false;
		if (range.isIncluded(iu.getVersion()))
			return true;
		return false;
	}
}
