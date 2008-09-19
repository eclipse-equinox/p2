package org.eclipse.equinox.p2.tests.engine;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.NullTouchpoint;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class NullTouchpointTest extends AbstractProvisioningTest {
	public static Test suite() {
		return new TestSuite(NullTouchpointTest.class);
	}

	public void testGetAction() {
		NullTouchpoint touchpoint = new NullTouchpoint();
		ProvisioningAction action = touchpoint.getAction("");
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
