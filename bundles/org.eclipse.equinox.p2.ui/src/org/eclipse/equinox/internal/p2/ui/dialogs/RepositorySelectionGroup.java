/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Yury Chernikov <Yury.Chernikov@borland.com> - Bug 271447 [ui] Bad layout in 'Install available software' dialog
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import com.ibm.icu.text.Collator;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A RepositorySelectionGroup is a reusable UI component that displays 
 * available repositories and allows the user to select them.  
 * 
 * @since 3.5
 */
public class RepositorySelectionGroup {

	private static final String SITE_NONE = ProvUIMessages.AvailableIUsPage_NoSites;
	private static final String SITE_ALL = ProvUIMessages.AvailableIUsPage_AllSites;
	private static final String SITE_LOCAL = ProvUIMessages.AvailableIUsPage_LocalSites;
	private static final int INDEX_SITE_NONE = 0;
	private static final int INDEX_SITE_ALL = 1;
	private static final int DEC_MARGIN_WIDTH = 2;
	private static final String LINKACTION = "linkAction"; //$NON-NLS-1$

	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=245569
	private static final int COUNT_VISIBLE_ITEMS = 20;
	IWizardContainer container;
	ProvisioningUI ui;
	IUViewQueryContext queryContext;

	ListenerList<IRepositorySelectionListener> listeners = new ListenerList<IRepositorySelectionListener>();

	Combo repoCombo;
	Link repoManipulatorLink;
	ControlDecoration repoDec;
	ComboAutoCompleteField repoAutoComplete;
	ProvUIProvisioningListener comboRepoListener;
	IRepositoryManipulationHook repositoryManipulationHook;

	Image info, warning, error;
	URI[] comboRepos; // the URIs shown in the combo, kept in sync with combo items
	HashMap<String, URI> disabledRepoProposals = new HashMap<String, URI>(); // proposal string -> disabled URI 

	public RepositorySelectionGroup(ProvisioningUI ui, IWizardContainer container, Composite parent, IUViewQueryContext queryContext) {
		this.container = container;
		this.queryContext = queryContext;
		this.ui = ui;
		createControl(parent);
	}

	public Control getDefaultFocusControl() {
		return repoCombo;
	}

	public void addRepositorySelectionListener(IRepositorySelectionListener listener) {
		listeners.add(listener);
	}

	protected void createControl(Composite parent) {
		final RepositoryTracker tracker = ui.getRepositoryTracker();
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
		comboComposite.setFont(parent.getFont());

		Label label = new Label(comboComposite, SWT.NONE);
		label.setText(ProvUIMessages.AvailableIUsPage_RepoFilterLabel);
		label.setFont(comboComposite.getFont());

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
		repoCombo.setVisibleItemCount(COUNT_VISIBLE_ITEMS);
		repoCombo.addKeyListener(new KeyAdapter() {

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
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

		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		// breathing room for info dec
		gd.horizontalIndent = DEC_MARGIN_WIDTH * 2;
		repoCombo.setLayoutData(gd);
		repoCombo.setFont(comboComposite.getFont());
		repoCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				URI location = null;
				IStatus status = null;
				String text = repoCombo.getText().trim();
				int index = getComboIndex(text);
				// only validate text that doesn't match existing text in combo
				if (index < 0) {
					location = tracker.locationFromString(repoCombo.getText());
					if (location == null) {
						status = tracker.getInvalidLocationStatus(repoCombo.getText());
					} else {
						status = tracker.validateRepositoryLocation(ui.getSession(), location, false, new NullProgressMonitor());
					}
				} else {
					// user typed or pasted an existing location.  Select it.
					repoComboSelectionChanged();
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
		setButtonLayoutData(button);
		button.setFont(comboComposite.getFont());

		// Link to repository manipulator
		repoManipulatorLink = createLink(comboComposite, new Action() {
			public void runWithEvent(Event event) {
				if (repositoryManipulationHook != null)
					repositoryManipulationHook.preManipulateRepositories();
				ui.manipulateRepositories(repoCombo.getShell());
				if (repositoryManipulationHook != null)
					repositoryManipulationHook.postManipulateRepositories();
			}
		}, getLinkLabel());
		gd = new GridData(SWT.END, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		repoManipulatorLink.setLayoutData(gd);
		repoManipulatorLink.setFont(comboComposite.getFont());

		addComboProvisioningListeners();
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				removeProvisioningListeners();
			}

		});
	}

	private String getLinkLabel() {
		if (ui.getPolicy().getRepositoryPreferencePageId() != null) {
			String pageName = ui.getPolicy().getRepositoryPreferencePageName();
			if (pageName == null)
				pageName = ProvUIMessages.RepositorySelectionGroup_PrefPageName;
			return NLS.bind(ProvUIMessages.RepositorySelectionGroup_PrefPageLink, pageName);
		}
		return ProvUIMessages.RepositorySelectionGroup_GenericSiteLinkTitle;
	}

	private void setButtonLayoutData(Button button) {
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		GC gc = new GC(button);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fm = gc.getFontMetrics();
		gc.dispose();
		int widthHint = Dialog.convertHorizontalDLUsToPixels(fm, IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
	}

	public void setRepositorySelection(int scope, URI location) {
		switch (scope) {
			case AvailableIUGroup.AVAILABLE_ALL :
				fillRepoCombo(SITE_ALL);
				break;
			case AvailableIUGroup.AVAILABLE_LOCAL :
				fillRepoCombo(SITE_LOCAL);
				break;
			case AvailableIUGroup.AVAILABLE_SPECIFIED :
				fillRepoCombo(getSiteString(location));
				break;
			default :
				fillRepoCombo(SITE_NONE);
		}
		setRepoComboDecoration(null);
	}

	public void setRepositoryManipulationHook(IRepositoryManipulationHook hook) {
		this.repositoryManipulationHook = hook;
	}

	protected void setRepoComboDecoration(final IStatus status) {
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
		// use a delay to show the validation method because the very next
		// selection or keystroke might fix it
		repoCombo.getDisplay().timerExec(500, new Runnable() {
			public void run() {
				if (repoDec != null && repoDec.getImage() != info)
					repoDec.showHoverText(status.getMessage());
			}
		});

	}

	/*
	 * Fill the repo combo and use the specified string
	 * as the selection.  If the selection is null, then the
	 * current selection should be preserved if applicable.
	 */
	void fillRepoCombo(final String selection) {
		RepositoryTracker tracker = ui.getRepositoryTracker();
		URI[] sites = tracker.getKnownRepositories(ui.getSession());
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
				String repoToSelect = selection;
				if (repoToSelect == null) {
					// If the combo is open and something is selected, use that index if we
					// weren't given a string to select.
					int selIndex = repoCombo.getSelectionIndex();
					if (selIndex >= 0)
						repoToSelect = repoCombo.getItem(selIndex);
					else
						repoToSelect = repoCombo.getText();
				}
				repoCombo.setItems(items);
				repoAutoComplete.setProposalStrings(getComboProposals());
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
		if (Display.getCurrent() == null)
			repoCombo.getDisplay().asyncExec(runnable);
		else
			runnable.run();
	}

	String getSiteString(URI uri) {
		String nickname = getMetadataRepositoryManager().getRepositoryProperty(uri, IRepository.PROP_NICKNAME);
		if (nickname != null && nickname.length() > 0)
			return NLS.bind(ProvUIMessages.AvailableIUsPage_NameWithLocation, new Object[] {nickname, ProvUIMessages.RepositorySelectionGroup_NameAndLocationSeparator, URIUtil.toUnencodedString(uri)});
		return URIUtil.toUnencodedString(uri);
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

	private void sortRepoItems(String[] strings, URI[] locations, boolean hasLocalSites) {
		int sortStart = 2;
		int sortEnd = hasLocalSites ? strings.length - 2 : strings.length - 1;
		if (sortStart >= sortEnd)
			return;
		final HashMap<URI, String> uriToString = new HashMap<URI, String>();
		for (int i = sortStart; i <= sortEnd; i++) {
			uriToString.put(locations[i], strings[i]);
		}
		final Collator collator = Collator.getInstance(Locale.getDefault());
		Comparator<String> stringComparator = new Comparator<String>() {
			public int compare(String a, String b) {
				return collator.compare(a, b);
			}
		};
		Comparator<URI> uriComparator = new Comparator<URI>() {
			public int compare(URI a, URI b) {
				return collator.compare(uriToString.get(a), uriToString.get(b));
			}
		};

		Arrays.sort(strings, sortStart, sortEnd, stringComparator);
		Arrays.sort(locations, sortStart, sortEnd, uriComparator);
	}

	private URI[] getLocalSites() {
		// use our current visibility flags plus the local filter
		int flags = ui.getRepositoryTracker().getMetadataRepositoryFlags() | IRepositoryManager.REPOSITORIES_LOCAL;
		return getMetadataRepositoryManager().getKnownRepositories(flags);
	}

	String[] getComboProposals() {
		int flags = ui.getRepositoryTracker().getMetadataRepositoryFlags() | IRepositoryManager.REPOSITORIES_DISABLED;
		String[] items = repoCombo.getItems();
		// Clear any previously remembered disabled repos
		disabledRepoProposals = new HashMap<String, URI>();
		URI[] disabled = getMetadataRepositoryManager().getKnownRepositories(flags);
		String[] disabledItems = new String[disabled.length];
		for (int i = 0; i < disabledItems.length; i++) {
			disabledItems[i] = getSiteString(disabled[i]);
			disabledRepoProposals.put(disabledItems[i], disabled[i]);
		}
		String[] both = new String[items.length + disabledItems.length];
		System.arraycopy(items, 0, both, 0, items.length);
		System.arraycopy(disabledItems, 0, both, items.length, disabledItems.length);
		return both;
	}

	int getComboIndex(String repoText) {
		// Callers have typically done this already, but just in case
		repoText = repoText.trim();
		// First look for exact match to the combo string.
		// This includes the name, etc.
		if (repoText.length() > 0) {
			String[] items = repoCombo.getItems();
			for (int i = 0; i < items.length; i++)
				if (repoText.equals(items[i])) {
					return i;
				}
		}
		// Look for URI match - the user may have pasted or dragged 
		// in a location that matches one we already know about, even
		// if the text does not match completely.  (slashes, no name, etc.)
		try {
			URI location = URIUtil.fromString(repoText);
			for (int i = 0; i < comboRepos.length; i++)
				if (URIUtil.sameURI(location, comboRepos[i])) {
					return i;
				}
		} catch (URISyntaxException e) {
			// never mind
		}

		// Special case.  The user has typed a URI with a trailing slash.
		// Make a URI without the trailing slash and see if it matches
		// a location we know about.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=268580
		int length = repoText.length();
		if (length > 0 && repoText.charAt(length - 1) == '/') {
			return getComboIndex(repoText.substring(0, length - 1));
		}
		return -1;
	}

	void addComboProvisioningListeners() {
		// We need to monitor repository events so that we can adjust the repo combo.
		comboRepoListener = new ProvUIProvisioningListener(getClass().getName(), ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY, ui.getOperationRunner()) {
			protected void repositoryAdded(RepositoryEvent e) {
				fillRepoCombo(getSiteString(e.getRepositoryLocation()));
			}

			protected void repositoryRemoved(RepositoryEvent e) {
				fillRepoCombo(null);
			}

			protected void refreshAll() {
				fillRepoCombo(null);
			}
		};
		ProvUI.getProvisioningEventBus(ui.getSession()).addListener(comboRepoListener);
	}

	void removeProvisioningListeners() {
		if (comboRepoListener != null) {
			ProvUI.getProvisioningEventBus(ui.getSession()).removeListener(comboRepoListener);
			comboRepoListener = null;
		}

	}

	/*
	 *  Add a repository using the text in the combo or launch a dialog if the text
	 *  represents an already known repo.  
	 */
	void addRepository(boolean alwaysPrompt) {
		final RepositoryTracker manipulator = ui.getRepositoryTracker();
		final String selectedRepo = repoCombo.getText().trim();
		int selectionIndex = getComboIndex(selectedRepo);
		final boolean isNewText = selectionIndex < 0;
		// If we are adding something already in the combo, just
		// select that item.
		if (!alwaysPrompt && !isNewText && selectionIndex != repoCombo.getSelectionIndex()) {
			repoComboSelectionChanged();
		} else if (alwaysPrompt) {
			AddRepositoryDialog dialog = new AddRepositoryDialog(repoCombo.getShell(), ui) {

				protected String getInitialLocationText() {
					if (isNewText) {
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=293068
						// we need to ensure any embedded nickname is stripped out
						URI loc = manipulator.locationFromString(selectedRepo);
						return loc.toString();
					}
					return super.getInitialLocationText();
				}

				@Override
				protected String getInitialNameText() {
					if (isNewText) {
						URI loc = manipulator.locationFromString(selectedRepo);
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=293068
						if (loc != null && manipulator instanceof ColocatedRepositoryTracker) {
							String parsedNickname = ((ColocatedRepositoryTracker) manipulator).getParsedNickname(loc);
							if (parsedNickname != null)
								return parsedNickname;
						}
					}
					return super.getInitialNameText();
				}

			};
			dialog.setTitle(ProvUIMessages.AddRepositoryDialog_Title);
			dialog.open();
			URI location = dialog.getAddedLocation();
			if (location != null)
				fillRepoCombo(getSiteString(location));
		} else if (isNewText) {
			try {
				container.run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						URI location;
						IStatus status;
						// This might be a disabled repo.  If so, no need to validate further.
						if (disabledRepoProposals.containsKey(selectedRepo)) {
							location = disabledRepoProposals.get(selectedRepo);
							status = Status.OK_STATUS;
						} else {
							location = manipulator.locationFromString(selectedRepo);
							if (location == null)
								status = manipulator.getInvalidLocationStatus(selectedRepo);
							else {
								status = manipulator.validateRepositoryLocation(ui.getSession(), location, false, monitor);
							}
						}
						if (status.isOK() && location != null) {
							String nick = null;
							if (manipulator instanceof ColocatedRepositoryTracker)
								nick = ((ColocatedRepositoryTracker) manipulator).getParsedNickname(location);
							manipulator.addRepository(location, nick, ui.getSession());
							fillRepoCombo(getSiteString(location));
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

	public ProvisioningContext getProvisioningContext() {
		int siteSel = getComboIndex(repoCombo.getText().trim());
		if (siteSel < 0 || siteSel == INDEX_SITE_ALL || siteSel == INDEX_SITE_NONE)
			return new ProvisioningContext(ui.getSession().getProvisioningAgent());
		URI[] locals = getLocalSites();
		// If there are local sites, the last item in the combo is "Local Sites Only"
		// Use all local sites in this case
		// We have to set metadata repositories and artifact repositories in the
		// provisioning context because the artifact repositories are used for
		// sizing.
		if (locals.length > 0 && siteSel == repoCombo.getItemCount() - 1) {
			ProvisioningContext context = new ProvisioningContext(ui.getSession().getProvisioningAgent());
			context.setMetadataRepositories(locals);
			context.setArtifactRepositories(locals);
			return context;
		}
		// A single site is selected.
		ProvisioningContext context = new ProvisioningContext(ui.getSession().getProvisioningAgent());
		context.setMetadataRepositories(new URI[] {comboRepos[siteSel]});
		context.setArtifactRepositories(new URI[] {comboRepos[siteSel]});
		return context;
	}

	void repoComboSelectionChanged() {
		int repoChoice = -1;
		URI repoLocation = null;

		int selection = -1;
		if (repoCombo.getListVisible()) {
			selection = repoCombo.getSelectionIndex();
		} else {
			selection = getComboIndex(repoCombo.getText().trim());
		}
		int localIndex = getLocalSites().length == 0 ? repoCombo.getItemCount() : repoCombo.getItemCount() - 1;
		if (comboRepos == null || selection < 0) {
			selection = INDEX_SITE_NONE;
		}

		if (selection == INDEX_SITE_NONE) {
			repoChoice = AvailableIUGroup.AVAILABLE_NONE;
		} else if (selection == INDEX_SITE_ALL) {
			repoChoice = AvailableIUGroup.AVAILABLE_ALL;
		} else if (selection >= localIndex) {
			repoChoice = AvailableIUGroup.AVAILABLE_LOCAL;
		} else {
			repoChoice = AvailableIUGroup.AVAILABLE_SPECIFIED;
			repoLocation = comboRepos[selection];
		}

		for (IRepositorySelectionListener listener : listeners) {
			listener.repositorySelectionChanged(repoChoice, repoLocation);
		}
	}

	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(ui.getSession());
	}
}
