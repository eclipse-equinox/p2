/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
		return (contents, position) -> {
			String[] items = getStringItems();
			if (contents.length() == 0 || items.length == 0)
				return new IContentProposal[0];
			StringMatcher matcher = new StringMatcher("*" + contents + "*", true, false); //$NON-NLS-1$ //$NON-NLS-2$
			ArrayList<String> matches = new ArrayList<>();
			for (int i1 = 0; i1 < items.length; i1++)
				if (matcher.match(items[i1]))
					matches.add(items[i1]);

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
			for (int i2 = 0; i2 < matches.size(); i2++) {
				final String proposal = matches.get(i2);
				proposals[i2] = new IContentProposal() {

					@Override
					public String getContent() {
						return proposal;
					}

					@Override
					public int getCursorPosition() {
						return proposal.length();
					}

					@Override
					public String getDescription() {
						return null;
					}

					@Override
					public String getLabel() {
						return null;
					}
				};
			}
			return proposals;
		};
	}
}