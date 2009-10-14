package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public class ORRequirement implements IRequiredCapability {
	private IRequiredCapability[] oredRequirements;

	public ORRequirement(IRequiredCapability[] reqs) {
		oredRequirements = reqs;
	}

	public IRequiredCapability[] getRequirements() {
		return oredRequirements;
	}

	public String getFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getNamespace() {
		// TODO Auto-generated method stub
		return null;
	}

	public VersionRange getRange() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getSelectors() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isGreedy() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean isMultiple() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isOptional() {
		return false;
	}

	public void setFilter(String filter) {
		// TODO Auto-generated method stub

	}

	public void setSelectors(String[] selectors) {
		// TODO Auto-generated method stub

	}

	public boolean isNegation() {
		return false;
	}

	public String toString() {
		String result = "OR(";
		for (int i = 0; i < oredRequirements.length; i++) {
			result += oredRequirements[i].toString();
		}
		return result + ")";
	}

	public boolean satisfiedBy(IProvidedCapability cap) {
		for (int i = 0; i < oredRequirements.length; i++) {
			boolean result = oredRequirements[i].satisfiedBy(cap);
			if (result)
				return true;
		}
		return false;
	}
}
