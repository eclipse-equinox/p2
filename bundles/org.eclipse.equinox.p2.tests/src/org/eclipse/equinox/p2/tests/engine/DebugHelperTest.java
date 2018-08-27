/*******************************************************************************
 * Copyright (c) 2015, 2017 Mykola Nikishov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
		HashMap<String, Object> parameters = new HashMap<>();
		parameters.put("key", null);

		String result = DebugHelper.formatParameters(parameters);

		Assert.assertEquals("{key=null}", result);
	}

}
