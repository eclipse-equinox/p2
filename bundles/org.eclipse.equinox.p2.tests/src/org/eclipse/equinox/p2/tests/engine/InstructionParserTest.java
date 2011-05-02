/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.util.List;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.ActionManager;
import org.eclipse.equinox.internal.p2.engine.InstructionParser;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class InstructionParserTest extends AbstractProvisioningTest {

	public static final ITouchpointType TOUCHPOINT_TYPE = MetadataFactory.createTouchpointType("InstructionParserTestTouchpoint", Version.create("1.0")); //$NON-NLS-1$ //$NON-NLS-2$

	public static class InstructionParserTestTouchpoint extends Touchpoint {

		public ITouchpointType getTouchpointType() {
			return TOUCHPOINT_TYPE;
		}

		public String qualifyAction(String actionId) {
			return "instructionparsertest." + actionId;
		}

	}

	public static class TestAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			return null;
		}

		public IStatus undo(Map parameters) {
			return null;
		}
	}

	public static Test suite() {
		return new TestSuite(InstructionParserTest.class);
	}

	public void testNullIUPhase() {
		try {
			new InstructionParser(null);
		} catch (RuntimeException e) {
			return;
		}
		fail();
	}

	public void testGoodAction() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction()", null), TOUCHPOINT_TYPE);
		assertEquals(1, actions.size());
	}

	public void testGoodActionFullyQualified() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("instructionparsertest.goodAction()", null), null);
		assertEquals(1, actions.size());
	}

	public void testBadActionFullyQualified() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("instructionparsertest.badAction()", null), null);
			actions.get(0).execute(null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActionFromImport() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction()", "instructionparsertest.goodAction"), null);
		assertEquals(1, actions.size());
	}

	public void testGoodActionFromImportWithVersionRange() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction()", "instructionparsertest.goodAction;version=[1.0,2.0)"), null);
		assertEquals(1, actions.size());
	}

	public void testBadActionFromImport() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("badAction()", "instructionparsertest.badAction"), null);
			actions.get(0).execute(null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActions() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(); goodAction()", null), TOUCHPOINT_TYPE);
		assertEquals(2, actions.size());
	}

	public void testGoodParameter() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1)", null), TOUCHPOINT_TYPE);
		assertEquals(1, actions.size());
	}

	public void testGoodParameters() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1, b:2)", null), TOUCHPOINT_TYPE);
		assertEquals(1, actions.size());
	}

	public void testBadParameter() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(badParameter)", null), TOUCHPOINT_TYPE);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodParamterBadParameter() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1, badParameter)", null), TOUCHPOINT_TYPE);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testBadAction() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("badAction", null), TOUCHPOINT_TYPE);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActionBadAction() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(); badAction()", null), TOUCHPOINT_TYPE);
			actions.get(1).execute(null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testNoActionFound() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			List<ProvisioningAction> actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("notfoundaction()", null), TOUCHPOINT_TYPE);
			actions.get(0).execute(null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}
}
