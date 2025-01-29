/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.net.URI;
import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;

public class UpdateDescriptor implements IUpdateDescriptor {
	private final Collection<IMatchExpression<IInstallableUnit>> descriptors;

	private final String description;
	private final int severity;
	private final URI location;

	public UpdateDescriptor(Collection<IMatchExpression<IInstallableUnit>> descriptors, int severity, String description, URI location) {
		this.descriptors = descriptors;
		this.severity = severity;
		this.description = description;
		this.location = location;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getSeverity() {
		return severity;
	}

	@Override
	public boolean isUpdateOf(IInstallableUnit iu) {
		return descriptors.iterator().next().isMatch(iu);
	}

	@Override
	public Collection<IMatchExpression<IInstallableUnit>> getIUsBeingUpdated() {
		return descriptors;
	}

	@Override
	public URI getLocation() {
		return location;
	}
}
