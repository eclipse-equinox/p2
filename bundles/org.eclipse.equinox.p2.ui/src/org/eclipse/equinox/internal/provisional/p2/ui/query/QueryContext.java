/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;

/**
 * A class that can be used to provide additional context for
 * selecting the appropriate query to be used for a particular 
 * UI element.  Typically an {@link IQueryProvider} uses this
 * object to determine any specific filtering or grouping that
 * should occur for a query.  Views can associate this context
 * with a model element to affect the traversal of the model.
 * 
 * @since 3.4
 *
 */
public abstract class QueryContext {

	public abstract int getQueryType();

}
