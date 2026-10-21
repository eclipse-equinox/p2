/*******************************************************************************
* Copyright (c) 2009, 2026 EclipseSource and others.
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
*   EclipseSource - initial API and implementation
*   IBM Corporation - ongoing development
******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.text.StringMatcher;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.actions.PropertyDialogAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Creates a details group for a list of IUs.
 */
public class IUDetailsGroup {

	private static final String LINKACTION = "linkAction"; //$NON-NLS-1$

	private GridLayout layout;
	private Composite groupComposite;
	private StyledText detailsArea;

	private Composite problemComposite;
	private TreeViewer problemViewer;
	private TreeColumnLayout problemColumnLayout;
	private TreeViewerColumn problemNameColumn;
	private TreeViewerColumn problemMessageColumn;
	private GridData gd;
	private Link propLink;
	private final ISelectionProvider selectionProvider;
	private final int widthHint;
	private final boolean scrollable;
	private final int problemTreeHeightInChars;
	private String lastText;

	public IUDetailsGroup(Composite parent, ISelectionProvider selectionProvider, int widthHint, boolean scrollable) {
		this(parent, selectionProvider, widthHint, scrollable, ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
	}

	public IUDetailsGroup(Composite parent, ISelectionProvider selectionProvider, int widthHint, boolean scrollable, int problemTreeHeightInChars) {
		this.selectionProvider = selectionProvider;
		this.widthHint = widthHint;
		this.scrollable = scrollable;
		this.problemTreeHeightInChars = problemTreeHeightInChars;
		createGroupComposite(parent);
	}

	/**
	 * Creates the group composite that holds the details area
	 * @param parent The parent composite
	 */
	void createGroupComposite(Composite parent) {
		Group detailsComposite = new Group(parent, SWT.NONE);
		groupComposite = detailsComposite;
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
		if (scrollable) {
			detailsArea = new StyledText(detailsComposite, SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		} else {
			detailsArea = new StyledText(detailsComposite, SWT.WRAP | SWT.READ_ONLY);
		}
		detailsArea.setLayoutData(gd);

		createProblemViewer(detailsComposite, gd, fontMetrics);

		gd = new GridData(SWT.END, SWT.BOTTOM, true, false);
		gd.horizontalIndent = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
		propLink = createLink(detailsComposite, new PropertyDialogAction(new SameShellProvider(parent.getShell()), selectionProvider), ProvUIMessages.AvailableIUsPage_GotoProperties);
		propLink.setLayoutData(gd);

		// set the initial state based on selection
		propLink.setVisible(!selectionProvider.getSelection().isEmpty());

	}

	private void createProblemViewer(Composite parent, GridData detailsAreaData, FontMetrics fontMetrics) {
		// The composite takes the place of the details area and lets the columns share the available width.
		problemComposite = new Composite(parent, SWT.NONE);
		TreeColumnLayout columnLayout = new TreeColumnLayout();
		problemComposite.setLayout(columnLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.verticalIndent = detailsAreaData.verticalIndent;
		data.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, problemTreeHeightInChars);
		data.minimumHeight = detailsAreaData.minimumHeight;
		data.widthHint = detailsAreaData.widthHint;
		data.exclude = true;
		problemComposite.setLayoutData(data);
		problemComposite.setVisible(false);

		problemViewer = new TreeViewer(problemComposite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		problemViewer.getTree().setHeaderVisible(true);
		problemViewer.setContentProvider(new ProblemContentProvider());

		TreeViewerColumn nameColumn = new TreeViewerColumn(problemViewer, SWT.NONE);
		nameColumn.getColumn().setText(ProvUIMessages.ProvUI_NameColumnTitle);
		nameColumn.setLabelProvider(new ProblemLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ResolutionProblems.Group group) {
					return group.getTitle();
				}
				return element instanceof ResolutionProblems.Problem problem ? problem.getName() : ""; //$NON-NLS-1$
			}
		});
		columnLayout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(40, 150));
		problemNameColumn = nameColumn;

		TreeViewerColumn messageColumn = new TreeViewerColumn(problemViewer, SWT.NONE);
		messageColumn.getColumn().setText(ProvUIMessages.ResolutionProblems_MessageColumn);
		messageColumn.setLabelProvider(new ProblemLabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof ResolutionProblems.Problem problem ? problem.getMessage() : ""; //$NON-NLS-1$
			}

			@Override
			public Image getImage(Object element) {
				return null;
			}
		});
		columnLayout.setColumnData(messageColumn.getColumn(), new ColumnWeightData(60, 150));
		problemColumnLayout = columnLayout;
		problemMessageColumn = messageColumn;

		ColumnViewerToolTipSupport.enableFor(problemViewer);
	}

	public void setProblems(List<ResolutionProblems.Group> groups) {
		problemViewer.setInput(groups);
		problemViewer.expandAll();
		showProblems(true);
		makeColumnsScrollable();
	}

	private void makeColumnsScrollable() {
		keepAtLeastContentWidth(problemNameColumn.getColumn(), 40);
		keepAtLeastContentWidth(problemMessageColumn.getColumn(), 60);
		problemComposite.layout(true);
	}

	private void keepAtLeastContentWidth(TreeColumn column, int weight) {
		column.pack();
		problemColumnLayout.setColumnData(column, new ColumnWeightData(weight, column.getWidth()));
	}

	private void showProblems(boolean show) {
		if (problemComposite.getVisible() == show) {
			return;
		}
		problemComposite.setVisible(show);
		((GridData) problemComposite.getLayoutData()).exclude = !show;
		detailsArea.setVisible(!show);
		((GridData) detailsArea.getLayoutData()).exclude = show;
		groupComposite.layout(true);
	}

	/**
	 * Set the detail text
	 */
	public void setDetailText(String text) {
		showProblems(false);
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

		link.addListener(SWT.Selection, event -> {
			IAction linkAction = getLinkAction(event.widget);
			if (linkAction != null) {
				linkAction.runWithEvent(event);
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

	/**
	 * Provides the groups as the top level elements, and the problems of a group as its children.
	 */
	private static final class ProblemContentProvider implements ITreeContentProvider {
		private static final Object[] NO_ELEMENTS = new Object[0];

		@Override
		public Object[] getElements(Object inputElement) {
			return inputElement instanceof List<?> groups ? groups.toArray() : NO_ELEMENTS;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return parentElement instanceof ResolutionProblems.Group group ? group.getProblems().toArray() : NO_ELEMENTS;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof ResolutionProblems.Group group && !group.getProblems().isEmpty();
		}
	}

	/**
	 * Shows the severity of a group or a problem as image.
	 */
	private abstract static class ProblemLabelProvider extends ColumnLabelProvider {

		@Override
		public Image getImage(Object element) {
			int severity = IStatus.INFO;
			if (element instanceof ResolutionProblems.Group group) {
				severity = group.getSeverity();
			} else if (element instanceof ResolutionProblems.Problem problem) {
				severity = problem.getSeverity();
			}
			ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
			if (severity == IStatus.ERROR) {
				return images.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
			if (severity == IStatus.WARNING) {
				return images.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			}
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof ResolutionProblems.Problem problem) {
				return problem.getMessage();
			}
			return null;
		}
	}

}
