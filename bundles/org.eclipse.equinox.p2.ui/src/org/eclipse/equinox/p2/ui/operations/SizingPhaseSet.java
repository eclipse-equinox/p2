/**
 * 
 */
package org.eclipse.equinox.p2.ui.operations;

import org.eclipse.equinox.p2.engine.Phase;
import org.eclipse.equinox.p2.engine.PhaseSet;
import org.eclipse.equinox.p2.engine.phases.Sizing;

public class SizingPhaseSet extends PhaseSet {
	private static Sizing sizing;

	SizingPhaseSet() {
		super(new Phase[] {sizing = new Sizing(100, "Compute sizes")});
	}

	Sizing getSizing() {
		return sizing;
	}
}