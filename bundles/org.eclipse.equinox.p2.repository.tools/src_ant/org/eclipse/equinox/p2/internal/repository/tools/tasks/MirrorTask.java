package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.List;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;

public class MirrorTask extends AbstractRepositoryTask {

	public MirrorTask() {
		application = new MirrorApplication();
	}

	public void execute() throws BuildException {
		try {
			prepareSourceRepos();
			application.initializeRepos(null);
			List ius = prepareIUs();
			if (ius == null || ius.size() == 0)
				throw new BuildException("Need to specify one or more IUs.");
			application.setSourceIUs(ius);
			IStatus result = application.run(null);
			if (result.matches(IStatus.ERROR))
				throw new ProvisionException(result);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while transforming repository.", e);
		}
	}

	public SlicingOption createSlicingOptions() {
		SlicingOption options = new SlicingOption();
		((MirrorApplication) application).setSlicingOptions(options.getOptions());
		return options;
	}
}
