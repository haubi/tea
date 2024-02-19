/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package org.eclipse.tea.library.build.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.model.PluginData;
import org.eclipse.tea.library.build.model.WorkspaceData;

/**
 * Updater to set missing default charset in Eclipse projects and PDE plugins,
 * using workspace default, if any, or UTF-8 otherways.
 *
 * Starting with Eclipse 2022-06 there is this warning:
 * <code>Default encoding ({0}) should be specified for library "{1}" to match workspace settings.</code>
 *
 * When setting the project's default charset, and there are source libraries,
 * build.properties requires the javacDefaultCharset property as well.
 */
@SuppressWarnings("restriction")
public class DefaultCharsetUpdater {
	private final WorkspaceData wsData;
	private final IProgressMonitor nullMonitor = new NullProgressMonitor();

	public DefaultCharsetUpdater(WorkspaceData wsData) {
		this.wsData = wsData;
	}

	public void update(TaskingLog log) {
		for (PluginData plugin : wsData.getPlugins()) {
			IProject project = plugin.getProject();
			try {
				String pluginCharset = project.getDefaultCharset(false);
				if (pluginCharset == null) {
					pluginCharset = project.getDefaultCharset(true);
					if (pluginCharset == null) {
						pluginCharset = "UTF-8";
					}
					project.setDefaultCharset(pluginCharset, nullMonitor);
				}
				setJavacDefaultEncodingInPDEBuildProperties(project, pluginCharset);
			} catch (CoreException e) {
				log.error("failed to update default charset for project " + project.getName(), e);
			}
		}
	}

	private void setJavacDefaultEncodingInPDEBuildProperties(IProject project, final String pluginCharset)
			throws CoreException {
		if (!isMissingJavacDefaultEncoding(project)) {
			return;
		}
		// PDEModelUtility.modifyModel works with open editors:
		// have seen deadlocks with main (=UI) thread
		Display.getDefault().syncExec(
				() -> PDEModelUtility.modifyModel(new ModelModification(PDEProject.getBuildProperties(project)) {

					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						IBuildModel buildModel = model.getAdapter(IBuildModel.class);
						if (!isMissingJavacDefaultEncoding(buildModel)) {
							return;
						}
						IBuildEntry buildProp = buildModel.getFactory().createEntry(propJavacDefaultEncodingForPlugin);
						buildProp.addToken(pluginCharset);
					}
				}, nullMonitor));
	}

	private boolean isMissingJavacDefaultEncoding(IProject project) {
		IPluginModelBase pluginModelBase = PluginRegistry.findModel(project);
		if (pluginModelBase == null) {
			return false;
		}
		try {
			IBuildModel buildModel = PluginRegistry.createBuildModel(pluginModelBase);
			return isMissingJavacDefaultEncoding(buildModel);
		} catch (CoreException e) {
			return false;
		}
	}

	private boolean isMissingJavacDefaultEncoding(IBuildModel buildModel) {
		if (buildModel == null) {
			return false;
		}
		IBuild build = buildModel.getBuild();
		if (!hasSourceLibs(build)) {
			return false; // not needed
		}
		IBuildEntry buildProp = build.getEntry(propJavacDefaultEncodingForPlugin);
		if (buildProp != null) {
			return false; // already set
		}
		return true;
	}

	private boolean hasSourceLibs(IBuild build) {
		if (build == null) {
			return false;
		}
		for (IBuildEntry entry : build.getBuildEntries()) {
			// org.eclipse.pde.internal.build.IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX
			if (entry.getName().startsWith("source.") && entry.getTokens().length > 0) {
				return true;
			}
		}
		return false;
	}

	private final static String propJavacDefaultEncodingForPlugin = propJavacDefaultEncodingFor(".");

	private final static String propJavacDefaultEncodingFor(String path) {
		// org.eclipse.pde.internal.build.IBuildPropertiesConstants.PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX
		return "javacDefaultEncoding." + path;
	}
}
