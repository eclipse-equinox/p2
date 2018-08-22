/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.expression;

/**
 * This interface may be implemented by any class that wants to provide easy
 * (high performance) access to its member to the expression evaluator. It
 * also gives the implementing class a way to hide or rename the members
 * otherwise accessible using getters.
 * @since 2.0
 */
public interface IMemberProvider {
	/**
	 * Returns the value for the specified member. Implementers can rely
	 * on that the <code>memberName</code> is a string that has been
	 * internalized using {@link String#intern()}.
	 * @param memberName The name of the member
	 * @return The member value.
	 * @throws IllegalArgumentException if the instance has no member with
	 * the given name.
	 */
	Object getMember(String memberName);
}
