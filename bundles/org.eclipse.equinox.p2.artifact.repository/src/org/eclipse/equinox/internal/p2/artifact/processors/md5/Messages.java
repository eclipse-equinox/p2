package org.eclipse.equinox.internal.p2.artifact.processors.md5;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.artifact.processors.md5.messages"; //$NON-NLS-1$
	public static String Error_invalid_hash;
	public static String Error_MD5_unavailable;
	public static String Error_unexpected_hash;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
