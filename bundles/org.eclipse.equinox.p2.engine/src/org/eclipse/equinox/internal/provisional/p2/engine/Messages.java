/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine;

import org.eclipse.osgi.util.NLS;

//TODO Shouldn't have messages class in API package
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.provisional.p2.engine.messages"; //$NON-NLS-1$
	public static String CertificateChecker_CertificateError;
	public static String CertificateChecker_CertificateRejected;
	public static String CertificateChecker_KeystoreConnectionError;
	public static String CertificateChecker_SignedContentIOError;
	public static String CertificateChecker_SignedContentError;
	public static String not_current_phase;
	public static String null_operands;
	public static String null_phase;
	public static String null_phases;
	public static String null_phaseset;
	public static String null_profile;
	public static String phase_error;
	public static String phase_not_started;
	public static String phase_started;
	public static String phaseid_not_positive;
	public static String phaseid_not_set;
	public static String Engine_Operation_Canceled_By_User;
	public static String InstallableUnitEvent_type_not_install_or_uninstall;
	public static String touchpoint_prepare_error;
	public static String touchpoint_commit_error;
	public static String touchpoint_rollback_error;
	public static String phase_undo_error;
	public static String phase_undo_operand_error;
	public static String session_prepare_error;
	public static String session_commit_error;
	public static String null_operand;
	public static String operand_started;
	public static String operand_not_started;
	public static String not_current_operand;
	public static String null_action;
	public static String session_context;
	public static String action_not_found;
	public static String actions_not_found;
	public static String preparing;
	public static String committing;
	public static String rollingback_cancel;
	public static String rollingback_error;
	public static String action_undo_error;
	public static String forced_action_execute_error;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//
	}
}
