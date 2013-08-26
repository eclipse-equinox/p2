/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.misc.StringMatcher;
import org.eclipse.jface.fieldassist.*;
import org.eclipse.swt.widgets.Combo;

/**
 * ComboAutoCompleteField is an auto complete field appropriate for
 * pattern matching the text in a combo to the contents of the combo.
 * If the proposals should include items outside of the combo, then
 * clients can set their own proposal strings.
 * 
 * @since 3.5
 */
public class ComboAutoCompleteField {

	ContentProposalAdapter adapter;
	Combo combo;
	String[] proposalStrings = null;

	public ComboAutoCompleteField(Combo c) {
		this.combo = c;
		adapter = new ContentProposalAdapter(combo, new ComboContentAdapter(), getProposalProvider(), null, null);
		adapter.setPropagateKeys(true);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	}

	public void setProposalStrings(String[] proposals) {
		proposalStrings = proposals;
	}

	String[] getStringItems() {
		if (proposalStrings == null)
			return combo.getItems();
		return proposalStrings;
	}

	IContentProposalProvider getProposalProvider() {
		return new IContentProposalProvider() {
			public IContentProposal[] getProposals(String contents, int position) {
				String[] items = getStringItems();
				if (contents.length() == 0 || items.length == 0)
					return new IContentProposal[0];
				StringMatcher matcher = new StringMatcher("*" + contents + "*", true, false); //$NON-NLS-1$ //$NON-NLS-2$
				ArrayList<String> matches = new ArrayList<String>();
				for (int i = 0; i < items.length; i++)
					if (matcher.match(items[i]))
						matches.add(items[i]);

				// We don't want to autoactivate if the only proposal exactly matches
				// what is in the combo.  This prevents the popup from
				// opening when the user is merely scrolling through the combo values or
				// has accepted a combo value.
				if (matches.size() == 1 && matches.get(0).equals(combo.getText()))
					return new IContentProposal[0];

				if (matches.isEmpty())
					return new IContentProposal[0];

				// Make the proposals
				IContentProposal[] proposals = new IContentProposal[matches.size()];
				for (int i = 0; i < matches.size(); i++) {
					final String proposal = matches.get(i);
					proposals[i] = new IContentProposal() {

						public String getContent() {
							return proposal;
						}

						public int getCursorPosition() {
							return proposal.length();
						}

						public String getDescription() {
							return null;
						}

						public String getLabel() {
							return null;
						}
					};
				}
				return proposals;
			}
		};
	}
}