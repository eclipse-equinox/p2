package org.eclipse.equinox.p2.tests.publisher.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class AbstractPublisherActionTest extends AbstractProvisioningTest {
	//Note: this is tests for AbstractPublisherAction and not a base class for other tests

	static class TestAction extends AbstractPublisherAction {
		@Override
		public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
			// TODO Auto-generated method stub
			return null;
		}

		public void testProcessCapabilityAdvice(InstallableUnitDescription iu, IPublisherInfo info) {
			AbstractPublisherAction.processCapabilityAdvice(iu, info);
		}

	}

	static class TestCapabilityAdvice implements ICapabilityAdvice {
		private final IProvidedCapability providedCapability;
		private final IRequiredCapability requiredCapability;

		public TestCapabilityAdvice(IProvidedCapability providedCapability, IRequiredCapability requiredCapability) {
			this.providedCapability = providedCapability;
			this.requiredCapability = requiredCapability;
		}

		public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu) {
			return new IProvidedCapability[] {providedCapability};
		}

		public IRequiredCapability[] getRequiredCapabilities(InstallableUnitDescription iu) {
			return new IRequiredCapability[] {requiredCapability};
		}

		public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
			return id.equals("test");
		}
	}

	public void testOverrideCapability() {

		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");
		iu.setRequiredCapabilities(createRequiredCapabilities("ns1", "name1", null, ""));
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability("ns2", "name2", null)});

		assertNotSame(9, iu.getProvidedCapabilities()[0].getVersion().getMajor());
		assertTrue(iu.getRequiredCapabilities()[0].isGreedy());

		IPublisherInfo info = new PublisherInfo();
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ns2", "name2", new Version(9, 0, 0));
		IRequiredCapability testRequiredCapability = MetadataFactory.createRequiredCapability("ns1", "name1", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals(9, iu.getProvidedCapabilities()[0].getVersion().getMajor());
		assertFalse(iu.getRequiredCapabilities()[0].isGreedy());
	}
}
