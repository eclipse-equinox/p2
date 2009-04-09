/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.messages"; //$NON-NLS-1$
	public static String cannot_get_extension;
	public static String cannot_instantiate_step;
	public static String ZipVerifierStep_invalid_archive;
	public static String processing_step_results;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//
	}
}
