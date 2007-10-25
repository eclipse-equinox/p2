/**
 * 
 */
package org.eclipse.equinox.p2.ui.operations;

import org.eclipse.equinox.p2.engine.Phase;
import org.eclipse.equinox.p2.engine.PhaseSet;
import org.eclipse.equinox.p2.engine.phases.*;

public class InstallAndConfigurePhaseSet extends PhaseSet {
	public InstallAndConfigurePhaseSet() {
		super(new Phase[] {new Unconfigure(10), new Uninstall(10), new Install(10), new Configure(10)});
	}
}