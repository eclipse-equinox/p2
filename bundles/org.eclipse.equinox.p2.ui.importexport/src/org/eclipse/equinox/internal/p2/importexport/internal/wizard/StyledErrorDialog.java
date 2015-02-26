/*******************************************************************************
 * Copyright (c) 2015 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.internal.Constants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class StyledErrorDialog extends MessageDialog {

	public static boolean openQuestion(Shell parent, String title, String message) {
		return open(QUESTION, parent, title, message, SWT.NONE);
	}

	public static void openInformation(Shell parent, String title, String message) {
		open(INFORMATION, parent, title, message, SWT.NONE);
	}

	public static void openWarning(Shell parent, String title, String message) {
		open(WARNING, parent, title, message, SWT.NONE);
	}

	public static boolean open(int kind, Shell parent, String title, String message, int style) {
		StyledErrorDialog dialog = new StyledErrorDialog(parent, title, null, message, kind, getButtonLabels(kind), 0);
		int style2 = style & SWT.SHEET;
		dialog.setShellStyle(dialog.getShellStyle() | style2);
		return dialog.open() == 0;
	}

	/**
	 * @param kind
	 */
	static String[] getButtonLabels(int kind) {
		String[] dialogButtonLabels;
		switch (kind) {
			case ERROR :
			case INFORMATION :
			case WARNING : {
				dialogButtonLabels = new String[] {IDialogConstants.OK_LABEL};
				break;
			}
			case CONFIRM : {
				dialogButtonLabels = new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL};
				break;
			}
			case QUESTION : {
				dialogButtonLabels = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
				break;
			}
			case QUESTION_WITH_CANCEL : {
				dialogButtonLabels = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
				break;
			}
			default : {
				throw new IllegalArgumentException("Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
			}
		}
		return dialogButtonLabels;
	}

	public StyledErrorDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int imageType, String[] buttonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, imageType, buttonLabels, defaultIndex);
	}

	@Override
	protected Control createMessageArea(Composite composite) {
		// create composite
		// create image
		Image image = getImage();
		if (image != null) {
			imageLabel = new Label(composite, SWT.NULL);
			image.setBackground(imageLabel.getBackground());
			imageLabel.setImage(image);
			addAccessibleListeners(imageLabel, image);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);
		}
		// create message
		if (message != null) {
			FormToolkit toolkit = new FormToolkit(Display.getDefault());
			Composite toolkitComp = toolkit.createComposite(composite);
			toolkitComp.setLayout(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
			FormText text = toolkit.createFormText(toolkitComp, false);
			text.setText(message, true, true);
			text.setBackground(composite.getBackground());
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).hint(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT).applyTo(toolkitComp);
			text.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent event) {
					try {
						URI uri = URI.create((String) event.data);
						if ("pref".equals(uri.getScheme())) { //$NON-NLS-1$
							Map<String, String> para = new HashMap<String, String>();
							para.put(IWorkbenchCommandConstants.WINDOW_PREFERENCES_PARM_PAGEID, uri.getAuthority());
							Command prefCommand = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(ICommandService.class).getCommand(IWorkbenchCommandConstants.WINDOW_PREFERENCES);
							prefCommand.executeWithChecks(new ExecutionEvent(prefCommand, para, null, null));
						}
					} catch (ExecutionException e) {
						Platform.getLog(Platform.getBundle(Constants.Bundle_ID)).log(new Status(IStatus.ERROR, Constants.Bundle_ID, e.getMessage(), e));
					} catch (NotDefinedException e) {
						Platform.getLog(Platform.getBundle(Constants.Bundle_ID)).log(new Status(IStatus.ERROR, Constants.Bundle_ID, e.getMessage(), e));
					} catch (NotEnabledException e) {
						Platform.getLog(Platform.getBundle(Constants.Bundle_ID)).log(new Status(IStatus.ERROR, Constants.Bundle_ID, e.getMessage(), e));
					} catch (NotHandledException e) {
						Platform.getLog(Platform.getBundle(Constants.Bundle_ID)).log(new Status(IStatus.ERROR, Constants.Bundle_ID, e.getMessage(), e));
					}
				}
			});
		}
		return composite;
	}

	private void addAccessibleListeners(Label label, final Image image) {
		label.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent event) {
				final String accessibleMessage = getAccessibleMessageFor(image);
				if (accessibleMessage == null) {
					return;
				}
				event.result = accessibleMessage;
			}
		});
	}

	private String getAccessibleMessageFor(Image image) {
		if (image.equals(getErrorImage())) {
			return JFaceResources.getString("error");//$NON-NLS-1$
		}

		if (image.equals(getWarningImage())) {
			return JFaceResources.getString("warning");//$NON-NLS-1$
		}

		if (image.equals(getInfoImage())) {
			return JFaceResources.getString("info");//$NON-NLS-1$
		}

		if (image.equals(getQuestionImage())) {
			return JFaceResources.getString("question"); //$NON-NLS-1$
		}

		return null;
	}

}
