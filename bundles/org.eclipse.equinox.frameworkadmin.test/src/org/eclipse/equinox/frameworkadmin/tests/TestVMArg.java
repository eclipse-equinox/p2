package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

public class TestVMArg  extends FwkAdminAndSimpleConfiguratorTest {
	private Manipulator m;

	public TestVMArg(String name) {
		super(name);
	}

	protected void setUp() throws  Exception {
		super.setUp();
		m = createMinimalConfiguration(TestEclipseDataArea.class.getName());
	}
	
	public void testVMInsideInstall() throws FrameworkAdminRuntimeException, IOException {
		//Test VM path in the install folder
		File jreLocation = new File(m.getLauncherData().getLauncher().getParentFile(), "jre");
		m.getLauncherData().setJvm(jreLocation);
		m.save(false);
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "jre");
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "file:");
		m.load();
		assertEquals(jreLocation, m.getLauncherData().getJvm());
		
		m.getLauncherData().setJvm(null);
		m.save(false);
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "jre");
	}
	
	public void tesVMOutsideInstall() throws FrameworkAdminRuntimeException, IOException {
		//Test VM path in the install folder
		File jreLocation = new File(m.getLauncherData().getLauncher().getParentFile(), "../../jre").getCanonicalFile();
		m.getLauncherData().setJvm(jreLocation);
		m.save(false);
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "jre");
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "file:");
		m.load();
		assertEquals(jreLocation, m.getLauncherData().getJvm());
	}
}
