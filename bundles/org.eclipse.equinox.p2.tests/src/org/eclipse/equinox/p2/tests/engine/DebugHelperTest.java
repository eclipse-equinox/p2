/*******************************************************************************
 * Copyright (c) 2015, 2015 Mykola Nikishov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mykola Nikishov - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.util.HashMap;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.engine.DebugHelper;
import org.junit.Assert;

public class DebugHelperTest extends TestCase {

	public void testFormatParametersContainsNullValue() {
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("key", null);

		String result = DebugHelper.formatParameters(parameters);

		Assert.assertEquals("{key=null}", result);
	}

}
