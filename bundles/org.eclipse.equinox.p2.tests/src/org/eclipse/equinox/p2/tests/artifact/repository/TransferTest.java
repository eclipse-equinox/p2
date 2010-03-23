package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.ECFTransport;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.BundleException;

public class TransferTest extends AbstractProvisioningTest {

	public void testGZFileAreNotUnzipped() {
		FileOutputStream fos = null;
		File f = null;
		try {
			f = File.createTempFile("TransferTest", "pack.gz");
			fos = new FileOutputStream(f);
			System.out.println(f);
			Platform.getBundle("org.eclipse.ecf.provider.filetransfer").start();
		} catch (IOException e) {
			fail("1.0", e);
		} catch (BundleException e) {
			fail("1.5", e);
		}
		IStatus s = ECFTransport.getInstance().download("http://download.eclipse.org/eclipse/updates/3.4.x/plugins/javax.servlet_2.5.0.v200806031605.jar.pack.gz", fos, new NullProgressMonitor());
		assertOK("2.0", s);
		try {
			fos.close();
			if (f != null) {
				System.out.println(f.length());
				assertTrue("4.0", f.length() < 75000);
			}
		} catch (IOException e) {
			fail("5.0", e);
		}
	}
}
