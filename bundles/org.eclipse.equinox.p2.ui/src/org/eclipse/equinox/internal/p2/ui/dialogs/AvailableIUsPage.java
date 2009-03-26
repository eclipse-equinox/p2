/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import com.ibm.icu.text.Collator;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.EmptyElementExplanation;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.*;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AvailableIUGroup;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

public class AvailableIUsPage extends ProvisioningWizardPage implements ISelectableIUsPage {

	private static final String DIALOG_SETTINGS_SECTION = "AvailableIUsPage"; //$NON-NLS-1$
	private static final String AVAILABLE_VIEW_TYPE = "AvailableViewType"; //$NON-NLS-1$
	private static final String SHOW_LATEST_VERSIONS_ONLY = "ShowLatestVersionsOnly"; //$NON-NLS-1$
	private static final String HIDE_INSTALLED_IUS = "HideInstalledContent"; //$NON-NLS-1$
	private static final String RESOLVE_ALL = "ResolveInstallWithAllSites"; //$NON-NLS-1$
	private static final String LINKACTION = "linkAction"; //$NON-NLS-1$
	private static final int DEFAULT_WIDTH = 300;
	private static final String SITE_NONE = ProvUIMessages.AvailableIUsPage_NoSites;
	private static final int INDEX_SITE_NONE = 0;
	private static final String SITE_ALL = ProvUIMessages.AvailableIUsPage_AllSites;
	private static final int INDEX_SITE_ALL = 1;
	private static final String SITE_LOCAL = ProvUIMessages.AvailableIUsPage_LocalSites;
	private static final int DEC_MARGIN_WIDTH = 2;

	String profileId;
	Policy policy;
	Object[] initialSelections;
	QueryableMetadataRepositoryManager manager;
	IUViewQueryContext queryContext;
	AvailableIUGroup availableIUGroup;
	Composite availableIUButtonBar;
	Combo repoCombo;
	Link repoLink, installLink;
	Button useCategoriesCheckbox, hideInstalledCheckbox, showLatestVersionsCheckbox, resolveAllCheckbox;
	Text detailsArea;
	StructuredViewerProvisioningListener profileListener;
	ProvUIProvisioningListener comboRepoListener;
	Display display;
	ControlDecoration repoDec;
	Image info, warning, error;
	int batchCount = 0;
	URI[] comboRepos;
	ComboAutoCompleteField repoAutoComplete;
	IUDetailsGroup iuDetailsGroup;

	public AvailableIUsPage(Policy policy, String profileId, QueryableMetadataRepositoryManager manager) {
		super("AvailableSoftwarePage"); //$NON-NLS-1$
		this.policy = policy;
		this.profileId = profileId;
		this.manager = manager;
		makeQueryContext();
		setTitle(ProvUIMessages.AvailableIUsPage_Title);
		setDescription(ProvUIMessages.AvailableIUsPage_Description);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		this.display = parent.getDisplay();

		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);
		composite.setLayoutData(gd);
		setDropTarget(composite);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;

		composite.setLayout(layout);
		// Repo manipulation 
		createRepoArea(composite);

		// Now the available group 
		// If we have a repository manipulator, we want to default to showing no repos.  Otherwise all.
		int filterConstant = AvailableIUGroup.AVAILABLE_NONE;
		if (policy.getRepositoryManipulator() == null)
			filterConstant = AvailableIUGroup.AVAILABLE_ALL;
		availableIUGroup = new AvailableIUGroup(policy, composite, JFaceResources.getDialogFont(), manager, queryContext, ProvUI.getIUColumnConfig(), filterConstant);

		// Selection listeners must be registered on both the normal selection
		// events and the check mark events.  Must be done after buttons 
		// are created so that the buttons can register and receive their selection notifications before us.
		availableIUGroup.getStructuredViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateDetails();
				iuDetailsGroup.enablePropertyLink(availableIUGroup.getSelectedIUElements().length == 1);
			}
		});

		availableIUGroup.getCheckboxTreeViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				validateNextButton();
			}
		});

		addViewerProvisioningListeners();

		availableIUGroup.setUseBoldFontForFilteredItems(queryContext.getViewType() != IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		setDropTarget(availableIUGroup.getStructuredViewer().getControl());
		activateCopy(availableIUGroup.getStructuredViewer().getControl());

		// Details area
		iuDetailsGroup = new IUDetailsGroup(composite, availableIUGroup.getStructuredViewer(), convertHorizontalDLUsToPixels(DEFAULT_WIDTH), false);
		detailsArea = iuDetailsGroup.getDetailsArea();

		// Controls for filtering/presentation/site selection
		Composite controlsComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		controlsComposite.setLayout(layout);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		controlsComposite.setLayoutData(gd);

		createViewControlsArea(controlsComposite);

		initializeWidgetState();
		setControl(composite);
		composite.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				removeProvisioningListeners();
			}

		});
	}

	private void createViewControlsArea(Composite parent) {
		showLatestVersionsCheckbox = new Button(parent, SWT.CHECK);
		showLatestVersionsCheckbox.setText(ProvUIMessages.AvailableIUsPage_ShowLatestVersions);
		showLatestVersionsCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}

			public void widgetSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}
		});

		hideInstalledCheckbox = new Button(parent, SWT.CHECK);
		hideInstalledCheckbox.setText(ProvUIMessages.AvailableIUsPage_HideInstalledItems);
		hideInstalledCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}

			public void widgetSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}
		});

		useCategoriesCheckbox = new Button(parent, SWT.CHECK);
		useCategoriesCheckbox.setText(ProvUIMessages.AvailableIUsPage_GroupByCategory);
		useCategoriesCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}

			public void widgetSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}
		});

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		installLink = createLink(parent, new Action() {
			public void runWithEvent(Event event) {
				ProvUI.openInstallationDialog(event);
			}
		}, ProvUIMessages.AvailableIUsPage_GotoInstallInfo);
		installLink.setLayoutData(gd);

		if (policy.getRepositoryManipulator() != null) {
			// Checkbox
			resolveAllCheckbox = new Button(parent, SWT.CHECK);
			resolveAllCheckbox.setText(ProvUIMessages.AvailableIUsPage_ResolveAllCheckbox);
			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 2;
			resolveAllCheckbox.setLayoutData(gd);
		}
	}

	private void createRepoArea(Composite parent) {
		// Site controls are only available if a repository manipulator
		// is specified.
		final RepositoryManipulator repoMan = policy.getRepositoryManipulator();
		if (repoMan != null) {
			// Get the possible field error indicators
			info = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage();
			warning = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage();
			error = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
			// Combo that filters sites
			Composite comboComposite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginTop = 0;
			layout.marginBottom = IDialogConstants.VERTICAL_SPACING;
			layout.numColumns = 3;
			comboComposite.setLayout(layout);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			comboComposite.setLayoutData(gd);

			Label label = new Label(comboComposite, SWT.NONE);
			label.setText(ProvUIMessages.AvailableIUsPage_RepoFilterLabel);

			repoCombo = new Combo(comboComposite, SWT.DROP_DOWN);
			repoCombo.addSelectionListener(new SelectionListener() {

				public void widgetDefaultSelected(SelectionEvent e) {
					repoComboSelectionChanged();
				}

				public void widgetSelected(SelectionEvent e) {
					repoComboSelectionChanged();
				}

			});
			// Auto complete - install before our own key listeners, so that auto complete gets first shot.
			repoAutoComplete = new ComboAutoCompleteField(repoCombo);

			repoCombo.addKeyListener(new KeyAdapter() {

				public void keyPressed(KeyEvent e) {
					if (e.keyCode == SWT.CR)
						addRepository(false);
				}
			});

			// We don't ever want this to be interpreted as a default
			// button event
			repoCombo.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_RETURN) {
						e.doit = false;
					}
				}
			});

			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			// breathing room for info dec
			gd.horizontalIndent = DEC_MARGIN_WIDTH * 2;
			repoCombo.setLayoutData(gd);
			repoCombo.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent event) {
					URI location = null;
					IStatus status = null;
					try {
						String text = repoCombo.getText();
						int index = getComboIndex(text);
						// only validate text that doesn't match existing text in combo
						if (index < 0) {
							location = URIUtil.fromString(repoCombo.getText());
							RepositoryLocationValidator validator = repoMan.getRepositoryLocationValidator(getShell());
							status = validator.validateRepositoryLocation(location, false, new NullProgressMonitor());
						} else {
							// user typed or pasted an existing location.  Select it.
							repoComboSelectionChanged();
						}
					} catch (URISyntaxException e) {
						status = RepositoryLocationValidator.getInvalidLocationStatus(repoCombo.getText());
					}
					setRepoComboDecoration(status);
				}
			});

			repoDec = new ControlDecoration(repoCombo, SWT.LEFT | SWT.TOP);
			repoDec.setMarginWidth(DEC_MARGIN_WIDTH);

			DropTarget target = new DropTarget(repoCombo, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
			target.addDropListener(new URLDropAdapter(true) {
				/* (non-Javadoc)
				 * @see org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLDropAdapter#handleURLString(java.lang.String, org.eclipse.swt.dnd.DropTargetEvent)
				 */
				protected void handleDrop(String urlText, DropTargetEvent event) {
					repoCombo.setText(urlText);
					event.detail = DND.DROP_LINK;
					addRepository(false);
				}
			});

			Button button = new Button(comboComposite, SWT.PUSH);
			button.setText(ProvUIMessages.AvailableIUsPage_AddButton);
			button.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					addRepository(true);
				}

				public void widgetSelected(SelectionEvent e) {
					addRepository(true);
				}
			});

			// Link to repository manipulator
			repoLink = createLink(comboComposite, new Action() {
				public void runWithEvent(Event event) {
					policy.getRepositoryManipulator().manipulateRepositories(getShell());
				}
			}, policy.getRepositoryManipulator().getManipulatorLinkLabel());
			gd = new GridData(SWT.END, SWT.FILL, true, false);
			gd.horizontalSpan = 3;
			repoLink.setLayoutData(gd);

			addComboProvisioningListeners();
		}
	}

	void validateNextButton() {
		setPageComplete(availableIUGroup.getCheckedLeafIUs().length > 0);
	}

	void updateQueryContext() {
		queryContext.setShowLatestVersionsOnly(showLatestVersionsCheckbox.getSelection());
		if (hideInstalledCheckbox.getSelection())
			queryContext.hideAlreadyInstalled(profileId);
		else {
			queryContext.showAlreadyInstalled();
			queryContext.setInstalledProfileId(profileId);
		}
		if (useCategoriesCheckbox.getSelection())
			queryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);
		else
			queryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
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

	private void setDropTarget(Control control) {
		if (policy.getRepositoryManipulator() != null) {
			DropTarget target = new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
			target.addDropListener(new RepositoryManipulatorDropTarget(policy.getRepositoryManipulator(), control));
		}
	}

	private void initializeWidgetState() {
		// Set widgets according to query context
		hideInstalledCheckbox.setSelection(queryContext.getHideAlreadyInstalled());
		showLatestVersionsCheckbox.setSelection(queryContext.getShowLatestVersionsOnly());
		useCategoriesCheckbox.setSelection(queryContext.shouldGroupByCategories());
		availableIUGroup.updateAvailableViewState();
		if (initialSelections != null)
			availableIUGroup.setChecked(initialSelections);

		// Focus should go on site combo unless it's not there.  In that case, go to the filter text.
		Control focusControl = null;
		if (repoCombo != null)
			focusControl = repoCombo;
		else
			focusControl = availableIUGroup.getDefaultFocusControl();
		if (focusControl != null)
			focusControl.setFocus();
		updateDetails();
		iuDetailsGroup.enablePropertyLink(availableIUGroup.getSelectedIUElements().length == 1);
		validateNextButton();

		if (repoCombo != null) {
			fillRepoCombo(SITE_NONE);
			setRepoComboDecoration(null);
			setDescription(ProvUIMessages.AvailableIUsPage_SelectASite);
		}

		if (resolveAllCheckbox != null) {
			IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
			IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
			String value = null;
			if (section != null)
				value = section.get(RESOLVE_ALL);
			// no section or no value in the section
			if (value == null)
				resolveAllCheckbox.setSelection(true);
			else
				resolveAllCheckbox.setSelection(section.getBoolean(RESOLVE_ALL));
		}
	}

	public boolean performFinish() {
		savePageSettings();
		return true;
	}

	private void makeQueryContext() {
		// Make a local query context that is based on the default.
		IUViewQueryContext defaultQueryContext = policy.getQueryContext();
		queryContext = new IUViewQueryContext(defaultQueryContext.getViewType());
		queryContext.setArtifactRepositoryFlags(defaultQueryContext.getArtifactRepositoryFlags());
		queryContext.setMetadataRepositoryFlags(defaultQueryContext.getMetadataRepositoryFlags());
		if (defaultQueryContext.getHideAlreadyInstalled()) {
			queryContext.hideAlreadyInstalled(profileId);
		} else {
			queryContext.setInstalledProfileId(profileId);
		}
		queryContext.setShowLatestVersionsOnly(defaultQueryContext.getShowLatestVersionsOnly());
		queryContext.setVisibleAvailableIUProperty(defaultQueryContext.getVisibleAvailableIUProperty());
		queryContext.setVisibleInstalledIUProperty(defaultQueryContext.getVisibleInstalledIUProperty());
		// Now check for saved away dialog settings
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section != null) {
			// View by...
			try {
				if (section.get(AVAILABLE_VIEW_TYPE) != null)
					queryContext.setViewType(section.getInt(AVAILABLE_VIEW_TYPE));
			} catch (NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.  
			}
			// We no longer (in 3.5) show a view by site, so ignore any older dialog setting that
			// instructs us to do this.
			if (queryContext.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_BY_REPO)
				queryContext.setViewType(IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);

			// Show latest versions
			if (section.get(SHOW_LATEST_VERSIONS_ONLY) != null)
				queryContext.setShowLatestVersionsOnly(section.getBoolean(SHOW_LATEST_VERSIONS_ONLY));

			// Hide installed content
			boolean hideContent = section.getBoolean(HIDE_INSTALLED_IUS);
			if (hideContent)
				queryContext.hideAlreadyInstalled(profileId);
			else {
				queryContext.setInstalledProfileId(profileId);
				queryContext.showAlreadyInstalled();
			}
		}
	}

	private void savePageSettings() {
		if (getShell().isDisposed())
			return;
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		section.put(AVAILABLE_VIEW_TYPE, queryContext.getViewType());
		section.put(SHOW_LATEST_VERSIONS_ONLY, showLatestVersionsCheckbox.getSelection());
		section.put(HIDE_INSTALLED_IUS, hideInstalledCheckbox.getSelection());
		if (resolveAllCheckbox != null)
			section.put(RESOLVE_ALL, resolveAllCheckbox.getSelection());
	}

	void updateDetails() {
		// First look for an empty explanation.
		Object[] elements = ((IStructuredSelection) availableIUGroup.getStructuredViewer().getSelection()).toArray();
		if (elements.length == 1 && elements[0] instanceof EmptyElementExplanation) {
			String description = ((EmptyElementExplanation) elements[0]).getDescription();
			if (description != null) {
				detailsArea.setText(description);
				return;
			}
		}

		// Now look for IU's
		IInstallableUnit[] selected = getSelectedIUs();
		if (selected.length == 1) {
			StringBuffer result = new StringBuffer();
			String description = IUPropertyUtils.getIUProperty(selected[0], IInstallableUnit.PROP_DESCRIPTION);
			if (description != null) {
				result.append(description);
			} else {
				String name = IUPropertyUtils.getIUProperty(selected[0], IInstallableUnit.PROP_NAME);
				if (name != null)
					result.append(name);
				else
					result.append(selected[0].getId());
				result.append(" "); //$NON-NLS-1$
				result.append(selected[0].getVersion().toString());
			}

			detailsArea.setText(result.toString());
			return;
		}
		detailsArea.setText(""); //$NON-NLS-1$
	}

	public IInstallableUnit[] getSelectedIUs() {
		return availableIUGroup.getSelectedIUs();
	}

	/*
	 * This method is provided only for automated testing.
	 */
	public AvailableIUGroup testGetAvailableIUGroup() {
		return availableIUGroup;
	}

	public IInstallableUnit[] getCheckedIUs() {
		return availableIUGroup.getCheckedLeafIUs();
	}

	/*
	 * Overridden so that we don't call getNextPage().
	 * We use getNextPage() to start resolving the install so
	 * we only want to do that when the next button is pressed.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage#getCheckedIUElements()
	 */
	public Object[] getCheckedIUElements() {
		return availableIUGroup.getCheckedLeafIUs();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage#getSelectedIUElements()
	 */
	public Object[] getSelectedIUElements() {
		return availableIUGroup.getSelectedIUElements();
	}

	/**
	 * Set the initial selections to be used in this page.  This method has no effect
	 * once the page has been created.
	 * 
	 * @param elements
	 */
	public void setInitialSelections(Object[] elements) {
		initialSelections = elements;
	}

	/*
	 *  Add a repository using the text in the combo or launch a dialog if the text
	 *  represents an already known repo.  For any add operation spawned by this
	 *  method, we do not want to notify the UI with a special listener.  This is to
	 *  prevent a multiple update flash because we intend to reset the available IU
	 *  filter as soon as the new repo is added.
	 */
	void addRepository(boolean alwaysPrompt) {
		final RepositoryManipulator repoMan = policy.getRepositoryManipulator();
		if (repoMan == null)
			return;
		final String selectedRepo = repoCombo.getText();
		int selectionIndex = getComboIndex(selectedRepo);
		final boolean isNewText = selectionIndex < 0;
		// If we are adding something already in the combo, just
		// select that item.
		if (!alwaysPrompt && !isNewText && selectionIndex != repoCombo.getSelectionIndex()) {
			repoComboSelectionChanged();
		} else if (alwaysPrompt) {
			AddRepositoryDialog dialog = new AddRepositoryDialog(getShell(), policy) {
				protected AddRepositoryOperation getOperation(URI repositoryLocation) {
					AddRepositoryOperation op = repoMan.getAddOperation(repositoryLocation);
					op.setNotify(false);
					return op;
				}

				protected String getInitialLocationText() {
					if (isNewText)
						return selectedRepo;
					return super.getInitialLocationText();
				}

			};
			dialog.setTitle(repoMan.getAddOperationLabel());
			dialog.open();
			URI location = dialog.getAddedLocation();
			if (location != null)
				fillRepoCombo(getSiteString(location));
		} else if (isNewText) {
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						URI location = null;
						IStatus status;
						try {
							location = URIUtil.fromString(selectedRepo);
							RepositoryLocationValidator validator = repoMan.getRepositoryLocationValidator(getShell());
							status = validator.validateRepositoryLocation(location, false, monitor);
						} catch (URISyntaxException e) {
							status = RepositoryLocationValidator.getInvalidLocationStatus(selectedRepo);
						}
						if (status.isOK() && location != null) {
							try {
								RepositoryOperation op = repoMan.getAddOperation(location);
								op.setNotify(false);
								op.execute(monitor);
								fillRepoCombo(getSiteString(location));
							} catch (ProvisionException e) {
								// TODO Auto-generated catch block
								ProvUI.handleException(e, null, StatusManager.SHOW);
							}
						}
						setRepoComboDecoration(status);
					}
				});
			} catch (InvocationTargetException e) {
				// ignore
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	void fillRepoCombo(final String selection) {
		if (repoCombo == null || policy.getRepositoryManipulator() == null)
			return;
		URI[] sites = policy.getRepositoryManipulator().getKnownRepositories();
		boolean hasLocalSites = getLocalSites().length > 0;
		final String[] items;
		if (hasLocalSites) {
			// None, All, repo1, repo2....repo n, Local
			comboRepos = new URI[sites.length + 3];
			items = new String[sites.length + 3];
		} else {
			// None, All, repo1, repo2....repo n
			comboRepos = new URI[sites.length + 2];
			items = new String[sites.length + 2];
		}
		items[INDEX_SITE_NONE] = SITE_NONE;
		items[INDEX_SITE_ALL] = SITE_ALL;
		for (int i = 0; i < sites.length; i++) {
			items[i + 2] = getSiteString(sites[i]);
			comboRepos[i + 2] = sites[i];
		}
		if (hasLocalSites)
			items[items.length - 1] = SITE_LOCAL;
		if (sites.length > 0)
			sortRepoItems(items, comboRepos, hasLocalSites);
		Runnable runnable = new Runnable() {
			public void run() {
				if (repoCombo == null || repoCombo.isDisposed())
					return;
				// If the combo is open and something is selected, use that index if we
				// weren't given a string to select.
				int selIndex = repoCombo.getSelectionIndex();
				if (selIndex < 0)
					selIndex = 0;
				String repoToSelect = selection == null ? repoCombo.getItem(selIndex) : selection;
				repoCombo.setItems(items);
				boolean selected = false;
				for (int i = 0; i < items.length; i++)
					if (items[i].equals(repoToSelect)) {
						selected = true;
						if (repoCombo.getListVisible())
							repoCombo.select(i);
						repoCombo.setText(repoToSelect);
						break;
					}
				if (!selected) {
					if (repoCombo.getListVisible())
						repoCombo.select(INDEX_SITE_NONE);
					repoCombo.setText(SITE_NONE);
				}
				repoComboSelectionChanged();
			}
		};
		// Only run the UI code async if we have to.  If we always async the code,
		// the automated tests (which are in the UI thread) can get out of sync
		if (Display.getCurrent() == null)
			display.asyncExec(runnable);
		else
			runnable.run();
	}

	String getSiteString(URI uri) {
		try {
			String nickname = ProvisioningUtil.getMetadataRepositoryProperty(uri, IRepository.PROP_NICKNAME);
			if (nickname != null && nickname.length() > 0)
				return NLS.bind(ProvUIMessages.AvailableIUsPage_NameWithLocation, nickname, URIUtil.toUnencodedString(uri));
		} catch (ProvisionException e) {
			// No error, just use the location string
		}
		return URIUtil.toUnencodedString(uri);
	}

	private void sortRepoItems(String[] strings, URI[] locations, boolean hasLocalSites) {
		int sortStart = 2;
		int sortEnd = hasLocalSites ? strings.length - 2 : strings.length - 1;
		if (sortStart >= sortEnd)
			return;
		final Collator collator = Collator.getInstance(Locale.getDefault());
		Comparator stringComparator = new Comparator() {
			public int compare(Object a, Object b) {
				return collator.compare(a, b);
			}
		};
		Comparator uriComparator = new Comparator() {
			public int compare(Object a, Object b) {
				return collator.compare(getSiteString((URI) a), getSiteString((URI) b));
			}
		};

		Arrays.sort(strings, sortStart, sortEnd, stringComparator);
		Arrays.sort(locations, sortStart, sortEnd, uriComparator);
	}

	private URI[] getLocalSites() {
		// use our current visibility flags plus the local filter
		int flags = queryContext.getMetadataRepositoryFlags() | IRepositoryManager.REPOSITORIES_LOCAL;
		try {
			return ProvisioningUtil.getMetadataRepositories(flags);
		} catch (ProvisionException e) {
			return null;
		}
	}

	int getComboIndex(String repoText) {
		int index = -1;
		if (repoText.length() > 0) {
			String[] items = repoCombo.getItems();
			for (int i = 0; i < items.length; i++)
				if (repoText.equals(items[i])) {
					index = i;
					break;
				}
		}
		return index;
	}

	void repoComboSelectionChanged() {
		int selection = -1;
		if (repoCombo.getListVisible())
			selection = repoCombo.getSelectionIndex();
		else {
			selection = getComboIndex(repoCombo.getText());
		}
		int localIndex = getLocalSites().length == 0 ? repoCombo.getItemCount() : repoCombo.getItemCount() - 1;
		if (comboRepos == null || selection < 0)
			selection = INDEX_SITE_NONE;
		String description = ProvUIMessages.AvailableIUsPage_Description;
		if (selection == INDEX_SITE_NONE) {
			availableIUGroup.setRepositoryFilter(AvailableIUGroup.AVAILABLE_NONE, null);
			description = ProvUIMessages.AvailableIUsPage_SelectASite;
		} else if (selection == INDEX_SITE_ALL) {
			availableIUGroup.setRepositoryFilter(AvailableIUGroup.AVAILABLE_ALL, null);
		} else if (selection >= localIndex) {
			availableIUGroup.setRepositoryFilter(AvailableIUGroup.AVAILABLE_LOCAL, null);
		} else {
			availableIUGroup.setRepositoryFilter(AvailableIUGroup.AVAILABLE_SPECIFIED, comboRepos[selection]);
		}
		validateNextButton();
		setDescription(description);
	}

	void addViewerProvisioningListeners() {
		// We might need to adjust the content of the available IU group's viewer
		// according to installation changes.  We want to be very selective about refreshing,
		// because the viewer has its own listeners installed.
		profileListener = new StructuredViewerProvisioningListener(availableIUGroup.getStructuredViewer(), ProvUIProvisioningListener.PROV_EVENT_PROFILE) {
			protected void profileAdded(String id) {
				// do nothing
			}

			protected void profileRemoved(String id) {
				// do nothing
			}

			protected void profileChanged(String id) {
				if (id.equals(profileId)) {
					asyncRefresh();
				}
			}
		};

		ProvUI.addProvisioningListener(profileListener);
	}

	void addComboProvisioningListeners() {
		// We need to monitor repository events so that we can adjust the repo combo.
		comboRepoListener = new ProvUIProvisioningListener(ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY) {
			protected void repositoryAdded(RepositoryEvent e) {
				if (e instanceof UIRepositoryEvent) {
					fillRepoCombo(getSiteString(e.getRepositoryLocation()));
				}
			}

			protected void repositoryRemoved(RepositoryEvent e) {
				fillRepoCombo(null);
			}

			protected void refreshAll() {
				fillRepoCombo(null);
			}
		};
		ProvUI.addProvisioningListener(comboRepoListener);

	}

	void removeProvisioningListeners() {
		if (profileListener != null) {
			ProvUI.removeProvisioningListener(profileListener);
			profileListener = null;
		}
		if (comboRepoListener != null) {
			ProvUI.removeProvisioningListener(comboRepoListener);
			comboRepoListener = null;
		}

	}

	void setRepoComboDecoration(IStatus status) {
		if (status == null || status.isOK() || status.getSeverity() == IStatus.CANCEL) {
			repoDec.setShowOnlyOnFocus(true);
			repoDec.setDescriptionText(ProvUIMessages.AvailableIUsPage_RepoFilterInstructions);
			repoDec.setImage(info);
			// We may have been previously showing an error or warning
			// hover.  We will need to dismiss it, but if there is no text
			// typed, don't do this, so that the user gets the info cue
			if (repoCombo.getText().length() > 0)
				repoDec.showHoverText(null);
			return;
		}
		Image image;
		if (status.getSeverity() == IStatus.WARNING)
			image = warning;
		else if (status.getSeverity() == IStatus.ERROR)
			image = error;
		else
			image = info;
		repoDec.setImage(image);
		repoDec.setDescriptionText(status.getMessage());
		repoDec.setShowOnlyOnFocus(false);
		repoDec.showHoverText(status.getMessage());
	}

	protected String getClipboardText(Control control) {
		// The default label provider constructor uses the default column config.
		// since we passed the default column config to the available iu group,
		// we know that this label provider matches the one used there.
		return CopyUtils.getIndentedClipboardText(getSelectedIUElements(), new IUDetailsLabelProvider());
	}

	public ProvisioningContext getProvisioningContext() {
		// If the user can't manipulate repos, always resolve against everything
		if (policy.getRepositoryManipulator() == null)
			return new ProvisioningContext();
		// Consult the checkbox to see if we should resolve against everything,
		// or use the combo to determine what to do.
		if (resolveAllCheckbox.getSelection())
			return new ProvisioningContext();
		int siteSel = getComboIndex(repoCombo.getText());
		if (siteSel < 0 || siteSel == INDEX_SITE_ALL || siteSel == INDEX_SITE_NONE)
			return new ProvisioningContext();
		URI[] locals = getLocalSites();
		// If there are local sites, the last item in the combo is "Local Sites Only"
		// Use all local sites in this case
		// We have to set metadata repositories and artifact repositories in the
		// provisioning context because the artifact repositories are used for
		// sizing.
		if (locals.length > 0 && siteSel == repoCombo.getItemCount() - 1) {
			ProvisioningContext context = new ProvisioningContext(locals);
			context.setArtifactRepositories(locals);
			return context;
		}
		// A single site is selected.
		ProvisioningContext context = new ProvisioningContext(new URI[] {comboRepos[siteSel]});
		context.setArtifactRepositories(new URI[] {comboRepos[siteSel]});
		return context;
	}
}
