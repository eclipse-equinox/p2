/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.expression;

import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ExpressionTest extends AbstractProvisioningTest {
	private static final IExpressionFactory factory = ExpressionUtil.getFactory();

	protected void testExpression(String exprStr, Object expectedOutcome) throws Exception {
		IExpression expr = ExpressionUtil.parse(exprStr);
		assertEquals(expr.evaluate(factory.createContext()), expectedOutcome);
	}

	protected void testMatch(String expr, boolean expectedOutcome) throws Exception {
		testExpression(expr, Boolean.valueOf(expectedOutcome));
	}

	public void testCompare() throws Exception {
		testMatch("'foo' == 'foo'", true);
		testMatch("'foo' == 'fooo'", false);
		testMatch("'foo' != 'foo'", false);
		testMatch("'foo' != 'fooo'", true);
		testMatch("2 < 1", false);
		testMatch("2 <= 1", false);
		testMatch("2 < 2", false);
		testMatch("2 <= 2", true);
		testMatch("2 < 3", true);
		testMatch("2 <= 3", true);
		testMatch("1 > 2", false);
		testMatch("1 >= 2", false);
		testMatch("2 > 2", false);
		testMatch("2 >= 2", true);
		testMatch("3 > 2", true);
		testMatch("3 >= 2", true);
	}

	public void testAutoCoerce() throws Exception {
		testMatch("'12' == 12", true);
		testMatch("'012' == 12", true);
		testMatch("'2' > '10'", true);
		testMatch("'2' > 10", false);
		testMatch("true == 'true'", true);
		testMatch("true == 'True'", true);
		testMatch("false == 'false'", true);
		testMatch("false == 'False'", true);
	}

	public void testLeftToRigthAssociativity() throws Exception {
		testMatch("2 < 10 == true", true);
		try {
			testMatch("true == 2 < 10", false);
			fail("Auto coercion from boolean to integer succeded");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}
}
