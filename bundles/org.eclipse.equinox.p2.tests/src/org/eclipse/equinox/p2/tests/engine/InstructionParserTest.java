package org.eclipse.equinox.p2.tests.engine;

import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.InstructionParser;
import org.eclipse.equinox.internal.p2.engine.NullTouchpoint;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.engine.Touchpoint;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.TouchpointType;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

public class InstructionParserTest extends AbstractProvisioningTest {

	public static class NullActionTouchpoint extends Touchpoint {
		private static final TouchpointType TOUCHPOINT_TYPE = MetadataFactory.createTouchpointType("NullActionTouchpoint", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$

		public TouchpointType getTouchpointType() {
			return TOUCHPOINT_TYPE;
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
			InstructionParser parser = new InstructionParser(null, new NullTouchpoint());
		} catch (RuntimeException e) {
			return;
		}
		fail();
	}

	public void testNullTouchpoint() {
		try {
			InstructionParser parser = new InstructionParser(new Collect(1), null);
		} catch (RuntimeException e) {
			return;
		}
		fail();
	}

	public void testGoodAction() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction()", null));
		assertEquals(1, actions.length);
	}

	public void testGoodActionFullyQualified() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullActionTouchpoint());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("phasetest.test()", null));
		assertEquals(1, actions.length);
	}

	public void testBadActionFullyQualified() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullActionTouchpoint());
		try {
			ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("bad.phasetest.test()", null));
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActionFromImport() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullActionTouchpoint());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("test()", "phasetest.test"));
		assertEquals(1, actions.length);
	}

	public void testGoodActionFromImportWithVersionRange() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullActionTouchpoint());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("test()", "phasetest.test;version=[1.0,2.0)"));
		assertEquals(1, actions.length);
	}

	public void testBadActionFromImport() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullActionTouchpoint());
		try {
			ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("test()", "bad.phasetest.test"));
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActions() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction1(); goodAction2()", null));
		assertEquals(2, actions.length);
	}

	public void testGoodParameter() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1)", null));
		assertEquals(1, actions.length);
	}

	public void testGoodParameters() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1, b:2)", null));
		assertEquals(1, actions.length);
	}

	public void testBadParameter() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(badParameter)", null));
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodParamterBadParameter() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(a:1, badParameter)", null));
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testBadAction() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("badAction", null));
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActionBadAction() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("goodAction(); badAction", null));
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testNoActionFound() {
		Touchpoint empty = new NullActionTouchpoint();
		InstructionParser parser = new InstructionParser(new Collect(1), empty);
		try {
			parser.parseActions(MetadataFactory.createTouchpointInstruction("notfoundaction()", null));
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testActionManagerActionFound() {
		Touchpoint empty = new NullActionTouchpoint();
		InstructionParser parser = new InstructionParser(new Collect(1), empty);
		ProvisioningAction[] actions = parser.parseActions(MetadataFactory.createTouchpointInstruction("org.eclipse.equinox.p2.tests.engine.test(a:1, b:2)", null));
		assertEquals(1, actions.length);
	}
}
