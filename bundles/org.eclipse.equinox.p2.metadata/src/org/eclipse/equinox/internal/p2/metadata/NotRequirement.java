package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public class NotRequirement implements IRequiredCapability {
	private IRequiredCapability negatedRequirement;

	public NotRequirement(IRequiredCapability iRequiredCapabilities) {
		negatedRequirement = iRequiredCapabilities;
	}

	public IRequiredCapability getRequirement() {
		return negatedRequirement;
	}

	public String getFilter() {
		return negatedRequirement.getFilter();
	}

	public String getName() {
		return negatedRequirement.getName();
	}

	public String getNamespace() {
		return negatedRequirement.getNamespace();
	}

	public VersionRange getRange() {
		return negatedRequirement.getRange();
	}

	public boolean isGreedy() {
		return negatedRequirement.isGreedy();
	}

	public boolean isMultiple() {
		return negatedRequirement.isMultiple();
	}

	public boolean isOptional() {
		return negatedRequirement.isOptional();
	}

	public void setFilter(String filter) {
		// TODO Auto-generated method stub
	}

	public boolean isNegation() {
		return true;
	}

	public String toString() {
		return "NOT(" + negatedRequirement.toString() + ')'; //$NON-NLS-1$
	}

	public boolean satisfiedBy(IProvidedCapability cap) {
		return !negatedRequirement.satisfiedBy(cap);
	}
}
