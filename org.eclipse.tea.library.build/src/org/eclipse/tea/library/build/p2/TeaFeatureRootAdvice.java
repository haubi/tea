/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.p2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.tea.library.build.model.FeatureBuild;

/**
 * Holds additional files that will be published together with the feature to
 * the update site.
 */
@SuppressWarnings("restriction")
public class TeaFeatureRootAdvice extends AbstractAdvice implements IFeatureRootAdvice {

	private final FeatureBuild feature;

	/** Holds information about the publishing session */
	private final IPublisherInfo publisherInfo;

	/**
	 * Maps the configuration (os.ws.arch) to the path computer that holds the
	 * files to publish
	 */
	private final Map<String, TeaRootFilePathComputer> mapping = new HashMap<>();

	public TeaFeatureRootAdvice(FeatureBuild feature, IPublisherInfo publisherInfo) {
		this.feature = feature;
		this.publisherInfo = publisherInfo;
	}

	@Override
	public TeaRootFilePathComputer getRootFileComputer(String configSpec) {
		TeaRootFilePathComputer computer = mapping.get(configSpec);
		if (computer == null) {
			computer = new TeaRootFilePathComputer(configSpec);
			mapping.put(configSpec, computer);
		}
		return computer;
	}

	@Override
	public FileSetDescriptor getDescriptor(String configSpec) {
		TeaRootFilePathComputer computer = getRootFileComputer(configSpec);
		return computer.getDescriptor();
	}

	@Override
	public String[] getConfigurations() {
		return mapping.keySet().toArray(new String[mapping.size()]);
	}

	@Override
	public boolean isApplicable(String config, boolean includeDefault, String id, Version version) {
		return (config == null || mapping.containsKey(config)) && (id == null || feature.getFeatureName().equals(id));
	}

	/**
	 * Adds the files described in the given mapping to this advice.
	 *
	 * @param configMap
	 *            the files and folders to copy indexed by the configuration
	 */
	public void addFiles(Map<String, Map<String, String>> configMap) throws Exception {
		for (Map.Entry<String, Map<String, String>> entry : configMap.entrySet()) {
			String config = entry.getKey();
			if (config.equals(Utils.ROOT_COMMON)) {
				// common files for all configurations must be added separately
				// to all supported configurations
				for (String publisherConfig : publisherInfo.getConfigurations()) {
					Map<String, String> entries = entry.getValue();
					addFilesForConfig(publisherConfig, entries);
				}
			} else {
				// root files are defined using os.ws.arch
				// but we need ws.os.arch
				String splitted[] = config.split("\\.");
				if (splitted.length != 3) {
					throw new RuntimeException(
							feature.getFeatureName() + " - Invalid root file configuration. Expecting os.ws.arch");
				}
				// add the platform specific files
				config = splitted[1] + "." + splitted[0] + "." + splitted[2];
				Map<String, String> entries = entry.getValue();
				addFilesForConfig(config, entries);
			}
		}
	}

	/** Adds the files for the given configuration */
	private void addFilesForConfig(String config, Map<String, String> rootMap) throws Exception {
		final TeaRootFilePathComputer pathComputer = getRootFileComputer(config);
		final Path featurePath = new Path(feature.getData().getBundleDir().getAbsolutePath());
		for (Map.Entry<String, String> entry : rootMap.entrySet()) {
			addFiles(pathComputer, featurePath.append(entry.getValue()), new Path(entry.getKey()));
		}
	}

	/** Recursively adds the files to the given path computer */
	private void addFiles(TeaRootFilePathComputer pathComputer, IPath sourcePath, IPath destPath) {
		final File sourceFile = sourcePath.toFile();
		if (!sourceFile.exists()) {
			throw new RuntimeException(feature.getFeatureName() + " - Cannot find root file '" + sourceFile + "'");
		}
		// we have to add each file separately so that the path computer knows
		// the files
		// and can return a suitable destination path

		// we do not add the directory entry itself
		// as the publisher would also expand the directory and add each
		// contained child
		if (sourceFile.isDirectory()) {
			for (File child : sourceFile.listFiles()) {
				IPath childDestPath = new Path(destPath.toOSString());
				IPath childSourcePath = sourcePath.append(child.getName());
				if (child.isDirectory()) {
					childDestPath = childDestPath.append(child.getName());
				}
				addFiles(pathComputer, childSourcePath, childDestPath);
			}
		} else {
			pathComputer.addRootfile(sourceFile, destPath);
		}
	}
}