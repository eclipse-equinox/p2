/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.publisher.actions.IUpdateDescriptorAdvice;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public final class AbstractPublisherActionTest extends AbstractProvisioningTest {
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

		public void testProcessUpdateDescriptorAdvice(InstallableUnitDescription iu, IPublisherInfo publisherInfo) {
			AbstractPublisherAction.processUpdateDescriptorAdvice(iu, publisherInfo);
		}
	}

	static class TestUpdateDescriptorAdvice implements IUpdateDescriptorAdvice {

		private final IUpdateDescriptor updateDescriptor;

		public TestUpdateDescriptorAdvice(IUpdateDescriptor updateDescriptor) {
			this.updateDescriptor = updateDescriptor;
		}

		public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
			return id.equals("test");
		}

		public IUpdateDescriptor getUpdateDescriptor(InstallableUnitDescription iu) {
			return this.updateDescriptor;
		}
	}

	static class TestCapabilityAdvice implements ICapabilityAdvice {
		private final IProvidedCapability providedCapability;
		private final IRequirement requiredCapability;
		private final IRequirement metaRequiredCapability;

		public TestCapabilityAdvice(IProvidedCapability providedCapability, IRequirement requiredCapability, IRequirement metaRequiredCapability) {
			this.providedCapability = providedCapability;
			this.requiredCapability = requiredCapability;
			this.metaRequiredCapability = metaRequiredCapability;
		}

		public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu) {
			if (providedCapability == null)
				return null;

			return new IProvidedCapability[] {providedCapability};
		}

		public IRequirement[] getRequiredCapabilities(InstallableUnitDescription iu) {
			if (requiredCapability == null)
				return null;

			return new IRequirement[] {requiredCapability};
		}

		public IRequirement[] getMetaRequiredCapabilities(InstallableUnitDescription iu) {
			if (metaRequiredCapability == null)
				return null;

			return new IRequirement[] {metaRequiredCapability};
		}

		public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
			return id.equals("test");
		}
	}

	public void testAddUpdateDescriptor() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");

		IPublisherInfo info = new PublisherInfo();
		VersionRange range = new VersionRange("[0.0.0,1.1.1)");
		IUpdateDescriptor testUpdateDescriptor = MetadataFactory.createUpdateDescriptor("name1", range, 10, "Test Description");

		info.addAdvice(new TestUpdateDescriptorAdvice(testUpdateDescriptor));
		TestAction action = new TestAction();
		action.testProcessUpdateDescriptorAdvice(iu, info);

		assertEquals("name1", RequiredCapability.extractName(iu.getUpdateDescriptor().getIUsBeingUpdated().iterator().next()));
		assertEquals(range, RequiredCapability.extractRange(iu.getUpdateDescriptor().getIUsBeingUpdated().iterator().next()));
		assertEquals(10, iu.getUpdateDescriptor().getSeverity());
		assertEquals("Test Description", iu.getUpdateDescriptor().getDescription());
	}

	public void testAddCapabilities() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");
		assertEquals(0, iu.getRequirements().size());
		assertEquals(0, iu.getProvidedCapabilities().size());
		assertEquals(0, iu.getMetaRequirements().size());

		IPublisherInfo info = new PublisherInfo();
		IRequirement testRequiredCapability = MetadataFactory.createRequirement("ns1", "name1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ns2", "name2", Version.createOSGi(9, 0, 0));
		IRequirement testMetaRequiredCapability = MetadataFactory.createRequirement("ns3", "name3", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals("name1", ((IRequiredCapability) iu.getRequirements().iterator().next()).getName());
		assertEquals("name2", iu.getProvidedCapabilities().iterator().next().getName());
		assertEquals("name3", ((IRequiredCapability) iu.getMetaRequirements().iterator().next()).getName());
	}

	public void testAddCapabilitiesIdentityCounts() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");

		IRequirement[] requiredCapabilities = new IRequirement[5];
		requiredCapabilities[0] = MetadataFactory.createRequirement("rtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[1] = MetadataFactory.createRequirement("rtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[2] = MetadataFactory.createRequirement("rtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[3] = MetadataFactory.createRequirement("rtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[4] = MetadataFactory.createRequirement("rtest3", "test3", null, null, false, false, false);
		iu.setRequirements(requiredCapabilities);

		IProvidedCapability[] providedCapabilities = new IProvidedCapability[5];
		providedCapabilities[0] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(1, 0, 0));
		providedCapabilities[1] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(2, 0, 0));
		providedCapabilities[2] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(1, 0, 0));
		providedCapabilities[3] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(2, 0, 0));
		providedCapabilities[4] = MetadataFactory.createProvidedCapability("ptest3", "test3", null);
		iu.setCapabilities(providedCapabilities);

		IRequirement[] metaRequiredCapabilities = new IRequirement[5];
		metaRequiredCapabilities[0] = MetadataFactory.createRequirement("mtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[1] = MetadataFactory.createRequirement("mtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[2] = MetadataFactory.createRequirement("mtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[3] = MetadataFactory.createRequirement("mtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[4] = MetadataFactory.createRequirement("mtest3", "test3", null, null, false, false, false);
		iu.setMetaRequirements(metaRequiredCapabilities);

		assertEquals(5, iu.getRequirements().size());
		assertEquals(5, iu.getProvidedCapabilities().size());
		assertEquals(5, iu.getMetaRequirements().size());

		IPublisherInfo info = new PublisherInfo();
		IRequirement testRequiredCapability = MetadataFactory.createRequirement("ns1", "name1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ns2", "name2", Version.createOSGi(9, 0, 0));
		IRequirement testMetaRequiredCapability = MetadataFactory.createRequirement("ns3", "name3", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals(6, iu.getRequirements().size());
		assertEquals(6, iu.getProvidedCapabilities().size());
		assertEquals(6, iu.getMetaRequirements().size());
	}

	public void testReplaceCapabilities() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");
		iu.setRequirements(createRequiredCapabilities("ns1", "name1", null, ""));
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability("ns2", "name2", null)});
		iu.setMetaRequirements(createRequiredCapabilities("ns3", "name3", null, ""));

		assertNotSame(9, PublisherHelper.toOSGiVersion(iu.getProvidedCapabilities().iterator().next().getVersion()).getMajor());
		assertTrue(iu.getRequirements().iterator().next().isGreedy());
		assertTrue(iu.getMetaRequirements().iterator().next().isGreedy());

		IPublisherInfo info = new PublisherInfo();
		IRequirement testRequiredCapability = MetadataFactory.createRequirement("ns1", "name1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ns2", "name2", Version.createOSGi(9, 0, 0));
		IRequirement testMetaRequiredCapability = MetadataFactory.createRequirement("ns3", "name3", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals(9, PublisherHelper.toOSGiVersion(iu.getProvidedCapabilities().iterator().next().getVersion()).getMajor());
		assertFalse(iu.getRequirements().iterator().next().isGreedy());
		assertFalse(iu.getMetaRequirements().iterator().next().isGreedy());
	}

	public void testReplaceCapabilitiesIdentityCounts() {
		InstallableUnitDescription iu = new InstallableUnitDescription();
		iu.setId("test");

		IRequirement[] requiredCapabilities = new IRequirement[5];
		requiredCapabilities[0] = MetadataFactory.createRequirement("rtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[1] = MetadataFactory.createRequirement("rtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[2] = MetadataFactory.createRequirement("rtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		requiredCapabilities[3] = MetadataFactory.createRequirement("rtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		requiredCapabilities[4] = MetadataFactory.createRequirement("rtest3", "test3", null, null, false, false, false);
		iu.setRequirements(requiredCapabilities);

		IProvidedCapability[] providedCapabilities = new IProvidedCapability[5];
		providedCapabilities[0] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(1, 0, 0));
		providedCapabilities[1] = MetadataFactory.createProvidedCapability("ptest1", "test1", Version.createOSGi(2, 0, 0));
		providedCapabilities[2] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(1, 0, 0));
		providedCapabilities[3] = MetadataFactory.createProvidedCapability("ptest2", "test2", Version.createOSGi(2, 0, 0));
		providedCapabilities[4] = MetadataFactory.createProvidedCapability("ptest3", "test3", null);
		iu.setCapabilities(providedCapabilities);

		IRequirement[] metaRequiredCapabilities = new IRequirement[5];
		metaRequiredCapabilities[0] = MetadataFactory.createRequirement("mtest1", "test1", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[1] = MetadataFactory.createRequirement("mtest1", "test1", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[2] = MetadataFactory.createRequirement("mtest2", "test2", new VersionRange("[1,2)"), null, false, false, false);
		metaRequiredCapabilities[3] = MetadataFactory.createRequirement("mtest2", "test2", new VersionRange("[2,3)"), null, false, false, false);
		metaRequiredCapabilities[4] = MetadataFactory.createRequirement("mtest3", "test3", null, null, false, false, false);
		iu.setMetaRequirements(metaRequiredCapabilities);

		assertEquals(5, iu.getRequirements().size());
		assertEquals(5, iu.getProvidedCapabilities().size());
		assertEquals(5, iu.getMetaRequirements().size());

		IPublisherInfo info = new PublisherInfo();
		IRequirement testRequiredCapability = MetadataFactory.createRequirement("rtest1", "test1", null, null, false, false, false);
		IProvidedCapability testProvideCapability = MetadataFactory.createProvidedCapability("ptest1", "test1", null);
		IRequirement testMetaRequiredCapability = MetadataFactory.createRequirement("mtest1", "test1", null, null, false, false, false);

		info.addAdvice(new TestCapabilityAdvice(testProvideCapability, testRequiredCapability, testMetaRequiredCapability));
		TestAction action = new TestAction();
		action.testProcessCapabilityAdvice(iu, info);

		assertEquals(4, iu.getRequirements().size());
		assertEquals(4, iu.getProvidedCapabilities().size());
		assertEquals(4, iu.getMetaRequirements().size());
	}

}
