/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.ActionManager;
import org.eclipse.equinox.internal.p2.engine.InstructionParser;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class InstructionParserTest extends AbstractProvisioningTest {

	public static final ITouchpointType TOUCHPOINT_TYPE = MetadataFactory.createTouchpointType("InstructionParserTestTouchpoint", Version.create("1.0")); //$NON-NLS-1$ //$NON-NLS-2$

	public static class InstructionParserTestTouchpoint extends Touchpoint {

		public ITouchpointType getTouchpointType() {
			return TOUCHPOINT_TYPE;
		}

		@Override
		public String qualifyAction(String actionId) {
			return "instructionparsertest." + actionId;
		}

	}

	public static class TestAction extends ProvisioningAction {
		@Override
		public IStatus execute(Map<String, Object> parameters) {
			return null;
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			return null;
		}
	}

	public static Test suite() {
		return new TestSuite(InstructionParserTest.class);
	}

	public void testNullIUPhase() {
		assertThrows(RuntimeException.class, () -> new InstructionParser(null));
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
		List<ProvisioningAction> actions = parser.parseActions(
				MetadataFactory.createTouchpointInstruction("instructionparsertest.badAction()", null), null);
		assertThrows(IllegalArgumentException.class, () -> actions.get(0).execute(null));
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
		List<ProvisioningAction> actions = parser.parseActions(
				MetadataFactory.createTouchpointInstruction("badAction()", "instructionparsertest.badAction"), null);
		assertThrows(IllegalArgumentException.class, () -> actions.get(0).execute(null));
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
		assertThrows(IllegalArgumentException.class,
				() -> parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(badParameter)", null),
						TOUCHPOINT_TYPE));
	}

	public void testGoodParamterBadParameter() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		assertThrows(IllegalArgumentException.class,
				() -> parser.parseActions(
						MetadataFactory.createTouchpointInstruction("goodAction(a:1, badParameter)", null),
						TOUCHPOINT_TYPE));
	}

	public void testBadAction() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		assertThrows(IllegalArgumentException.class, () -> parser
				.parseActions(MetadataFactory.createTouchpointInstruction("badAction", null), TOUCHPOINT_TYPE));
	}

	public void testGoodActionBadAction() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser.parseActions(
				MetadataFactory.createTouchpointInstruction("goodAction(); badAction()", null), TOUCHPOINT_TYPE);
		assertThrows(IllegalArgumentException.class, () -> actions.get(1).execute(null));
	}

	public void testNoActionFound() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		List<ProvisioningAction> actions = parser
				.parseActions(MetadataFactory.createTouchpointInstruction("notfoundaction()", null), TOUCHPOINT_TYPE);
		assertThrows(IllegalArgumentException.class, () -> actions.get(0).execute(null));
	}
}
