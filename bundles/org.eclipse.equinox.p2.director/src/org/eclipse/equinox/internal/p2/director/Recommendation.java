/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class Recommendation {
	private RequiredCapability applyOn;
	private RequiredCapability newValue;

	public Recommendation(RequiredCapability applyOn2, RequiredCapability newValue2) {
		this.applyOn = applyOn2;
		this.newValue = newValue2;
	}

	public RequiredCapability applyOn() {
		return applyOn;
	}

	public RequiredCapability newValue() {
		return newValue;
	}

	public boolean matches(RequiredCapability toMatch) {
		if (!toMatch.getNamespace().equals(applyOn.getNamespace()))
			return false;
		if (!toMatch.getName().equals(applyOn.getName()))
			return false;
		if (toMatch.getRange().equals(applyOn.getRange()))
			return true;

		//TODO Here, in the long run we want to be smarter .for example we could check that the range of the match is a subset of the range specified on applyOn.
		return false;
	}

	boolean matches(Recommendation toMatch) {
		return matches(toMatch.applyOn());
	}

	protected Recommendation merge(Recommendation r2) {
		VersionRange result = intersect(newValue().getRange(), r2.newValue().getRange());
		if (result == null)
			return null;
		return new Recommendation(applyOn, new RequiredCapability(applyOn.getNamespace(), applyOn.getName(), result));
	}

	private VersionRange intersect(VersionRange r1, VersionRange r2) {
		Version resultMin = null;
		boolean resultMinIncluded = false;
		Version resultMax = null;
		boolean resultMaxIncluded = false;

		int minCompare = r1.getMinimum().compareTo(r2.getMinimum());
		if (minCompare < 0) {
			resultMin = r2.getMinimum();
			resultMinIncluded = r2.getIncludeMinimum();
		} else if (minCompare > 0) {
			resultMin = r1.getMinimum();
			resultMinIncluded = r1.getIncludeMinimum();
		} else if (minCompare == 0) {
			resultMin = r1.getMinimum();
			resultMinIncluded = r1.getIncludeMinimum() && r2.getIncludeMinimum();
		}

		int maxCompare = r1.getMaximum().compareTo(r2.getMaximum());
		if (maxCompare > 0) {
			resultMax = r2.getMaximum();
			resultMaxIncluded = r2.getIncludeMaximum();
		} else if (maxCompare < 0) {
			resultMax = r1.getMaximum();
			resultMaxIncluded = r1.getIncludeMaximum();
		} else if (maxCompare == 0) {
			resultMax = r1.getMaximum();
			resultMaxIncluded = r1.getIncludeMaximum() && r2.getIncludeMaximum();
		}

		int resultRangeComparison = resultMin.compareTo(resultMax);
		if (resultRangeComparison < 0)
			return new VersionRange(resultMin, resultMinIncluded, resultMax, resultMaxIncluded);
		else if (resultRangeComparison == 0 && resultMinIncluded == resultMaxIncluded)
			return new VersionRange(resultMin, resultMinIncluded, resultMax, resultMaxIncluded);
		else
			return null;
	}

	public boolean isCompatible(Recommendation other) {
		return intersect(newValue.getRange(), other.newValue.getRange()) != null;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((applyOn == null) ? 0 : applyOn.hashCode());
		result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Recommendation other = (Recommendation) obj;
		if (applyOn == null) {
			if (other.applyOn != null)
				return false;
		} else if (!applyOn.equals(other.applyOn))
			return false;
		if (newValue == null) {
			if (other.newValue != null)
				return false;
		} else if (!newValue.equals(other.newValue))
			return false;
		return true;
	}
}