/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
	private Collection<IMatchExpression<IInstallableUnit>> descriptors;

	private String description;
	private int severity;
	private URI location;

	public UpdateDescriptor(Collection<IMatchExpression<IInstallableUnit>> descriptors, int severity, String description, URI location) {
		this.descriptors = descriptors;
		this.severity = severity;
		this.description = description;
		this.location = location;
	}

	public String getDescription() {
		return description;
	}

	public int getSeverity() {
		return severity;
	}

	public boolean isUpdateOf(IInstallableUnit iu) {
		return descriptors.iterator().next().isMatch(iu);
	}

	public Collection<IMatchExpression<IInstallableUnit>> getIUsBeingUpdated() {
		return descriptors;
	}

	public URI getLocation() {
		return location;
	}
}
