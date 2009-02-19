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

import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;

public class PropertyLookupQuery1 extends MatchQuery {

	public String getSomeProperty() {
		return "foo";
	}

	public String getThatProperty(Object param) {
		return "bar";
	}

	public boolean isMatch(Object candidate) {
		return false;
	}
}
