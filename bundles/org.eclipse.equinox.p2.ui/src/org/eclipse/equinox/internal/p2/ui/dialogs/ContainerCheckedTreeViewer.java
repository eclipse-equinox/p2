/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;

/**
 * Copy of ContainerCheckedTreeViewer which is specialized for use
 * with DeferredFetchFilteredTree.  Originally copied from
 * org.eclipse.ui.dialogs and altered to achieve the following:
 * 
 * (1)checking a parent will expand it when we know it's a long
 * running operation that involves a placeholder.
*  The modified method is doCheckStateChanged().
*  
 * (2)when preserving selection, we do not want the check state
 * to be rippled through the child and parent nodes.
 * Since we know that preservingSelection(Runnable) isn't working
 * properly, no need to do a bunch of work here.
 * The added methods is preservingSelection(Runnable).
 * Modified methods are updateChildrenItems(TreeItem parent) and
 * updateParentItems(TreeItem parent).
 * 
 * (3)we correct the problem with preservingSelection(Runnable) by
 * remembering the check state and restoring it after a refresh.  We
 * fire a check state event so clients monitoring the selection will know
 * what's going on.  Added methods are internalRefresh(Object, boolean), 
 * saveCheckedState(), restoreCheckedState(), and 
 * fireCheckStateChanged(Object, boolean).  That last method is public
 * so that DeferredFetchFilteredTree can do the same thing when it 
 * remembers selections.
 * 
 * This class does not correct the general problem reported in
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=170521
 * That is handled by preserving selections additively in 
 * DeferredFetchFilteredTree.  This class simply provides the API
 * needed by that class and manages the parent state according to the
 * children.
 */
public class ContainerCheckedTreeViewer extends CheckboxTreeViewer {

	private boolean rippleCheckMarks = true;
	private ArrayList<Object> savedCheckState;

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Composite)
	 */
	public ContainerCheckedTreeViewer(Composite parent) {
		super(parent);
		initViewer();
	}

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Composite,int)
	 */
	public ContainerCheckedTreeViewer(Composite parent, int style) {
		super(parent, style);
		initViewer();
	}

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Tree)
	 */
	public ContainerCheckedTreeViewer(Tree tree) {
		super(tree);
		initViewer();
	}

	private void initViewer() {
		setUseHashlookup(true);
		addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				doCheckStateChanged(event.getElement());
			}
		});
		addTreeListener(new ITreeViewerListener() {
			public void treeCollapsed(TreeExpansionEvent event) {
			}

			public void treeExpanded(TreeExpansionEvent event) {
				Widget item = findItem(event.getElement());
				if (item instanceof TreeItem) {
					initializeItem((TreeItem) item);
				}
			}
		});
	}

	/**
	 * Update element after a checkstate change.
	 * @param element
	 */
	protected void doCheckStateChanged(Object element) {
		Widget item = findItem(element);
		if (item instanceof TreeItem) {
			TreeItem treeItem = (TreeItem) item;
			treeItem.setGrayed(false);
			// BEGIN MODIFICATION OF COPIED CLASS
			if (element instanceof QueriedElement && treeItem.getChecked()) {
				if (!((QueriedElement) element).hasQueryable()) {
					// We have checked an element that will take some time
					// to get its children.  Use this opportunity to auto-expand 
					// the tree so that the check mark is not misleading.  Don't
					// update the check state because it will just be a pending 
					// placeholder.
					expandToLevel(element, 1);
					return;
				}
			}
			// END MODIFICATION OF COPIED CLASS
			updateChildrenItems(treeItem);
			updateParentItems(treeItem.getParentItem());
		}
	}

	/**
	 * The item has expanded. Updates the checked state of its children. 
	 */
	private void initializeItem(TreeItem item) {
		if (item.getChecked() && !item.getGrayed()) {
			updateChildrenItems(item);
		}
	}

	/**
	 * Updates the check state of all created children
	 */
	// MODIFIED to ignore parent state when in the middle of a 
	// selection preserving refresh.  
	private void updateChildrenItems(TreeItem parent) {
		// We are in the middle of preserving selections, don't
		// update any children according to parent
		if (!rippleCheckMarks)
			return;
		Item[] children = getChildren(parent);
		boolean state = parent.getChecked();
		for (int i = 0; i < children.length; i++) {
			TreeItem curr = (TreeItem) children[i];
			if (curr.getData() != null && ((curr.getChecked() != state) || curr.getGrayed())) {
				curr.setChecked(state);
				curr.setGrayed(false);
				updateChildrenItems(curr);
			}
		}
	}

	/**
	 * Updates the check / gray state of all parent items
	 */
	private void updateParentItems(TreeItem item) {
		// We are in the middle of preserving selections, don't
		// update any parents according to children
		if (!rippleCheckMarks)
			return;
		if (item != null) {
			Item[] children = getChildren(item);
			boolean containsChecked = false;
			boolean containsUnchecked = false;
			for (int i = 0; i < children.length; i++) {
				TreeItem curr = (TreeItem) children[i];
				containsChecked |= curr.getChecked();
				containsUnchecked |= (!curr.getChecked() || curr.getGrayed());
			}
			item.setChecked(containsChecked);
			item.setGrayed(containsChecked && containsUnchecked);
			updateParentItems(item.getParentItem());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICheckable#setChecked(java.lang.Object, boolean)
	 */
	public boolean setChecked(Object element, boolean state) {
		if (super.setChecked(element, state)) {
			doCheckStateChanged(element);
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CheckboxTreeViewer#setCheckedElements(java.lang.Object[])
	 */
	public void setCheckedElements(Object[] elements) {
		super.setCheckedElements(elements);
		for (int i = 0; i < elements.length; i++) {
			doCheckStateChanged(elements[i]);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#setExpanded(org.eclipse.swt.widgets.Item, boolean)
	 */
	protected void setExpanded(Item item, boolean expand) {
		super.setExpanded(item, expand);
		if (expand && item instanceof TreeItem) {
			initializeItem((TreeItem) item);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CheckboxTreeViewer#getCheckedElements()
	 */
	public Object[] getCheckedElements() {
		Object[] checked = super.getCheckedElements();
		// add all items that are children of a checked node but not created yet
		ArrayList<Object> result = new ArrayList<Object>();
		for (int i = 0; i < checked.length; i++) {
			Object curr = checked[i];
			result.add(curr);
			Widget item = findItem(curr);
			if (item != null) {
				Item[] children = getChildren(item);
				// check if contains the dummy node
				if (children.length == 1 && children[0].getData() == null) {
					// not yet created
					collectChildren(curr, result);
				}
			}
		}
		return result.toArray();
	}

	/**
	 * Recursively add the filtered children of element to the result.
	 * @param element
	 * @param result
	 */
	private void collectChildren(Object element, ArrayList<Object> result) {
		Object[] filteredChildren = getFilteredChildren(element);
		for (int i = 0; i < filteredChildren.length; i++) {
			Object curr = filteredChildren[i];
			result.add(curr);
			collectChildren(curr, result);
		}
	}

	// The super implementation doesn't really work because the
	// non-expanded items are not holding their real elements yet. 
	// Yet the code that records the checked state uses the 
	// elements to remember what checkmarks should be restored.
	// The result is that non-expanded elements are not up to date
	// and if anything in there should have been checked, it
	// won't be.  The best we can do is at least turn off all the
	// rippling checks that happen during this method since we are going
	// to reset all the checkmarks anyway.
	protected void preservingSelection(Runnable updateCode) {
		rippleCheckMarks = false;
		super.preservingSelection(updateCode);
		rippleCheckMarks = true;
	}

	protected void internalRefresh(Object element, boolean updateLabels) {
		saveCheckedState();
		super.internalRefresh(element, updateLabels);
		restoreCheckedState();
	}

	// We only remember the leaves.  This is specific to our
	// use case, not necessarily a good idea for fixing the general
	// problem.
	private void saveCheckedState() {
		Object[] checked = getCheckedElements();
		savedCheckState = new ArrayList<Object>(checked.length);
		for (int i = 0; i < checked.length; i++)
			if (!isExpandable(checked[i]) && !getGrayed(checked[i]))
				savedCheckState.add(checked[i]);
	}

	// Now we restore checked state.  
	private void restoreCheckedState() {
		setCheckedElements(new Object[0]);
		setGrayedElements(new Object[0]);
		Object element = null;
		// We are assuming that once a leaf, always a leaf.
		Iterator<Object> iter = savedCheckState.iterator();
		while (iter.hasNext()) {
			element = iter.next();
			setChecked(element, true);
		}
		// Listeners need to know something changed.
		if (element != null)
			fireCheckStateChanged(element, true);
	}

	// This method is public so that the DeferredFetchFilteredTree can also
	// call it.
	public void fireCheckStateChanged(Object element, boolean state) {
		fireCheckStateChanged(new CheckStateChangedEvent(this, element, state));
	}
}
