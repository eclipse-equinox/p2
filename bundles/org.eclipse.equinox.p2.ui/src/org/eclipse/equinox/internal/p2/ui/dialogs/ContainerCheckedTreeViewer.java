/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;

/**
 * CheckboxTreeViewer with special behaviour of the checked / gray state on 
 * container (non-leaf) nodes:
 * Copied from org.eclipse.ui.dialogs
 * Altered to achieve the following:
 * (1)checking a parent will also expand it when we know it's a long
 * running operation that involves a placeholder.
*  The modified method is doCheckStateChanged().
*  
 * (2)when preserving selection, we do not want the check state
 * of the parents to influence the check state of the children.
 * When children appear due to relaxed filtering,
 * we never want to assume they should also be selected.  There are
 * cases where this is not necessarily the right thing to do, but
 * there are more cases where it is wrong.
 * The added methods are preservingSelection(Runnable) and 
 * updateParentsUsingChildren(TreeItem).
 * Modified method is updateChildrenItems(TreeItem parent).

 * 
 * (3) API added to update parent selection according to children.
 * This is used after a filter refresh to ensure that parents are
 * up to date.  Added method is
 * updateParentSelectionsUsingChildren()
 */
public class ContainerCheckedTreeViewer extends CheckboxTreeViewer {

	private boolean rippleCheckMarks = true;

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
		ArrayList result = new ArrayList();
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
	private void collectChildren(Object element, ArrayList result) {
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
}
