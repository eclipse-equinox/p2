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
		IStatus s = ECFTransport.getInstance().download("http://download.eclipse.org/eclipse/updates/3.5-I-builds/plugins/javax.servlet.jsp_2.0.0.v200806031607.jar.pack.gz", fos, new NullProgressMonitor());
		assertOK("2.0", s);
		try {
			fos.close();
			if (f != null) {
				System.out.println(f.length());
				assertTrue("4.0", f.length() < 50000);
			}
		} catch (IOException e) {
			fail("5.0", e);
		}
	}
}
