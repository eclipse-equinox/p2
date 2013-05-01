/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.*;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.operations.Remedy;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class RemediationGroup {
	final int ALLOWPARTIALINSTALL_INDEX = 0;
	final int ALLOWDIFFERENTVERSION_INDEX = 1;
	final int ALLOWINSTALLEDUPDATE_INDEX = 2;
	final int ALLOWINSTALLEDREMOVAL_INDEX = 3;

	private RemediationOperation remediationOperation;
	Composite remediationComposite;
	private Button bestBeingInstalledRelaxedButton;
	private Button bestInstalledRelaxedButton;
	Button buildMyOwnSolution;
	final ArrayList<Button> checkboxes = new ArrayList<Button>();
	private Composite resultFoundComposite;
	private Composite resultComposite;
	private Composite resultNotFoundComposite;
	private Composite resultErrorComposite;
	private Remedy currentRemedy;

	private TreeViewer treeViewer;
	protected IUElementListRoot input;
	private StackLayout switchRemediationLayout;
	Group detailsControl;
	Text detailStatusText;
	Composite checkBoxesComposite;
	private IUDetailsGroup iuDetailsGroup;

	HashMap<String, String[]> CONSTRAINTS;
	private WizardPage containerPage;

	public RemediationGroup(WizardPage page) {
		CONSTRAINTS = new HashMap<String, String[]>();
		CONSTRAINTS.put(ProvUIMessages.RemediationPage_BeingInstalledSection, new String[] {ProvUIMessages.RemediationPage_BeingInstalledSection_AllowPartialInstall, ProvUIMessages.RemediationPage_BeingInstalledSection_AllowDifferentVersion});
		CONSTRAINTS.put(ProvUIMessages.RemediationPage_InstalledSection, new String[] {ProvUIMessages.RemediationPage_InstalledSection_AllowInstalledUpdate, ProvUIMessages.RemediationPage_InstalledSection_AllowInstalledRemoval});

		containerPage = page;
	}

	public Composite getComposite() {
		return remediationComposite;
	}

	public void createRemediationControl(Composite container) {
		remediationComposite = new Composite(container, SWT.NONE);
		remediationComposite.setLayout(new GridLayout());
		Listener solutionslistener;

		Label descriptionLabel = new Label(remediationComposite, SWT.NONE);
		descriptionLabel.setText(ProvUIMessages.RemediationPage_SubDescription);

		solutionslistener = new Listener() {
			public void handleEvent(Event e) {
				Button btn = (Button) e.widget;
				Remedy remedy = (btn.getData() == null ? null : (Remedy) btn.getData());
				checkboxes.get(ALLOWPARTIALINSTALL_INDEX).setSelection(remedy != null && remedy.getConfig().allowPartialInstall);
				checkboxes.get(ALLOWDIFFERENTVERSION_INDEX).setSelection(remedy != null && remedy.getConfig().allowDifferentVersion);
				checkboxes.get(ALLOWINSTALLEDUPDATE_INDEX).setSelection(remedy != null && remedy.getConfig().allowInstalledUpdate);
				checkboxes.get(ALLOWINSTALLEDREMOVAL_INDEX).setSelection(remedy != null && remedy.getConfig().allowInstalledRemoval);
				for (Iterator<Button> iterator = checkboxes.iterator(); iterator.hasNext();) {
					Button btn1 = iterator.next();
					btn1.setVisible(true);
				}
				if (btn == buildMyOwnSolution && btn.getSelection()) {
					checkBoxesComposite.setVisible(true);
					((GridData) checkBoxesComposite.getLayoutData()).exclude = false;
				} else {
					checkBoxesComposite.setVisible(false);
					((GridData) checkBoxesComposite.getLayoutData()).exclude = true;
				}
				currentRemedy = searchRemedyMatchingUserChoices();
				refreshResultComposite();
				remediationComposite.layout(false);
			}
		};

		bestBeingInstalledRelaxedButton = new Button(remediationComposite, SWT.RADIO);
		bestBeingInstalledRelaxedButton.setText(ProvUIMessages.RemediationPage_BestSolutionBeingInstalledRelaxed);
		bestBeingInstalledRelaxedButton.addListener(SWT.Selection, solutionslistener);

		bestInstalledRelaxedButton = new Button(remediationComposite, SWT.RADIO);
		bestInstalledRelaxedButton.setText(ProvUIMessages.RemediationPage_BestSolutionInstallationRelaxed);
		bestInstalledRelaxedButton.addListener(SWT.Selection, solutionslistener);

		buildMyOwnSolution = new Button(remediationComposite, SWT.RADIO);
		buildMyOwnSolution.setText(ProvUIMessages.RemediationPage_BestSolutionBuilt);
		buildMyOwnSolution.addListener(SWT.Selection, solutionslistener);

		Listener checkboxListener = new Listener() {
			public void handleEvent(Event e) {
				currentRemedy = searchRemedyMatchingUserChoices();
				refreshResultComposite();
			}
		};
		checkBoxesComposite = new Composite(remediationComposite, SWT.NONE);
		checkBoxesComposite.setLayout(new GridLayout(1, false));
		GridData data = new GridData();
		data.exclude = false;
		data.horizontalAlignment = SWT.FILL;
		checkBoxesComposite.setLayoutData(data);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 30;
		Iterator<String> iter = CONSTRAINTS.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			String[] values = CONSTRAINTS.get(key);
			for (String value : values) {
				Button checkBtn = new Button(checkBoxesComposite, SWT.CHECK);
				checkBtn.setText(value);
				checkBtn.setData(value);
				checkBtn.setLayoutData(gd);
				checkBtn.addListener(SWT.Selection, checkboxListener);
				checkboxes.add(checkBtn);
			}

		}

		resultComposite = new Composite(remediationComposite, SWT.NONE);
		// GridLayoutFactory.fillDefaults().numColumns(1).applyTo(resultComposite);
		switchRemediationLayout = new StackLayout();
		resultComposite.setLayout(switchRemediationLayout);
		GridData data1 = new GridData(GridData.FILL_BOTH);
		resultComposite.setLayoutData(data1);

		resultErrorComposite = new Composite(resultComposite, SWT.NONE);
		resultErrorComposite.setLayout(new GridLayout());

		resultNotFoundComposite = new Composite(resultComposite, SWT.NONE);
		resultNotFoundComposite.setLayout(new GridLayout());
		Label resultNotFoundLabel = new Label(resultNotFoundComposite, SWT.NONE);
		resultNotFoundLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		resultNotFoundLabel.setText(ProvUIMessages.RemediationPage_NoSolutionFound);

		resultFoundComposite = new Composite(resultComposite, SWT.NONE);
		resultFoundComposite.setLayout(new GridLayout());

		Group insideFoundComposite = new Group(resultFoundComposite, SWT.NONE);
		insideFoundComposite.setText("Solution details");
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		insideFoundComposite.setLayout(gridLayout);
		insideFoundComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		treeViewer = new TreeViewer(insideFoundComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_BOTH);
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(data);
		tree.setHeaderVisible(true);
		IUColumnConfig[] columns = new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_IdColumnTitle, IUColumnConfig.COLUMN_ID, ILayoutConstants.DEFAULT_COLUMN_WIDTH)};
		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(treeViewer.getTree(), SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(treeViewer.getTree()));
		}
		ProvElementContentProvider contentProvider = new ProvElementContentProvider();
		treeViewer.setContentProvider(contentProvider);
		IUDetailsLabelProvider labelProvider = new IUDetailsLabelProvider(null, columns, null);
		treeViewer.setLabelProvider(labelProvider); //	columnLayout.setColumnData(column.getColumn(), new ColumnWeightData(100, 100, true));

		iuDetailsGroup = new IUDetailsGroup(resultErrorComposite, treeViewer, 500, true);

	}

	public IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	private Remedy searchBestDefaultRemedy() {
		if (remediationOperation.bestSolutionChangingTheRequest() != null) {
			return remediationOperation.bestSolutionChangingTheRequest();
		}
		if (remediationOperation.bestSolutionChangingWhatIsInstalled() != null) {
			return remediationOperation.bestSolutionChangingWhatIsInstalled();
		}
		return remediationOperation.getRemedies().get(0);
	}

	public void update(RemediationOperation operation) {
		this.remediationOperation = operation;
		currentRemedy = searchBestDefaultRemedy();

		bestBeingInstalledRelaxedButton.setData(remediationOperation.bestSolutionChangingTheRequest());
		bestInstalledRelaxedButton.setData(remediationOperation.bestSolutionChangingWhatIsInstalled());

		bestBeingInstalledRelaxedButton.setEnabled(remediationOperation.bestSolutionChangingTheRequest() != null);
		bestInstalledRelaxedButton.setEnabled(remediationOperation.bestSolutionChangingWhatIsInstalled() != null);
		bestBeingInstalledRelaxedButton.setSelection(false);
		bestInstalledRelaxedButton.setSelection(false);
		buildMyOwnSolution.setSelection(false);

		if (currentRemedy == remediationOperation.bestSolutionChangingTheRequest()) {
			bestBeingInstalledRelaxedButton.setSelection(true);
			bestBeingInstalledRelaxedButton.notifyListeners(SWT.Selection, new Event());
		} else if (currentRemedy == remediationOperation.bestSolutionChangingWhatIsInstalled()) {
			bestInstalledRelaxedButton.setSelection(true);
			bestInstalledRelaxedButton.notifyListeners(SWT.Selection, new Event());
		} else {
			buildMyOwnSolution.setData(currentRemedy);
			buildMyOwnSolution.setSelection(true);
			buildMyOwnSolution.notifyListeners(SWT.Selection, new Event());
		}
	}

	private boolean isContraintOK(int btnIndex, boolean value) {
		return (checkboxes.get(btnIndex).getSelection() && value) || (!checkboxes.get(btnIndex).getSelection() && !value);
	}

	Remedy searchRemedyMatchingUserChoices() {
		List<Remedy> remedies = remediationOperation.getRemedies();
		for (Remedy remedy : remedies) {
			if (isContraintOK(ALLOWPARTIALINSTALL_INDEX, remedy.getConfig().allowPartialInstall) && isContraintOK(ALLOWDIFFERENTVERSION_INDEX, remedy.getConfig().allowDifferentVersion) && isContraintOK(ALLOWINSTALLEDUPDATE_INDEX, remedy.getConfig().allowInstalledUpdate) && isContraintOK(ALLOWINSTALLEDREMOVAL_INDEX, remedy.getConfig().allowInstalledRemoval)) {
				if (remedy.getRequest() != null) {
					return remedy;
				}
			}
		}
		return null;
	}

	void refreshResultComposite() {
		resultComposite.setVisible(true);
		if (!checkboxes.get(ALLOWPARTIALINSTALL_INDEX).getSelection() && !checkboxes.get(ALLOWDIFFERENTVERSION_INDEX).getSelection() && !checkboxes.get(ALLOWINSTALLEDUPDATE_INDEX).getSelection() && !checkboxes.get(ALLOWINSTALLEDREMOVAL_INDEX).getSelection()) {
			switchRemediationLayout.topControl = resultErrorComposite;
		} else {

			if (currentRemedy == null) {
				switchRemediationLayout.topControl = resultNotFoundComposite;
			} else {
				input = new IUElementListRoot();
				AvailableIUElement[] ius = ElementUtils.requestToElement(currentRemedy);
				if (ius.length == 0) {
					switchRemediationLayout.topControl = resultNotFoundComposite;
				} else {
					input.setChildren(ius);
					treeViewer.setInput(input);
					switchRemediationLayout.topControl = resultFoundComposite;
				}
			}
		}
		resultComposite.layout();
		containerPage.setPageComplete(currentRemedy != null);
	}

	public Remedy getCurrentRemedy() {
		return currentRemedy;
	}

	public String getMessage() {
		return ProvUIMessages.InstallRemediationPage_Description;
	}
}
