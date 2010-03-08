/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class AbstractPublisherActionTest extends AbstractProvisioningTest {
	//Note: this is tests for AbstractPublisherAction and not a base class for other tests

	static class TestAction extends AbstractPublisherAction {
		@Override
		public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
			// TODO Auto-generated method stub
			return null;
		}

		public void testProcessCapabilityAdvice(InstallableUnitDescription iu, IPublisherInfo publisherInfo) {
			AbstractPublisherAction.processCapabilityAdvice(iu, publisherInfo);
		}

	}

	static class TestCapabilityAdvice implements ICapabilityAdvice {
		private final IProvidedCapability providedCapability;
		private final IRequiredCapability requiredCapability;
		private final IRequiredCapability metaRequiredCapability;

		public TestCapabilityAdvice(IProvidedCapability providedCapability, IRequiredCapability requiredCapability, IRequiredCapability metaRequiredCapability) {
			this.providedCapability = providedCapability;
			this.requiredCapability = requiredCapability;
			this.metaRequiredCapability = metaRequiredCapability;
		}

		public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu) {
			if (providedCapability == null)
				return null;

			return new IProvidedCapability[] {providedCapability};
		}

		public IRequiredCapability[] getRequiredCapabilities(InstallableUnitDescription iu) {
			if (requiredCapability == null)
				return null;

			return new IRequiredCapability[] {requiredCapability};
		}

		public IRequiredCapability[] getMetaRequiredCapabilities(InstallableUnitDescription iu) {
			if (metaRequiredCapability == null)
				return null;

			return new IRequiredCapability[] {metaRequiredCapability};
		}

		public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
			return id.equals("test");
		}
	}

	public void testAddCapabilities() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");
		assertEquals(0, iu.getRequiredCapabilities().size());
		assertEquals(0, iu.getProvidedCapabilities().size());
		assertEquals(0, iu.getMetaRequiredCapabilities().size());

		IPublisherInfo info = new PublisherInfo();
		IRequiredCapability testRequiredCapability = MetadataFactory.createRequiredCapability("ns1", "name1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ns2", "name2", Version.createOSGi(9, 0, 0));
		IRequiredCapability testMetaRequiredCapability = MetadataFactory.createRequiredCapability("ns3", "name3", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals("name1", ((IRequiredCapability) iu.getRequiredCapabilities().iterator().next()).getName());
		assertEquals("name2", iu.getProvidedCapabilities().iterator().next().getName());
		assertEquals("name3", ((IRequiredCapability) iu.getMetaRequiredCapabilities().iterator().next()).getName());
	}

	public void testAddCapabilitiesIdentityCounts() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");

		IRequiredCapability[] requiredCapabilities = new IRequiredCapability[5];
		requiredCapabilities[0] = MetadataFactory.createRequiredCapability("rtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[1] = MetadataFactory.createRequiredCapability("rtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[2] = MetadataFactory.createRequiredCapability("rtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[3] = MetadataFactory.createRequiredCapability("rtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[4] = MetadataFactory.createRequiredCapability("rtest3", "test3", null, null, false, false, false);
		iu.setRequiredCapabilities(requiredCapabilities);

		IProvidedCapability[] providedCapabilities = new IProvidedCapability[5];
		providedCapabilities[0] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(1, 0, 0));
		providedCapabilities[1] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(2, 0, 0));
		providedCapabilities[2] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(1, 0, 0));
		providedCapabilities[3] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(2, 0, 0));
		providedCapabilities[4] = MetadataFactory.createProvidedCapability("ptest3", "test3", null);
		iu.setCapabilities(providedCapabilities);

		IRequiredCapability[] metaRequiredCapabilities = new IRequiredCapability[5];
		metaRequiredCapabilities[0] = MetadataFactory.createRequiredCapability("mtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[1] = MetadataFactory.createRequiredCapability("mtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[2] = MetadataFactory.createRequiredCapability("mtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[3] = MetadataFactory.createRequiredCapability("mtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[4] = MetadataFactory.createRequiredCapability("mtest3", "test3", null, null, false, false, false);
		iu.setMetaRequiredCapabilities(metaRequiredCapabilities);

		assertEquals(5, iu.getRequiredCapabilities().size());
		assertEquals(5, iu.getProvidedCapabilities().size());
		assertEquals(5, iu.getMetaRequiredCapabilities().size());

		IPublisherInfo info = new PublisherInfo();
		IRequiredCapability testRequiredCapability = MetadataFactory.createRequiredCapability("ns1", "name1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ns2", "name2", Version.createOSGi(9, 0, 0));
		IRequiredCapability testMetaRequiredCapability = MetadataFactory.createRequiredCapability("ns3", "name3", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals(6, iu.getRequiredCapabilities().size());
		assertEquals(6, iu.getProvidedCapabilities().size());
		assertEquals(6, iu.getMetaRequiredCapabilities().size());
	}

	public void testReplaceCapabilities() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");
		iu.setRequiredCapabilities(createRequiredCapabilities("ns1", "name1", null, ""));
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability("ns2", "name2", null)});
		iu.setMetaRequiredCapabilities(createRequiredCapabilities("ns3", "name3", null, ""));

		assertNotSame(9, PublisherHelper.toOSGiVersion(iu.getProvidedCapabilities().iterator().next().getVersion()).getMajor());
		assertTrue(iu.getRequiredCapabilities().iterator().next().isGreedy());
		assertTrue(iu.getMetaRequiredCapabilities().iterator().next().isGreedy());

		IPublisherInfo info = new PublisherInfo();
		IRequiredCapability testRequiredCapability = MetadataFactory.createRequiredCapability("ns1", "name1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ns2", "name2", Version.createOSGi(9, 0, 0));
		IRequiredCapability testMetaRequiredCapability = MetadataFactory.createRequiredCapability("ns3", "name3", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals(9, PublisherHelper.toOSGiVersion(iu.getProvidedCapabilities().iterator().next().getVersion()).getMajor());
		assertFalse(iu.getRequiredCapabilities().iterator().next().isGreedy());
		assertFalse(iu.getMetaRequiredCapabilities().iterator().next().isGreedy());
	}

	public void testReplaceCapabilitiesIdentityCounts() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");

		IRequiredCapability[] requiredCapabilities = new IRequiredCapability[5];
		requiredCapabilities[0] = MetadataFactory.createRequiredCapability("rtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[1] = MetadataFactory.createRequiredCapability("rtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[2] = MetadataFactory.createRequiredCapability("rtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[3] = MetadataFactory.createRequiredCapability("rtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[4] = MetadataFactory.createRequiredCapability("rtest3", "test3", null, null, false, false, false);
		iu.setRequiredCapabilities(requiredCapabilities);

		IProvidedCapability[] providedCapabilities = new IProvidedCapability[5];
		providedCapabilities[0] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(1, 0, 0));
		providedCapabilities[1] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(2, 0, 0));
		providedCapabilities[2] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(1, 0, 0));
		providedCapabilities[3] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(2, 0, 0));
		providedCapabilities[4] = MetadataFactory.createProvidedCapability("ptest3", "test3", null);
		iu.setCapabilities(providedCapabilities);

		IRequiredCapability[] metaRequiredCapabilities = new IRequiredCapability[5];
		metaRequiredCapabilities[0] = MetadataFactory.createRequiredCapability("mtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[1] = MetadataFactory.createRequiredCapability("mtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[2] = MetadataFactory.createRequiredCapability("mtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[3] = MetadataFactory.createRequiredCapability("mtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[4] = MetadataFactory.createRequiredCapability("mtest3", "test3", null, null, false, false, false);
		iu.setMetaRequiredCapabilities(metaRequiredCapabilities);

		assertEquals(5, iu.getRequiredCapabilities().size());
		assertEquals(5, iu.getProvidedCapabilities().size());
		assertEquals(5, iu.getMetaRequiredCapabilities().size());

		IPublisherInfo info = new PublisherInfo();
		IRequiredCapability testRequiredCapability = MetadataFactory.createRequiredCapability("rtest1", "test1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ptest1", "test1", null);
		IRequiredCapability testMetaRequiredCapability = MetadataFactory.createRequiredCapability("mtest1", "test1", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals(4, iu.getRequiredCapabilities().size());
		assertEquals(4, iu.getProvidedCapabilities().size());
		assertEquals(4, iu.getMetaRequiredCapabilities().size());
	}

}
