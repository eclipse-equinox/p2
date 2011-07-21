/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.util.*;
import org.eclipse.osgi.util.NLS;

/**
 * Enumeration for all types of installable items which are accepted in a product.
 */
public enum ProductContentType {

	BUNDLES("bundles"), // only bundles are accepted in the product //$NON-NLS-1$ 
	FEATURES("features"), // only features are accepted in the product //$NON-NLS-1$
	MIXED("mixed"); // all kinds of installable units are accepted in the product //$NON-NLS-1$

	private String contentTypeString;
	private static Map<String, ProductContentType> mappings = new HashMap<String, ProductContentType>();

	static {
		mappings.put("bundles", BUNDLES); //$NON-NLS-1$
		mappings.put("features", FEATURES); //$NON-NLS-1$
		mappings.put("mixed", MIXED); //$NON-NLS-1$
	}

	private ProductContentType(String contentTypeString) {
		this.contentTypeString = contentTypeString;
	}

	@Override
	public String toString() {
		return contentTypeString;
	}

	/**
	 * Parses a string to the content type which can be included in the product.
	 * @param typeAsString input string to be parsed
	 * @return the content type which can be included in the product.
	 * @throws IllegalArgumentException if <code>typeAsString</code> is not a valid for a product content type
	 */
	public static ProductContentType toProductContentType(String typeAsString) throws IllegalArgumentException {
		ProductContentType result = mappings.get(typeAsString.toLowerCase(Locale.ENGLISH));
		if (result == null)
			throw new IllegalArgumentException(NLS.bind(Messages.exception_invalidProductContentType, typeAsString, getAllowedSetOfValues()));
		return result;
	}

	/**
	 * @return set of all possible values for a product content type
	 */
	public static Set<String> getAllowedSetOfValues() {
		return mappings.keySet();
	}
}