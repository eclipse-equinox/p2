/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.Dictionary;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;

public class PermissiveSlicer extends Slicer {
	private boolean skipOptionalRequirement;
	private boolean everythingGreedy;
	private boolean considerFilter;
	private boolean considerStrictDependency;

	public PermissiveSlicer(IQueryable input, Dictionary context, boolean skipOptionalRequirement, boolean everythingGreedy, boolean considerFilter, boolean considerStrictDependency) {
		super(input, context);
		this.skipOptionalRequirement = skipOptionalRequirement;
		this.everythingGreedy = everythingGreedy;
		this.considerFilter = considerFilter;
		this.considerStrictDependency = considerStrictDependency;
	}

	protected boolean isApplicable(IInstallableUnit iu) {
		return true;
	}

	protected boolean isApplicable(IRequiredCapability req) {
		//Every filter in this method needs to continue except when the filter does not pass
		if (skipOptionalRequirement)
			if (req.isOptional())
				return false;

		if (considerStrictDependency) {
			if (!req.getRange().getMinimum().equals(req.getRange().getMaximum()))
				return false;
		}

		if (considerFilter)
			return super.isApplicable(req);
		return true;
	}

	protected boolean isGreedy(IRequiredCapability req) {
		if (everythingGreedy) {
			return true;
		}
		return super.isGreedy(req);
	}
}
