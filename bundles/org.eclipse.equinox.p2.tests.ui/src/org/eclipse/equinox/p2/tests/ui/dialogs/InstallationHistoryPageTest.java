/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import java.lang.reflect.Field;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.sdk.RevertProfilePageWithCompare;
import org.eclipse.equinox.internal.p2.ui.viewers.ProvElementContentProvider;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.equinox.p2.ui.RevertProfilePage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Tests for the Installation History page.
 * If nothing else, this test ensures that installation history can be hosted
 * somewhere besides the about dialog
 */
public class InstallationHistoryPageTest extends AbstractProvisioningUITest {
	volatile int jobs;
	volatile int done;

	class TestDialog extends Dialog {
		RevertProfilePage page;

		TestDialog() {
			super(ProvUI.getDefaultParentShell());
		}

		protected Control createDialogArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			page = new RevertProfilePage();
			page.createControl(composite);
			return composite;
		}
	}

	class TestDialog2 extends Dialog {
		RevertProfilePageWithCompare page;

		TestDialog2() {
			super(ProvUI.getDefaultParentShell());
		}

		protected Control createDialogArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			page = new RevertProfilePageWithCompare();
			page.createControl(composite);
			return composite;
		}
	}

	/**
	 * Hammers the background fetch
	 */
	public void disabledtestDialogBackgroundFetch() {
		TestDialog dialog = new TestDialog();
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			Field viewerField = RevertProfilePage.class.getDeclaredField("configsViewer");
			viewerField.setAccessible(true);
			AbstractTableViewer viewer = (AbstractTableViewer) viewerField.get(dialog.page);
			ProvElementContentProvider provider = (ProvElementContentProvider) viewer.getContentProvider();
			Object input = viewer.getInput();
			Field jobField = ProvElementContentProvider.class.getDeclaredField("fetchJob");
			jobField.setAccessible(true);
			Field jobFamily = ProvElementContentProvider.class.getDeclaredField("fetchFamily");
			jobFamily.setAccessible(true);

			jobs = 0;
			done = 0;
			final Object family = jobFamily.get(provider);

			Job.getJobManager().addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent e) {
					if (e.getJob().belongsTo(family)) {
						done++;
						int status = e.getResult().getSeverity();
						if (status != IStatus.CANCEL && status != IStatus.OK) {
							// something unexpected happened.
							fail("Fetch job failed unexpectedly " + e.getResult().getMessage());
						}
					}
				}
			});

			// hammer the elements repetitively to start multiple fast running fetch jobs
			for (int i = 0; i < 5; i++) {
				provider.getElements(input);
				Job job = (Job) jobField.get(provider);
				if (job != null) {
					jobs++;
				}
			}
			// We need to wait for all the fetch jobs to finish and then verify that they did
			Job.getJobManager().join(family, null);
			assertTrue("Something's wrong.  No fetch occurred", jobs > 0);
			assertEquals("Not all jobs finished as expected", jobs, done);
		} catch (Exception e) {
			fail("Failure during reflection", e);
		} finally {
			dialog.close();
		}
	}

	/**
	 * Tests the dialog - just launches it for now
	 */
	public void testDialog() {
		TestDialog dialog = new TestDialog();
		dialog.setBlockOnOpen(false);
		dialog.open();
		dialog.close();
	}

	/**
	 * Tests the dialog - just launches it for now
	 */
	public void testDialogWithCompare() {
		TestDialog2 dialog = new TestDialog2();
		dialog.setBlockOnOpen(false);
		dialog.open();

		dialog.close();
	}
}
