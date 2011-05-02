/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - converted into expression based query
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.query;

import org.eclipse.equinox.p2.query.ExpressionMatchQuery;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.expression.*;

/**
 * A query that finds all IUs that are considered an "Update" of the 
 * specified IU.  
 */
public final class UpdateQuery extends ExpressionMatchQuery<IInstallableUnit> {
	private static final IExpression expr1;
	private static final IExpression expr2;

	static {
		IExpressionParser parser = ExpressionUtil.getParser();

		// This expression is used in case the updateFrom is an IInstallableUnitPatch
		//
		expr1 = parser.parse("$0 ~= updateDescriptor && ($0.id != id || $0.version < version)"); //$NON-NLS-1$

		// When updateFrom is not an IInstallableUnitPatch, we need to do one of two things depending
		// on if the current item is an InstallableUnitPatch or not.
		//
		expr2 = parser.parse("this ~= class('org.eclipse.equinox.p2.metadata.IInstallableUnitPatch')" + // //$NON-NLS-1$
				"? $0 ~= lifeCycle" + // //$NON-NLS-1$
				": $0 ~= updateDescriptor && ($0.id != id || $0.version < version)"); //$NON-NLS-1$
	}

	public UpdateQuery(IInstallableUnit updateFrom) {
		super(IInstallableUnit.class, updateFrom instanceof IInstallableUnitPatch ? expr1 : expr2, updateFrom, IInstallableUnitPatch.class);
	}
}
