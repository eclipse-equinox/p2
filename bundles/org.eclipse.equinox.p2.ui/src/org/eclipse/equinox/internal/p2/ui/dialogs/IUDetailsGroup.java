/******************************************************************************* 
* Copyright (c) 2009, 2016 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM Corporation - ongoing development
******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.actions.PropertyDialogAction;
import org.eclipse.equinox.internal.p2.ui.misc.StringMatcher;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Creates a details group for a list of IUs.
 */
public class IUDetailsGroup {

	private static final String LINKACTION = "linkAction"; //$NON-NLS-1$

	private GridLayout layout;
	private StyledText detailsArea;
	private GridData gd;
	private Link propLink;
	private ISelectionProvider selectionProvider;
	private int widthHint;
	private boolean scrollable;
	private String lastText;

	/**
	 * 
	 */
	public IUDetailsGroup(Composite parent, ISelectionProvider selectionProvider, int widthHint, boolean scrollable) {
		this.selectionProvider = selectionProvider;
		this.widthHint = widthHint;
		this.scrollable = scrollable;
		createGroupComposite(parent);
	}

	/**
	 * Creates the group composite that holds the details area
	 * @param parent The parent composite
	 */
	void createGroupComposite(Composite parent) {
		Group detailsComposite = new Group(parent, SWT.NONE);
		GC gc = new GC(parent);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();

		detailsComposite.setText(ProvUIMessages.ProfileModificationWizardPage_DetailsLabel);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		detailsComposite.setLayout(layout);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		detailsComposite.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.verticalIndent = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);
		gd.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		gd.minimumHeight = Dialog.convertHeightInCharsToPixels(fontMetrics, ILayoutConstants.MINIMUM_DESCRIPTION_HEIGHT);
		gd.widthHint = widthHint;
		if (scrollable)
			detailsArea = new StyledText(detailsComposite, SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		else
			detailsArea = new StyledText(detailsComposite, SWT.WRAP | SWT.READ_ONLY);
		detailsArea.setLayoutData(gd);

		gd = new GridData(SWT.END, SWT.BOTTOM, true, false);
		gd.horizontalIndent = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
		propLink = createLink(detailsComposite, new PropertyDialogAction(new SameShellProvider(parent.getShell()), selectionProvider), ProvUIMessages.AvailableIUsPage_GotoProperties);
		propLink.setLayoutData(gd);

		// set the initial state based on selection
		propLink.setVisible(!selectionProvider.getSelection().isEmpty());

	}

	/**
	 * Set the detail text
	 */
	public void setDetailText(String text) {
		// If the string is the same but the user has scrolled, the text
		// widget will reset the selection.  This makes it look like the text
		// has changed when it hasn't.  For this reason, we check equality first.
		if (lastText == null || !lastText.equals(text)) {
			detailsArea.setText(text);
		}
		lastText = text;
	}

	/**
	 * Set the pattern to highlight in the detail text
	 */
	public void setDetailHighlight(String pattern) {
		detailsArea.setStyleRanges(new StyleRange[0]);
		if (pattern != null && !pattern.isEmpty()) {
			StringMatcher matcher = new StringMatcher(pattern, true, false);
			int i = 0;
			StringMatcher.Position match = null;
			do {
				match = matcher.find(lastText, i, lastText.length());
				if (match != null) {
					i = match.getEnd();
					detailsArea.setStyleRange(new StyleRange(match.getStart(), match.getEnd() - match.getStart(), null, null, SWT.BOLD));
				}
			} while (match != null);
		}
	}

	/**
	 * Toggles the property link for the details area.
	 */
	public void enablePropertyLink(boolean enable) {
		propLink.setVisible(enable);
	}

	private Link createLink(Composite parent, IAction action, String text) {
		Link link = new Link(parent, SWT.PUSH);
		link.setText(text);

		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				IAction linkAction = getLinkAction(event.widget);
				if (linkAction != null) {
					linkAction.runWithEvent(event);
				}
			}
		});
		link.setToolTipText(action.getToolTipText());
		link.setData(LINKACTION, action);
		return link;
	}

	IAction getLinkAction(Widget widget) {
		Object data = widget.getData(LINKACTION);
		if (data == null || !(data instanceof IAction)) {
			return null;
		}
		return (IAction) data;
	}

}
