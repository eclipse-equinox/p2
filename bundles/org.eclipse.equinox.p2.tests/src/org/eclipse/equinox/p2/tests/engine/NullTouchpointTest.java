package org.eclipse.equinox.p2.tests.engine;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.ActionManager;
import org.eclipse.equinox.internal.p2.engine.NullTouchpoint;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class NullTouchpointTest extends AbstractProvisioningTest {
	public static Test suite() {
		return new TestSuite(NullTouchpointTest.class);
	}

	public void testQualifyAction() {
		NullTouchpoint touchpoint = new NullTouchpoint();
		String actionId = touchpoint.qualifyAction("");
		ProvisioningAction action = ActionManager.getInstance().getAction(actionId);
		assertEquals(IStatus.OK, action.execute(null).getSeverity());
		assertEquals(IStatus.OK, action.undo(null).getSeverity());
	}

	public void testSupports() {
		NullTouchpoint touchpoint = new NullTouchpoint();
		assertTrue(touchpoint.supports("install"));
		assertTrue(touchpoint.supports("uninstall"));
		assertFalse(touchpoint.supports("configure"));
		assertFalse(touchpoint.supports("unconfigure"));
		assertFalse(touchpoint.supports("sizing"));
	}

}
