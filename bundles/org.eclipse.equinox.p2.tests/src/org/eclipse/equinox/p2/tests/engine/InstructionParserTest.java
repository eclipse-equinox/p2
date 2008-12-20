package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.equinox.internal.p2.engine.InstructionParser;

import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.ActionManager;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.TouchpointType;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

public class InstructionParserTest extends AbstractProvisioningTest {

	public static final TouchpointType TOUCHPOINT_TYPE = MetadataFactory.createTouchpointType("InstructionParserTestTouchpoint", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$

	public static class InstructionParserTestTouchpoint extends Touchpoint {

		public TouchpointType getTouchpointType() {
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
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction()", null), TOUCHPOINT_TYPE);
		assertEquals(1, actions.length);
	}

	public void testGoodActionFullyQualified() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("instructionparsertest.goodAction()", null), null);
		assertEquals(1, actions.length);
	}

	public void testBadActionFullyQualified() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("instructionparsertest.badAction()", null), null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActionFromImport() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction()", "instructionparsertest.goodAction"), null);
		assertEquals(1, actions.length);
	}

	public void testGoodActionFromImportWithVersionRange() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction()", "instructionparsertest.goodAction;version=[1.0,2.0)"), null);
		assertEquals(1, actions.length);
	}

	public void testBadActionFromImport() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("badAction()", "instructionparsertest.badAction"), null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActions() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(); goodAction()", null), TOUCHPOINT_TYPE);
		assertEquals(2, actions.length);
	}

	public void testGoodParameter() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1)", null), TOUCHPOINT_TYPE);
		assertEquals(1, actions.length);
	}

	public void testGoodParameters() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1, b:2)", null), TOUCHPOINT_TYPE);
		assertEquals(1, actions.length);
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
			parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(); badAction()", null), TOUCHPOINT_TYPE);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testNoActionFound() {
		InstructionParser parser = new InstructionParser(new ActionManager());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("notfoundaction()", null), TOUCHPOINT_TYPE);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}
}
