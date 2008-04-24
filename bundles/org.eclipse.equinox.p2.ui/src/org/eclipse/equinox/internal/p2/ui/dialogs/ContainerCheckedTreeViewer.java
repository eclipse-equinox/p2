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
 * Altered so that checking a parent will expand it when we know it's a long
 * running operation to get the children.  
 * 
 * The modified method is doCheckStateChanged()
 */
public class ContainerCheckedTreeViewer extends CheckboxTreeViewer {

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
	private void updateChildrenItems(TreeItem parent) {
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

}
