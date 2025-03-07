/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse.bundledescription;

import java.util.Collections;
import java.util.Map;
import org.eclipse.equinox.internal.p2.publisher.eclipse.bundledescription.BaseDescriptionImpl.BaseCapability;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.*;

abstract class VersionConstraintImpl implements VersionConstraint {

	protected final Object monitor = new Object();

	private String name;
	private VersionRange versionRange;
	private BundleDescription bundle;
	private BaseDescription supplier;
	private volatile Object userObject;

	@Override
	public String getName() {
		synchronized (this.monitor) {
			if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(name)) {
				return BundleDescriptionBuilder.NAME;
			}
			return name;
		}
	}

	@Override
	public VersionRange getVersionRange() {
		synchronized (this.monitor) {
			if (versionRange == null) {
				return VersionRange.emptyRange;
			}
			return versionRange;
		}
	}

	@Override
	public BundleDescription getBundle() {
		synchronized (this.monitor) {
			return bundle;
		}
	}

	@Override
	public boolean isResolved() {
		synchronized (this.monitor) {
			return supplier != null;
		}
	}

	@Override
	public BaseDescription getSupplier() {
		synchronized (this.monitor) {
			return supplier;
		}
	}

	@Override
	public boolean isSatisfiedBy(BaseDescription candidate) {
		synchronized (this.monitor) {
			return false;
		}
	}

	protected void setName(String name) {
		synchronized (this.monitor) {
			this.name = name;
		}
	}

	protected void setVersionRange(VersionRange versionRange) {
		synchronized (this.monitor) {
			this.versionRange = versionRange;
		}
	}

	protected void setBundle(BundleDescription bundle) {
		synchronized (this.monitor) {
			this.bundle = bundle;
		}
	}

	protected void setSupplier(BaseDescription supplier) {
		synchronized (this.monitor) {
			this.supplier = supplier;
		}
	}

	protected abstract String getInternalNameSpace();

	protected abstract Map<String, String> getInternalDirectives();

	protected abstract Map<String, Object> getInteralAttributes();

	protected abstract boolean hasMandatoryAttributes(String[] mandatory);

	@Override
	public BundleRequirement getRequirement() {
		String namespace = getInternalNameSpace();
		if (namespace == null) {
			return null;
		}
		return new BundleRequirementImpl(namespace);
	}

	@Override
	public Object getUserObject() {
		return userObject;
	}

	@Override
	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	class BundleRequirementImpl implements BundleRequirement {
		private final String namespace;

		public BundleRequirementImpl(String namespace) {
			this.namespace = namespace;
		}

		@Override
		public String getNamespace() {
			return namespace;
		}

		@Override
		public Map<String, String> getDirectives() {
			return Collections.unmodifiableMap(getInternalDirectives());
		}

		@Override
		public Map<String, Object> getAttributes() {
			return Collections.unmodifiableMap(getInteralAttributes());
		}

		@Override
		public BundleRevision getRevision() {
			return getBundle();
		}

		@Override
		public boolean matches(BundleCapability capability) { // NO_UCD
			return isSatisfiedBy(((BaseCapability) capability).getBaseDescription());
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(VersionConstraintImpl.this);
		}

		private VersionConstraintImpl getVersionConstraint() {
			return VersionConstraintImpl.this;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof BundleRequirementImpl)) {
				return false;
			}
			return ((BundleRequirementImpl) obj).getVersionConstraint() == VersionConstraintImpl.this;
		}

		@Override
		public String toString() {
			return getNamespace() + BaseDescriptionImpl.toString(getAttributes(), false) + BaseDescriptionImpl.toString(getDirectives(), true);
		}

		@Override
		public BundleRevision getResource() {
			return getRevision();
		}
	}

	static StringBuilder addFilterAttributes(StringBuilder filter, Map<String, ?> attributes) {
		for (Map.Entry<String, ?> entry : attributes.entrySet()) {
			addFilterAttribute(filter, entry.getKey(), entry.getValue());
		}
		return filter;
	}

	static StringBuilder addFilterAttribute(StringBuilder filter, String attr, Object value) {
		return addFilterAttribute(filter, attr, value, true);
	}

	static StringBuilder addFilterAttribute(StringBuilder filter, String attr, Object value, boolean escapeWildCard) {
		if (value instanceof VersionRange) {
			VersionRange range = (VersionRange) value;
			filter.append(range.toFilterString(attr));
		} else {
			filter.append('(').append(attr).append('=').append(escapeValue(value, escapeWildCard)).append(')');
		}
		return filter;
	}

	private static String escapeValue(Object o, boolean escapeWildCard) {
		String value = o.toString();
		boolean escaped = false;
		int inlen = value.length();
		int outlen = inlen << 1; /* inlen * 2 */

		char[] output = new char[outlen];
		value.getChars(0, inlen, output, inlen);

		int cursor = 0;
		for (int i = inlen; i < outlen; i++) {
			char c = output[i];
			switch (c) {
				case '*' :
					if (!escapeWildCard) {
						break;
					}
				case '\\' :
				case '(' :
				case ')' :
					output[cursor] = '\\';
					cursor++;
					escaped = true;
					break;
			}

			output[cursor] = c;
			cursor++;
		}

		return escaped ? new String(output, 0, cursor) : value;
	}
}
