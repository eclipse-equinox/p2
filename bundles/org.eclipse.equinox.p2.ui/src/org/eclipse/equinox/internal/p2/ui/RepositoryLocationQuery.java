/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.equinox.p2.query.ExpressionMatchQuery;

import java.net.URI;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;

/**
 * RepositoryLocationQuery yields true for all URI elements.  
 * 
 * @since 3.5
 */
public class RepositoryLocationQuery extends ExpressionMatchQuery<URI> {

	public RepositoryLocationQuery() {
		super(URI.class, ExpressionUtil.TRUE_EXPRESSION);
	}
}
