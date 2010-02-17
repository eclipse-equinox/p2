/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala - bug 187762
 *     Mohamed Tarief - tarief@eg.ibm.com - IBM - Bug 174481
 *     Tasktop Technologies - generalized filter code for structured viewers
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.discovery.util;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.Messages;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Based on {@link org.eclipse.ui.dialogs.FilteredTree}.
 * 
 * @author Steffen Pingel
 */
public abstract class FilteredViewer {

	private static Boolean useNativeSearchField;

	private static boolean useNativeSearchField(Composite composite) {
		if (useNativeSearchField == null) {
			useNativeSearchField = Boolean.FALSE;
			Text testText = null;
			try {
				testText = new Text(composite, SWT.SEARCH | SWT.ICON_CANCEL);
				useNativeSearchField = new Boolean((testText.getStyle() & SWT.ICON_CANCEL) != 0);
			} finally {
				if (testText != null) {
					testText.dispose();
				}
			}

		}
		return useNativeSearchField;
	}

	private Label clearFilterTextControl;

	private Composite container;

	private Text filterText;

	private int minimumHeight;

	private String previousFilterText = ""; //$NON-NLS-1$

	private WorkbenchJob refreshJob;

	private long refreshJobDelay = 200L;

	private PatternFilter searchFilter;

	protected StructuredViewer viewer;

	public FilteredViewer() {
		// constructor
	}

	private void clearFilterText() {
		filterText.setText(""); //$NON-NLS-1$
		filterTextChanged();
	}

	private Label createClearFilterTextControl(Composite filterContainer, final Text filterText) {
		final Image inactiveImage = DiscoveryImages.FIND_CLEAR_DISABLED.createImage();
		final Image activeImage = DiscoveryImages.FIND_CLEAR.createImage();
		final Image pressedImage = new Image(filterContainer.getDisplay(), activeImage, SWT.IMAGE_GRAY);

		final Label clearButton = new Label(filterContainer, SWT.NONE);
		clearButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		clearButton.setImage(inactiveImage);
		clearButton.setToolTipText(Messages.ConnectorDiscoveryWizardMainPage_clearButton_toolTip);
		clearButton.setBackground(filterContainer.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		clearButton.addMouseListener(new MouseAdapter() {
			private MouseMoveListener fMoveListener;

			private boolean isMouseInButton(MouseEvent e) {
				Point buttonSize = clearButton.getSize();
				return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y && e.y < buttonSize.y;
			}

			@Override
			public void mouseDown(MouseEvent e) {
				clearButton.setImage(pressedImage);
				fMoveListener = new MouseMoveListener() {
					private boolean fMouseInButton = true;

					public void mouseMove(MouseEvent e) {
						boolean mouseInButton = isMouseInButton(e);
						if (mouseInButton != fMouseInButton) {
							fMouseInButton = mouseInButton;
							clearButton.setImage(mouseInButton ? pressedImage : inactiveImage);
						}
					}
				};
				clearButton.addMouseMoveListener(fMoveListener);
			}

			@Override
			public void mouseUp(MouseEvent e) {
				if (fMoveListener != null) {
					clearButton.removeMouseMoveListener(fMoveListener);
					fMoveListener = null;
					boolean mouseInButton = isMouseInButton(e);
					clearButton.setImage(mouseInButton ? activeImage : inactiveImage);
					if (mouseInButton) {
						clearFilterText();
						filterText.setFocus();
					}
				}
			}
		});
		clearButton.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent e) {
				clearButton.setImage(activeImage);
			}

			public void mouseExit(MouseEvent e) {
				clearButton.setImage(inactiveImage);
			}

			public void mouseHover(MouseEvent e) {
			}
		});
		clearButton.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				inactiveImage.dispose();
				activeImage.dispose();
				pressedImage.dispose();
			}
		});
		clearButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result = Messages.ConnectorDiscoveryWizardMainPage_clearButton_accessibleListener;
			}
		});
		clearButton.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_PUSHBUTTON;
			}
		});
		return clearButton;
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
		container.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (refreshJob != null) {
					refreshJob.cancel();
				}
			}
		});

		doCreateHeader();

		viewer = doCreateViewer(container);
		searchFilter = doCreateFilter();
		viewer.addFilter(searchFilter);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, minimumHeight).applyTo(viewer.getControl());
	}

	protected PatternFilter doCreateFilter() {
		return new PatternFilter() {
			@Override
			protected boolean isParentMatch(Viewer viewer, Object element) {
				return false;
			}
		};
	}

	private void doCreateFindControl(Composite header) {
		Label label = new Label(header, SWT.NONE);
		label.setText(Messages.ConnectorDiscoveryWizardMainPage_filterLabel);

		Composite textFilterContainer;
		boolean nativeSearch = useNativeSearchField(header);
		if (nativeSearch) {
			textFilterContainer = new Composite(header, SWT.NONE);
		} else {
			textFilterContainer = new Composite(header, SWT.BORDER);
			textFilterContainer.setBackground(header.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(textFilterContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(textFilterContainer);

		if (nativeSearch) {
			filterText = new Text(textFilterContainer, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		} else {
			filterText = new Text(textFilterContainer, SWT.SINGLE);
		}

		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				filterTextChanged();
			}
		});
		if (nativeSearch) {
			filterText.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					if (e.detail == SWT.ICON_CANCEL) {
						clearFilterText();
					}
				}
			});
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(filterText);
		} else {
			GridDataFactory.fillDefaults().grab(true, false).applyTo(filterText);
			clearFilterTextControl = createClearFilterTextControl(textFilterContainer, filterText);
			clearFilterTextControl.setVisible(false);
		}
	}

	private void doCreateHeader() {
		Composite header = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(header);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(header);

		doCreateFindControl(header);
		doCreateHeaderControls(header);

		// arrange all header controls horizontally
		GridLayoutFactory.fillDefaults().numColumns(header.getChildren().length).applyTo(header);
	}

	protected void doCreateHeaderControls(Composite header) {
		// ignore
	}

	protected WorkbenchJob doCreateRefreshJob() {
		return new WorkbenchJob("filter") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (filterText.isDisposed()) {
					return Status.CANCEL_STATUS;
				}
				String text = filterText.getText();
				text = text.trim();

				if (!previousFilterText.equals(text)) {
					previousFilterText = text;
					doFind(text);
				}
				return Status.OK_STATUS;
			}
		};
	}

	protected abstract StructuredViewer doCreateViewer(Composite container);

	protected void doFind(String text) {
		searchFilter.setPattern(text);
		if (clearFilterTextControl != null) {
			clearFilterTextControl.setVisible(text != null && text.length() != 0);
		}
		viewer.refresh(true);
	}

	private void filterTextChanged() {
		if (refreshJob == null) {
			refreshJob = doCreateRefreshJob();
		} else {
			refreshJob.cancel();
		}
		refreshJob.schedule(refreshJobDelay);
	}

	public Control getControl() {
		return container;
	}

	public int getMinimumHeight() {
		return minimumHeight;
	}

	protected long getRefreshJobDelay() {
		return refreshJobDelay;
	}

	public StructuredViewer getViewer() {
		return viewer;
	}

	public void setMinimumHeight(int minimumHeight) {
		this.minimumHeight = minimumHeight;
		if (viewer != null) {
			GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, minimumHeight).applyTo(viewer.getControl());
		}
	}

	protected void setRefreshJobDelay(long refreshJobDelay) {
		this.refreshJobDelay = refreshJobDelay;
	}

}
