package org.eclipse.equinox.internal.p2.ql;

final class RootVariableScope implements VariableScope {
	private static final RootVariableScope instance = new RootVariableScope();

	private RootVariableScope() {
		// To prevent singleton pattern breakage
	}

	public static VariableScope getInstance() {
		return instance;
	}

	public Object getValue(Variable var) {
		throw new IllegalArgumentException("No such variable: " + var.getName()); //$NON-NLS-1$
	}

	public void setValue(Variable var, Object val) {
		throw new IllegalArgumentException("No such variable: " + var.getName()); //$NON-NLS-1$
	}
}