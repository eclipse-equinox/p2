package org.eclipse.equinox.p2.tests.jarprocessor;

import java.io.*;
import java.util.Properties;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.jarprocessor.PackStep;
import org.eclipse.equinox.internal.p2.jarprocessor.verifier.Verifier;
import org.eclipse.equinox.internal.p2.jarprocessor.verifier.VerifyStep;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor.Options;

public class JarProcessorTests extends AbstractProvisioningTest {

	public void testVerifyStep() throws Exception {
		if (!VerifyStep.canVerify())
			return;

		// run verify on 
		File workingDir = getTestFolder("testVerifyStep");

		Verifier verifier = new Verifier() {
			@Override
			public void verify(File workingDirectory, String[] input) {
				Properties options = new Properties();
				JarProcessor processor = new JarProcessor();
				processor.setWorkingDirectory(workingDirectory.getAbsolutePath());
				processor.addProcessStep(new VerifyStep(options, false));

				FileFilter filter = new FileFilter() {
					public boolean accept(File pathname) {
						String name = pathname.getName();
						if (pathname.isFile() && name.endsWith(".jar"))
							if ((name.indexOf("source") == -1) && name.startsWith("org.eclipse.equinox.p2"))
								return true;
						return false;
					}

				};
				for (int i = 0; i < input.length; i++) {
					File inputFile = new File(input[i]);
					if (inputFile.exists()) {
						try {
							process(inputFile, filter, true, processor, null);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};

		String install = Platform.getInstallLocation().getURL().getPath();
		File plugins = new File(install, "plugins");
		verifier.verify(workingDir, new String[] {plugins.getAbsolutePath()});
	}

	public void testPackUnpackVerify() throws Exception {
		if (!PackStep.canPack() || !VerifyStep.canVerify())
			return;

		File workingDir = getTestFolder("testPackUnpackVerify");

		File input = new File(workingDir, "in");
		File packed = new File(workingDir, "packed");

		String install = Platform.getInstallLocation().getURL().getPath();
		File plugins = new File(install, "plugins");
		File[] files = plugins.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String name = pathname.getName();
				if (pathname.isFile() && name.endsWith(".jar") && name.indexOf(".source") == -1) {
					if (name.startsWith("org.eclipse.core.c") || name.startsWith("org.eclipse.core.r"))
						return true;
				}
				return false;
			}
		});

		input.mkdirs();
		for (int i = 0; i < files.length; i++) {
			copy("Setup input", files[i], new File(input, files[i].getName()));
		}

		Options options = new Options();
		options.pack = true;
		options.outputDir = packed.getAbsolutePath();
		options.input = input;

		JarProcessorExecutor executor = new JarProcessorExecutor();
		executor.runJarProcessor(options);

		Verifier.main(new String[] {"-dir", packed.getAbsolutePath(), packed.getAbsolutePath()});
	}
}
