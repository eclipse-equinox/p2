/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery.model;

/**
 * Provides means to tag items in the catalog.
 * 
 * @author David Green
 * @author Steffen Pingel
 */
public class Tag extends AbstractCatalogItem {

	private final Object classifier;

	private final String value;

	private final String label;

	public Tag(String value, String label) {
		this(null, value, label);
	}

	public Tag(Object tagClassifier, String value, String label) {
		this.classifier = tagClassifier;
		this.value = value;
		this.label = label;
	}

	/**
	 * the classifier, which places the tag in a logical category
	 * 
	 * @return the classifier or null if this tag is not in any category
	 */
	public Object getTagClassifier() {
		return classifier;
	}

	/**
	 * Returns the value of the tag, not intended for display.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns a short user-visible value that is used by the user to identify the tag.
	 */
	public String getLabel() {
		return label;
	}

	@Override
	public String toString() {
		return "Tag [classifier=" + classifier + ", value=" + value + ", label=" + label + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public int hashCode() {
		// we don't include the label here
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// we don't consider the label when comparing equality
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Tag other = (Tag) obj;
		if (classifier == null) {
			if (other.classifier != null) {
				return false;
			}
		} else if (!classifier.equals(other.classifier)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

}
