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

		public ProvisioningAction getAction(String actionId) {
			return null;
		}

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
		ProvisioningAction[] actions = parser.parseActions("goodAction()");
		assertEquals(1, actions.length);
	}

	public void testGoodActions() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		ProvisioningAction[] actions = parser.parseActions("goodAction1(); goodAction2()");
		assertEquals(2, actions.length);
	}

	public void testGoodParameter() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		ProvisioningAction[] actions = parser.parseActions("goodAction(a:1)");
		assertEquals(1, actions.length);
	}

	public void testGoodParameters() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		ProvisioningAction[] actions = parser.parseActions("goodAction(a:1, b:2)");
		assertEquals(1, actions.length);
	}

	public void testBadParameter() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions("goodAction(badParameter)");
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodParamterBadParameter() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions("goodAction(a:1, badParameter)");
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testBadAction() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions("badAction");
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testGoodActionBadAction() {
		InstructionParser parser = new InstructionParser(new Collect(1), new NullTouchpoint());
		try {
			parser.parseActions("goodAction(); badAction");
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testNoActionFound() {
		Touchpoint empty = new NullActionTouchpoint();
		InstructionParser parser = new InstructionParser(new Collect(1), empty);
		try {
			parser.parseActions("notfoundaction()");
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testActionManagerActionFound() {
		Touchpoint empty = new NullActionTouchpoint();
		InstructionParser parser = new InstructionParser(new Collect(1), empty);
		ProvisioningAction[] actions = parser.parseActions("org.eclipse.equinox.p2.tests.engine.test(a:1, b:2)");
		assertEquals(1, actions.length);
	}
}
