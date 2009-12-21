/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.ContextQuery;

public class PropertyLookupQuery2 extends ContextQuery {

	public String getSomeOtherProperty() {
		return "bar";
	}

	public Collector perform(Iterator iterator) {
		return null;
	}
}
