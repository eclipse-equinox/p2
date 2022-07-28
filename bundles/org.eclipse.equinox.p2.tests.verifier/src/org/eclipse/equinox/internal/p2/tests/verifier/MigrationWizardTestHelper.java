/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *     Ericsson AB - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.tests.verifier;

import java.net.URI;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration.MigrationSupport;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

@SuppressWarnings("restriction")
public class MigrationWizardTestHelper extends MigrationSupport {
	//Variable that keeps track if the wizard has been requested to open
	public boolean wizardOpened = false;

	@Override
	protected void openMigrationWizard(final IProfile inputProfile, final Collection<IInstallableUnit> unitsToMigrate, final URI[] reposToMigrate) {
		wizardOpened = true;
	}

}
