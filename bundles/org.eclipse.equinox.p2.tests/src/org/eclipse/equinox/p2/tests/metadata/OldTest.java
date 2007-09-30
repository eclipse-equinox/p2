/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import com.thoughtworks.xstream.XStream;
import java.io.*;
import java.util.ArrayList;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.resolution.Transformer;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class OldTest {
	public void testDependencyOnCapability() {
		InstallableUnit osgi = new InstallableUnit();
		osgi.setId("org.eclipse.osgi");
		osgi.setVersion(new Version(3, 2, 0, null));

		osgi.setRequiredCapabilities(new RequiredCapability[] {new RequiredCapability("java.runtime", "JRE", null, null, false, false)});

		InstallableUnit jre = new InstallableUnit();
		jre.setId("com.ibm.jre");
		jre.setVersion(new Version(1, 4, 2, "sr2"));
		jre.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("java.runtime", "JRE", new Version(1, 4, 2, "sr2"))});

		ServiceReference sr = TestActivator.context.getServiceReference(PlatformAdmin.class.getName());

		StateObjectFactory factory = ((PlatformAdmin) TestActivator.context.getService(sr)).getFactory();
		Transformer t = new Transformer(factory);
		t.visitInstallableUnit(osgi);
		BundleDescription osgiBd = t.getResult();

		t = new Transformer(factory);
		t.visitInstallableUnit(jre);
		BundleDescription jreBd = t.getResult();

		State state = factory.createState(true);
		state.addBundle(osgiBd);
		state.addBundle(jreBd);
		state.resolve();

		System.out.println(osgiBd + ": " + osgiBd.isResolved());
		System.out.println(jreBd + ": " + jreBd.isResolved());
	}

	public void testNamedDependency() {
		InstallableUnit jface = new InstallableUnit();
		jface.setId("org.eclipse.jface");
		jface.setVersion(new Version(3, 2, 0, null));

		jface.setRequiredCapabilities(new RequiredCapability[] {RequiredCapability.createRequiredCapabilityForName("org.eclipse.swt", null, false)});

		InstallableUnit swt = new InstallableUnit();
		swt.setId("org.eclipse.swt");
		swt.setVersion(new Version(3, 2, 0, null));

		ServiceReference sr = TestActivator.context.getServiceReference(PlatformAdmin.class.getName());

		StateObjectFactory factory = ((PlatformAdmin) TestActivator.context.getService(sr)).getFactory();
		Transformer t = new Transformer(factory);
		t.visitInstallableUnit(jface);
		BundleDescription jfaceBd = t.getResult();

		t = new Transformer(factory);
		t.visitInstallableUnit(swt);
		BundleDescription swtBd = t.getResult();

		State state = factory.createState(true);
		state.addBundle(jfaceBd);
		state.addBundle(swtBd);
		state.resolve();

		System.out.println(jfaceBd + ": " + jfaceBd.isResolved());
		System.out.println(swtBd + ": " + swtBd.isResolved());
	}

	public void testBackup() {
		InstallableUnit osgi = new InstallableUnit();
		osgi.setId("org.eclipse.osgi");
		osgi.setVersion(new Version(3, 2, 0, null));
		osgi.setRequiredCapabilities(new RequiredCapability[] {new RequiredCapability("java.runtime", "JRE", null, null, false, false)});

		InstallableUnit jre = new InstallableUnit();
		jre.setId("com.ibm.jre");
		jre.setVersion(new Version(1, 4, 2, "sr2"));
		jre.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("java.runtime", "JRE", new Version(1, 4, 2, "sr2"))});

		ArrayList all = new ArrayList();
		all.add(osgi);
		try {
			new XStream().toXML(all, new FileOutputStream(new File("d:/tmp/m2.xml")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
