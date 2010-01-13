/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.expression;

import java.util.Map;
import org.osgi.framework.Filter;

/**
 * An interface that combines the IExpression with the LDAP filter. The
 * string representation is the LDAP filter syntax.
 * @since 2.0
 */
public interface IFilterExpression extends IExpression, Filter {
	/**
	 * Filter using a <code>Map</code>. This <code>Filter</code> is
	 * executed using the specified <code>Map</code>'s keys and values.
	 * The keys are case insensitively matched with this <code>Filter</code>.
	 * 
	 * @param map The <code>Map</code> whose keys are used in the
	 *        match.
	 * @return <code>true</code> if the <code>map</code>'s keys and
	 *         values match this filter; <code>false</code> otherwise.
	 * @throws IllegalArgumentException If <code>map</code> contains case
	 *         variants of the same key name.
	 */
	boolean match(Map<String, ? extends Object> map);

	/**
	 * Filter with case sensitivity using a <code>Map</code>. This
	 * <code>Filter</code> is executed using the specified
	 * <code>Map</code>'s keys and values. The keys are case sensitively
	 * matched with this <code>Filter</code>.
	 * 
	 * @param map The <code>Map</code> whose keys are used in the
	 *        match.
	 * @return <code>true</code> if the <code>map</code>'s keys and
	 *         values match this filter; <code>false</code> otherwise.
	 */
	boolean matchCase(Map<String, ? extends Object> map);
}
