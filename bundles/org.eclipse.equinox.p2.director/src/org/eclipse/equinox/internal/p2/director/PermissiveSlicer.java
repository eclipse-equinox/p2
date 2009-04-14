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
	private boolean includeOptionalDependencies; //Cause optional dependencies not be followed as part of the
	private boolean everythingGreedy;
	private boolean considerFilter;
	private boolean considerOnlyStrictDependency;
	private boolean evalFilterTo;

	public PermissiveSlicer(IQueryable input, Dictionary context, boolean includeOptionalDependencies, boolean everythingGreedy, boolean evalFilterTo, boolean considerOnlyStrictDependency) {
		super(input, context, true);
		this.considerFilter = (context != null && context.size() > 1) ? true : false;
		this.includeOptionalDependencies = includeOptionalDependencies;
		this.everythingGreedy = everythingGreedy;
		this.evalFilterTo = evalFilterTo;
		this.considerOnlyStrictDependency = considerOnlyStrictDependency;
	}

	protected boolean isApplicable(IInstallableUnit iu) {
		if (considerFilter)
			return super.isApplicable(iu);
		if (iu.getFilter() == null)
			return true;
		return evalFilterTo;
	}

	protected boolean isApplicable(IRequiredCapability req) {
		//Every filter in this method needs to continue except when the filter does not pass
		if (!includeOptionalDependencies)
			if (req.isOptional())
				return false;

		if (considerOnlyStrictDependency) {
			if (!req.getRange().getMinimum().equals(req.getRange().getMaximum()))
				return false;
		}

		//deal with filters
		if (considerFilter)
			return super.isApplicable(req);
		if (req.getFilter() == null)
			return true;
		return evalFilterTo;
	}

	protected boolean isGreedy(IRequiredCapability req) {
		if (everythingGreedy) {
			return true;
		}
		return super.isGreedy(req);
	}
}
