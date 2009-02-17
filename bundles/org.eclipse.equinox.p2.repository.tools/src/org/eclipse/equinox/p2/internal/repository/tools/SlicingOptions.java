package org.eclipse.equinox.p2.internal.repository.tools;

import java.util.Dictionary;

public class SlicingOptions {
	private boolean includeOptionalDependencies = true;
	private boolean everythingGreedy = true;
	private boolean forceFilterTo = true;
	private boolean considerStrictDependencyOnly = false;
	private Dictionary filter = null;

	public boolean includeOptionalDependencies() {
		return includeOptionalDependencies;
	}

	public void includeOptionalDependencies(boolean optional) {
		this.includeOptionalDependencies = optional;
	}

	public boolean isEverythingGreedy() {
		return everythingGreedy;
	}

	public void everythingGreedy(boolean greedy) {
		this.everythingGreedy = greedy;
	}

	public boolean forceFilterTo() {
		return forceFilterTo;
	}

	public void forceFilterTo(boolean forcedTo) {
		this.forceFilterTo = forcedTo;
	}

	public boolean considerStrictDependencyOnly() {
		return considerStrictDependencyOnly;
	}

	public void considerStrictDependencyOnly(boolean strict) {
		this.considerStrictDependencyOnly = strict;
	}

	public Dictionary getFilter() {
		return filter;
	}

	public void setFilter(Dictionary filter) {
		this.filter = filter;
	}
}
