package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.provisional.p2.directorywatcher.messages"; //$NON-NLS-1$
	public static String artifact_repo_manager_not_registered;
	public static String error_main_loop;
	public static String error_processing;
	public static String failed_create_artifact_repo;
	public static String failed_create_metadata_repo;
	public static String filename_missing;
	public static String metadata_repo_manager_not_registered;
	public static String null_folder;
	public static String thread_not_started;
	public static String thread_started;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//
	}
}
