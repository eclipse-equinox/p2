package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.*;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class RemediationPage extends ResolutionStatusPage {
	private static final int ALLOWPARTIALINSTALL_INDEX = 0;
	private static final int ALLOWDIFFERENTVERSION_INDEX = 1;
	private static final int ALLOWINSTALLEDUPDATE_INDEX = 2;
	private static final int ALLOWINSTALLEDREMOVAL_INDEX = 3;
	private static final HashMap<String, String[]> CONSTRAINTS = new HashMap<String, String[]>() {
		{
			put(ProvUIMessages.RemediationPage_BeingInstalledSection, new String[] {ProvUIMessages.RemediationPage_BeingInstalledSection_AllowPartialInstall, ProvUIMessages.RemediationPage_BeingInstalledSection_AllowDifferentVersion});
			put(ProvUIMessages.RemediationPage_InstalledSection, new String[] {ProvUIMessages.RemediationPage_InstalledSection_AllowInstalledUpdate, ProvUIMessages.RemediationPage_InstalledSection_AllowInstalledRemoval});
		}
	};

	private Composite mainComposite;
	private Composite resultComposite;
	private Composite resultNotFoundComposite;
	private Composite resultErrorComposite;
	private StackLayout switchResultLayout;
	private Composite resultFoundComposite;
	protected IUElementListRoot input;
	private TreeViewer treeViewer;

	private IUDetailsGroup iuDetailsGroup;
	private Listener bestSolutionlistener;
	private Button bestBeingInstalledRelaxedButton;
	private Button bestInstalledRelaxedButton;
	private Button buildMyOwnSolution;
	private ArrayList<Button> checkboxes;

	protected RemediationPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot input, ProfileChangeOperation operation) {
		super("RemediationPage", ui, wizard); //$NON-NLS-1$
		if (wizard instanceof UpdateWizard) {
			setTitle(ProvUIMessages.UpdateRemediationPage_Title);
			setDescription(ProvUIMessages.UpdateRemediationPage_Description);
		} else {
			setTitle(ProvUIMessages.InstallRemediationPage_Title);
			setDescription(ProvUIMessages.InstallRemediationPage_Description);
		}
	}

	public void createControl(Composite parent) {
		mainComposite = new Composite(parent, SWT.NONE);
		checkboxes = new ArrayList<Button>();
		mainComposite.setLayout(new GridLayout());

		Label descriptionLabel = new Label(mainComposite, SWT.NONE);
		descriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		descriptionLabel.setText(ProvUIMessages.RemediationPage_SubDescription);

		bestSolutionlistener = new Listener() {
			public void handleEvent(Event e) {
				Button btn = (Button) e.widget;
				Remedy remedy = (btn.getData() == null ? null : (Remedy) btn.getData());
				checkboxes.get(ALLOWPARTIALINSTALL_INDEX).setSelection(remedy != null && remedy.getConfig().allowPartialInstall);
				checkboxes.get(ALLOWDIFFERENTVERSION_INDEX).setSelection(remedy != null && remedy.getConfig().allowDifferentVersion);
				checkboxes.get(ALLOWINSTALLEDUPDATE_INDEX).setSelection(remedy != null && remedy.getConfig().allowInstalledUpdate);
				checkboxes.get(ALLOWINSTALLEDREMOVAL_INDEX).setSelection(remedy != null && remedy.getConfig().allowInstalledRemoval);
				for (Iterator<Button> iterator = checkboxes.iterator(); iterator.hasNext();) {
					Button btn1 = iterator.next();
					btn1.setVisible(remedy == null);
				}
				refreshResultComposite();
			}
		};

		bestBeingInstalledRelaxedButton = new Button(mainComposite, SWT.RADIO);
		bestBeingInstalledRelaxedButton.setText(ProvUIMessages.RemediationPage_BestSolutionBeingInstalledRelaxed);
		bestBeingInstalledRelaxedButton.addListener(SWT.Selection, bestSolutionlistener);

		bestInstalledRelaxedButton = new Button(mainComposite, SWT.RADIO);
		bestInstalledRelaxedButton.setText(ProvUIMessages.RemediationPage_BestSolutionInstallationRelaxed);
		bestInstalledRelaxedButton.addListener(SWT.Selection, bestSolutionlistener);

		buildMyOwnSolution = new Button(mainComposite, SWT.RADIO);
		buildMyOwnSolution.setText(ProvUIMessages.RemediationPage_BestSolutionBuilt);
		buildMyOwnSolution.addListener(SWT.Selection, bestSolutionlistener);

		Listener relaxedConstraintlistener = new Listener() {
			public void handleEvent(Event e) {
				refreshResultComposite();
			}
		};

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 30;
		Iterator<String> iter = CONSTRAINTS.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			String[] values = CONSTRAINTS.get(key);
			for (int i = 0; i < values.length; i++) {
				Button checkBtn = new Button(mainComposite, SWT.CHECK);
				checkBtn.setText(values[i]);
				checkBtn.setData(values[i]);
				checkBtn.setLayoutData(gd);
				checkBtn.addListener(SWT.Selection, relaxedConstraintlistener);
				checkboxes.add(checkBtn);
			}

		}

		resultComposite = new Composite(mainComposite, SWT.NONE);
		switchResultLayout = new StackLayout();
		resultComposite.setLayout(switchResultLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		resultComposite.setLayoutData(data);

		resultErrorComposite = new Composite(resultComposite, SWT.NONE);
		resultErrorComposite.setLayout(new GridLayout());

		resultNotFoundComposite = new Composite(resultComposite, SWT.NONE);
		resultNotFoundComposite.setLayout(new GridLayout());
		Label resultNotFoundLabel = new Label(resultNotFoundComposite, SWT.NONE);
		resultNotFoundLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		resultNotFoundLabel.setText(ProvUIMessages.RemediationPage_NoSolutionFound);

		resultFoundComposite = new Composite(resultComposite, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		resultFoundComposite.setLayout(gridLayout);

		treeViewer = new TreeViewer(resultFoundComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(data);
		tree.setHeaderVisible(true);
		activateCopy(tree);
		IUColumnConfig[] columns = getColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(tree));
		}

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(getColumnConfig());
		treeViewer.setComparator(comparator);
		treeViewer.setComparer(new ProvElementComparer());

		ProvElementContentProvider contentProvider = new ProvElementContentProvider();
		treeViewer.setContentProvider(contentProvider);
		IUDetailsLabelProvider labelProvider = new IUDetailsLabelProvider(null, getColumnConfig(), getShell());
		treeViewer.setLabelProvider(labelProvider);
		resultComposite.setVisible(false);
		iuDetailsGroup = new IUDetailsGroup(resultErrorComposite, treeViewer, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH), true);

		setControl(mainComposite);
		setPageComplete(false);

		Dialog.applyDialogFont(mainComposite);

	}

	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	public void updateStatus(IUElementListRoot newRoot, ProfileChangeOperation operation, Object[] planSelections) {
		setDetailText(operation);
		checkboxes.get(ALLOWPARTIALINSTALL_INDEX).setEnabled(planSelections.length > 1);
		RemediationOperation remediationOperation = ((ProvisioningOperationWizard) getWizard()).getRemediationOperation();
		boolean isSelected = false;
		if (remediationOperation.bestSolutionChangingTheRequest() != null) {
			bestBeingInstalledRelaxedButton.setData(remediationOperation.bestSolutionChangingTheRequest());
			bestBeingInstalledRelaxedButton.setSelection(true);
			remediationOperation.setCurrentRemedy(remediationOperation.bestSolutionChangingTheRequest());
			bestBeingInstalledRelaxedButton.notifyListeners(SWT.Selection, new Event());
			isSelected = true;
		}
		bestBeingInstalledRelaxedButton.setEnabled(remediationOperation.bestSolutionChangingTheRequest() != null);

		if (remediationOperation.bestSolutionChangingWhatIsInstalled() != null) {
			bestInstalledRelaxedButton.setData(remediationOperation.bestSolutionChangingWhatIsInstalled());
			bestInstalledRelaxedButton.setSelection(isSelected == false);
			if (!isSelected) {
				remediationOperation.setCurrentRemedy(remediationOperation.bestSolutionChangingWhatIsInstalled());
				bestInstalledRelaxedButton.notifyListeners(SWT.Selection, new Event());
			}
			isSelected = true;
		}
		bestInstalledRelaxedButton.setEnabled(remediationOperation.bestSolutionChangingWhatIsInstalled() != null);
		buildMyOwnSolution.setSelection(isSelected == false);
		if (!isSelected) {
			remediationOperation.setCurrentRemedy(remediationOperation.getRemedies().get(0));
			buildMyOwnSolution.setData(remediationOperation.getRemedies().get(0));
			buildMyOwnSolution.notifyListeners(SWT.Selection, new Event());
		}
	}

	@Override
	protected void updateCaches(IUElementListRoot root, ProfileChangeOperation resolvedOperation) {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean isCreated() {
		return false;
	}

	@Override
	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	@Override
	protected IInstallableUnit getSelectedIU() {
		// Not applicable 
		return null;
	}

	@Override
	protected Object[] getSelectedElements() {
		return new Object[] {};//((ProvisioningOperationWizard) getWizard()).mainPage.getSelectedIUElements();
	}

	public ArrayList<AvailableIUElement> transformIUstoIUElements() {
		ArrayList<AvailableIUElement> temp = new ArrayList<AvailableIUElement>();
		ArrayList<String> updateIds = new ArrayList<String>();
		IUElementListRoot root = new IUElementListRoot();
		Remedy currentRemedy = ((ProvisioningOperationWizard) getWizard()).remediationOperation.getCurrentRemedy();
		for (IInstallableUnit addedIU : currentRemedy.getRequest().getAdditions()) {
			AvailableIUElement element = new AvailableIUElement(root, addedIU, getProfileId(), true);
			for (IInstallableUnit removedIU : currentRemedy.getRequest().getRemovals()) {
				if (removedIU.getId().equals(addedIU.getId())) {
					int addedComparedToRemoved = addedIU.getVersion().compareTo(removedIU.getVersion());
					element.setBeingDowngraded(addedComparedToRemoved < 0);
					element.setBeingUpgraded(addedComparedToRemoved > 0);
					updateIds.add(addedIU.getId());
					break;
				}
			}
			if (!updateIds.contains(addedIU.getId()))
				element.setBeingAdded(true);
			temp.add(element);
		}
		for (IInstallableUnit removedIU : currentRemedy.getRequest().getRemovals()) {
			if (!updateIds.contains(removedIU.getId())) {
				AvailableIUElement element = new AvailableIUElement(root, removedIU, getProfileId(), false);
				element.setBeingRemoved(true);
				temp.add(element);
			}
		}
		return temp;
	}

	private boolean isContraintOK(int btnIndex, boolean value) {
		return (checkboxes.get(btnIndex).getSelection() && value) || (!checkboxes.get(btnIndex).getSelection() && !value);
	}

	public void refreshResultComposite() {
		resultComposite.setVisible(true);
		Remedy currentRemedy = ((ProvisioningOperationWizard) getWizard()).remediationOperation.getCurrentRemedy();
		currentRemedy = null;
		if (!checkboxes.get(ALLOWPARTIALINSTALL_INDEX).getSelection() && !checkboxes.get(ALLOWDIFFERENTVERSION_INDEX).getSelection() && !checkboxes.get(ALLOWINSTALLEDUPDATE_INDEX).getSelection() && !checkboxes.get(ALLOWINSTALLEDREMOVAL_INDEX).getSelection()) {
			switchResultLayout.topControl = resultErrorComposite;
		} else {
			RemediationOperation remediationOperation = ((ProvisioningOperationWizard) getWizard()).getRemediationOperation();
			List<Remedy> remedies = remediationOperation.getRemedies();
			for (Iterator<Remedy> iterator = remedies.iterator(); iterator.hasNext();) {
				Remedy remedy = iterator.next();
				if (isContraintOK(ALLOWPARTIALINSTALL_INDEX, remedy.getConfig().allowPartialInstall) && isContraintOK(ALLOWDIFFERENTVERSION_INDEX, remedy.getConfig().allowDifferentVersion) && isContraintOK(ALLOWINSTALLEDUPDATE_INDEX, remedy.getConfig().allowInstalledUpdate) && isContraintOK(ALLOWINSTALLEDREMOVAL_INDEX, remedy.getConfig().allowInstalledRemoval)) {
					if (remedy.getRequest() != null) {
						currentRemedy = remedy;
						((ProvisioningOperationWizard) getWizard()).remediationOperation.setCurrentRemedy(currentRemedy);
						break;
					}
				}
			}
			if (currentRemedy == null) {
				switchResultLayout.topControl = resultNotFoundComposite;
			} else {
				input = new IUElementListRoot();
				ArrayList<AvailableIUElement> ius = new ArrayList<AvailableIUElement>();
				ius.addAll(transformIUstoIUElements());
				if (ius.size() == 0) {
					switchResultLayout.topControl = resultNotFoundComposite;
					currentRemedy = null;
				} else {
					input.setChildren(ius.toArray());
					treeViewer.setInput(input);
					switchResultLayout.topControl = resultFoundComposite;
				}
			}
		}
		setPageComplete(currentRemedy != null);
		resultComposite.layout();
	}

	@Override
	protected String getDialogSettingsName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SashForm getSashForm() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getColumnWidth(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected String getClipboardText(Control control) {
		// TODO Auto-generated method stub
		return null;
	}

	protected Collection<IInstallableUnit> getIUs() {
		return ElementUtils.elementsToIUs(input.getChildren(input));
	}
}