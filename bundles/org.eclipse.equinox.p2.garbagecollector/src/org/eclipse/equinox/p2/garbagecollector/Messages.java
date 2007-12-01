package org.eclipse.equinox.p2.garbagecollector;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.garbagecollector.messages"; //$NON-NLS-1$

	public static String Error_in_extension;
	public static String Missing_bus;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
