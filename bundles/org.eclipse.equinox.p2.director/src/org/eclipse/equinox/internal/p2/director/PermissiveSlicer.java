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
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class PermissiveSlicer extends Slicer {
	private boolean includeOptionalDependencies; //Cause optional dependencies not be followed as part of the
	private boolean everythingGreedy;
	private boolean considerFilter;
	private boolean considerOnlyStrictDependency;
	private boolean evalFilterTo;
	private boolean onlyFilteredRequirements;

	public PermissiveSlicer(IQueryable<IInstallableUnit> input, Dictionary<? extends Object, ? extends Object> context, boolean includeOptionalDependencies, boolean everythingGreedy, boolean evalFilterTo, boolean considerOnlyStrictDependency, boolean onlyFilteredRequirements) {
		super(input, context, true);
		this.considerFilter = (context != null && context.size() > 1) ? true : false;
		this.includeOptionalDependencies = includeOptionalDependencies;
		this.everythingGreedy = everythingGreedy;
		this.evalFilterTo = evalFilterTo;
		this.considerOnlyStrictDependency = considerOnlyStrictDependency;
		this.onlyFilteredRequirements = onlyFilteredRequirements;
	}

	protected boolean isApplicable(IInstallableUnit iu) {
		if (considerFilter)
			return super.isApplicable(iu);
		if (iu.getFilter() == null)
			return true;
		return evalFilterTo;
	}

	protected boolean isApplicable(IRequirement req) {
		//Every filter in this method needs to continue except when the filter does not pass
		if (!includeOptionalDependencies)
			if (req.getMin() == 0)
				return false;

		if (considerOnlyStrictDependency) {
			if (req instanceof IRequiredCapability) {
				IRequiredCapability reqCap = (IRequiredCapability) req;
				if (!reqCap.getRange().getMinimum().equals(reqCap.getRange().getMaximum()))
					return false;
			}
		}

		//deal with filters
		if (considerFilter) {
			if (onlyFilteredRequirements && req.getFilter() == null) {
				return false;
			}
			return super.isApplicable(req);
		}
		if (req.getFilter() == null) {
			if (onlyFilteredRequirements)
				return false;
			return true;
		}
		return evalFilterTo;
	}

	protected boolean isGreedy(IRequirement req) {
		if (everythingGreedy) {
			return true;
		}
		return super.isGreedy(req);
	}
}
