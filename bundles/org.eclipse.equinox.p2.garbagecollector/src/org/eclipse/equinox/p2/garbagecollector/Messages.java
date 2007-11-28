package org.eclipse.equinox.p2.garbagecollector;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.garbagecollector.messages"; //$NON-NLS-1$

	public static String CoreGarbageCollector_0;
	public static String CoreGarbageCollector_1;
	public static String GarbageCollector_0;
	public static String GarbageCollector_1;
	public static String GarbageCollector_2;
	public static String GarbageCollector_3;
	public static String GarbageCollector_4;
	public static String GCActivator_0;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
