package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.osgi.framework.Version;

/**
 * Publishing advice from a p2 advice file. An advice file (p2.inf) can be embedded
 * in the source of a bundle, feature, or product to specify additional advice to be
 * added to the {@link IInstallableUnit} corresponding to the bundle, feature, or product.
 */
public class AdviceFileAdvice implements ITouchpointAdvice {
	/**
	 * The location of the bundle advice file, relative to the bundle root location.
	 */
	public static final IPath BUNDLE_ADVICE_FILE = new Path("META-INF/p2.inf"); //$NON-NLS-1$

	private static final String ADVICE_INSTRUCTIONS_PREFIX = "instructions."; //$NON-NLS-1$
	private IPath basePath;
	private final IPath adviceFilePath;

	/**
	 * Creates advice for an advice file at the given location. If <tt>basePath</tt>
	 * is a directory, then <tt>adviceFilePath</tt> is appended to this location to
	 * obtain the location of the advice file. If <tt>basePath</tt> is a file, then
	 * <tt>adviceFilePath</tt> is used to 
	 * @param basePath
	 * @param adviceFilePath
	 */
	public AdviceFileAdvice(IPath basePath, IPath adviceFilePath) {
		this.basePath = basePath;
		this.adviceFilePath = adviceFilePath;
	}

	/**
	 * Loads the advice file and returns it in map form.
	 */
	private Map getInstructions() {
		File location = basePath.toFile();
		if (location == null || !location.exists())
			return Collections.EMPTY_MAP;

		ZipFile jar = null;
		InputStream stream = null;
		try {
			if (location.isDirectory()) {
				File adviceFile = new File(location, adviceFilePath.toString());
				try {
					stream = new BufferedInputStream(new FileInputStream(adviceFile));
				} catch (IOException e) {
					return Collections.EMPTY_MAP;
				}
			} else if (location.isFile()) {
				try {
					jar = new ZipFile(location);
					ZipEntry entry = jar.getEntry(adviceFilePath.toString());
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
		// when to turn this on?  for now only when the p2.inf exists on disk.  It may be too expensive to grab 
		// it out of jars when looking at an old update site.
		File location = basePath.toFile();
		if (location == null || !location.isDirectory())
			return false;

		return new File(location, adviceFilePath.toString()).exists();
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.publisher.eclipse.ITouchpointAdvice#getTouchpointData()
	 */
	public TouchpointData getTouchpointData(TouchpointData existing) {
		Map touchpointData = new HashMap(existing.getInstructions());
		Map bundleAdvice = getInstructions();
		for (Iterator iterator = bundleAdvice.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			if (key.startsWith(ADVICE_INSTRUCTIONS_PREFIX)) {
				String phase = key.substring(ADVICE_INSTRUCTIONS_PREFIX.length());
				String instruction = touchpointData.containsKey(phase) ? ((TouchpointInstruction) touchpointData.get(phase)).getBody() : ""; //$NON-NLS-1$
				if (instruction.length() > 0 && !instruction.endsWith(";")) //$NON-NLS-1$
					instruction += ";"; //$NON-NLS-1$
				instruction += ((String) bundleAdvice.get(key)).trim();
				touchpointData.put(phase, instruction);
			}
		}
		return MetadataFactory.createTouchpointData(touchpointData);
	}

}
