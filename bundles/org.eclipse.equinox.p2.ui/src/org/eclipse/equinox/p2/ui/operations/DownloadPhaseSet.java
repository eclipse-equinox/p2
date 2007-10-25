/**
 * 
 */
package org.eclipse.equinox.p2.ui.operations;

import org.eclipse.equinox.p2.engine.Phase;
import org.eclipse.equinox.p2.engine.PhaseSet;
import org.eclipse.equinox.p2.engine.phases.Collect;

public class DownloadPhaseSet extends PhaseSet {
	public DownloadPhaseSet() {
		super(new Phase[] {new Collect(10)});
	}
}