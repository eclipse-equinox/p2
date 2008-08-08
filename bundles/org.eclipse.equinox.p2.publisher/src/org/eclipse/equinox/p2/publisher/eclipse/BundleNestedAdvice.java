package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.osgi.framework.Version;

/**
 * Publishing advice for bundles.  The advice is read from a file embedded or nested
 * in the bundle itself in a file called META-INF/p2.inf.
 */
public class BundleNestedAdvice implements IBundleAdvice {
	private static final String BUNDLE_ADVICE_FILE = "META-INF/p2.inf"; //$NON-NLS-1$

	public Properties getArtifactProperties(File location) {
		return null;
	}

	public Properties getIUProperties(File location) {
		return null;
	}

	// FIXME 1.0 merge.   not really sure what to do with this method.  Got it from the Generator.  There is
	// an advice file that needs to be read.  This should likely go in another advice object.
	// also, what kinds of advice can be in the file?
	public Map getInstructions(File location) {
		if (location == null || !location.exists())
			return Collections.EMPTY_MAP;

		ZipFile jar = null;
		InputStream stream = null;
		try {
			if (location.isDirectory()) {
				File adviceFile = new File(location, BUNDLE_ADVICE_FILE);
				try {
					stream = new BufferedInputStream(new FileInputStream(adviceFile));
				} catch (IOException e) {
					return Collections.EMPTY_MAP;
				}
			} else if (location.isFile()) {
				try {
					jar = new ZipFile(location);
					ZipEntry entry = jar.getEntry(BUNDLE_ADVICE_FILE);
					if (entry == null)
						return Collections.EMPTY_MAP;
					stream = new BufferedInputStream(jar.getInputStream(entry));
				} catch (IOException e) {
					return Collections.EMPTY_MAP;
				}
			}

			Properties advice = null;
			try {
				advice = new Properties();
				advice.load(stream);
			} catch (IOException e) {
				return Collections.EMPTY_MAP;
			}
			return advice != null ? advice : Collections.EMPTY_MAP;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					// boo
				}
			if (jar != null)
				try {
					jar.close();
				} catch (IOException e) {
					// boo
				}
		}
	}

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return false;
	}

}
