/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.identity;

import java.security.BasicPermission;
import java.security.Permission;

public class NamespacePermission extends BasicPermission {
	private static final long serialVersionUID = 3257004371500806969L;

	public static final String ADD_NAMESPACE = "add"; //$NON-NLS-1$

	public static final String ALL_NAMESPACE = "all"; //$NON-NLS-1$

	public static final String CONTAINS_NAMESPACE = "contains"; //$NON-NLS-1$

	public static final String GET_NAMESPACE = "get"; //$NON-NLS-1$

	public static final String REMOVE_NAMESPACE = "remove"; //$NON-NLS-1$

	protected String actions;

	/**
	 * @since 3.9
	 */
	public NamespacePermission() {
		super("", "");
	}

	public NamespacePermission(String s) {
		super(s);
	}

	public NamespacePermission(String s, String s1) {
		super(s, s1);
		actions = s1;
	}

	public String getActions() {
		return actions;
	}

	public boolean implies(Permission p) {
		return false;
	}
}