/*******************************************************************************
 *  Copyright (c) 2018 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.tasks.p2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.equinox.internal.p2.director.app.ILog;
import org.eclipse.tea.core.annotations.TaskCaptureStdOutput;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.jar.JarManager;
import org.eclipse.tea.library.build.jar.ZipExec;
import org.eclipse.tea.library.build.jar.ZipExecFactory;
import org.eclipse.tea.library.build.jar.ZipExecPart;
import org.eclipse.tea.library.build.model.FeatureBuild;
import org.eclipse.tea.library.build.model.PlatformTriple;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.p2.TeaProductDescription;
import org.eclipse.tea.library.build.p2.UpdateSite;
import org.eclipse.tea.library.build.p2.UpdateSiteManager;
import org.eclipse.tea.library.build.util.FileUtils;

/**
 * Task that will export a product from on an existing update site.
 */
@TaskCaptureStdOutput
@SuppressWarnings("restriction")
public class TaskRunProductExport {

	private final String siteName, productFeature, productFileName;
	private PlatformTriple[] buildPlatforms = PlatformTriple.getAllPlatforms();
	private final Map<PlatformTriple, File> outputs = new HashMap<>();
	private final boolean zip;

	/**
	 * Creates a new product by exporting it from a given update site
	 *
	 * @param siteName
	 *            the name of the update site
	 * @param productFeature
	 *            the id of the feature that contains the product file
	 * @param productFileName
	 *            the name of the product file (something like myApp.product)
	 * @param zip
	 *            Whether to ZIP the result, or leave as a directory
	 */
	public TaskRunProductExport(String siteName, String productFeature, String productFileName, boolean zip) {
		this.siteName = siteName;
		this.productFeature = productFeature;
		this.productFileName = productFileName;
		this.zip = zip;
	}

	@Override
	public String toString() {
		return "Export Product (" + productFeature + ')';
	}

	public void setPlatformsToBuild(PlatformTriple[] platforms) {
		buildPlatforms = platforms;
	}

	@Execute
	public void run(TaskingLog log, UpdateSiteManager um, JarManager jm, WorkspaceBuild wb) throws Exception {
		final UpdateSite site = um.getSite(siteName);
		final File baseProductDir = BuildDirectories.get().getProductDirectory();
		if (!site.directory.isDirectory()) {
			throw new RuntimeException("Repository '" + site.directory + "' is not existing");
		}
		final ILog logger = new TaskingLogLoggerDelegate(log);

		FeatureBuild feature = wb.getFeature(productFeature);
		File productFile = new File(feature.getData().getBundleDir(), productFileName);
		TeaProductDescription productDescriptor = new TeaProductDescription(productFile, feature);
		final String productName = productDescriptor.getProductName();
		final String productId = productDescriptor.getId();

		for (PlatformTriple platform : buildPlatforms) {
			final File productDir = new File(baseProductDir,
					productName + "-" + platform.os + "." + platform.ws + "." + platform.arch);
			final String buildVersion = jm.getQualifier();
			final File archivedProductFile = new File(baseProductDir, productName + "-" + buildVersion + "."
					+ platform.os + "." + platform.ws + "." + platform.arch + ".zip");

			// cleanup any old artifacts
			FileUtils.deleteDirectory(productDir);

			// export product for the given platform
			log.info("Building product '" + productDir.getName() + "'");
			createProduct(productId, site.directory, new File(productDir, productName), platform, logger);

			if (zip) {
				log.info("Archiving product '" + archivedProductFile.getName() + "'");
				createArchive(jm.getZipExecFactory(), productDir, archivedProductFile);

				// cleanup unpacked version again
				FileUtils.deleteDirectory(productDir);

				outputs.put(platform, archivedProductFile);
			} else {
				outputs.put(platform, productDir);
			}
		}
	}

	public File getOutput(PlatformTriple platform) {
		return outputs.get(platform);
	}

	public String getProductFeatureName() {
		return productFeature;
	}

	/**
	 * Runs the director application to create the product for the given
	 * platform
	 */
	protected void createProduct(String productId, File repositoryDir, File destinationDir, PlatformTriple platform,
			ILog logger) throws Exception {

		// arguments for DirectorApplication
		Collection<String> cmdArgs = new ArrayList<>();
		cmdArgs.add("-installIU");
		cmdArgs.add(productId);
		cmdArgs.add("-repository");
		cmdArgs.add("file:" + repositoryDir.getAbsolutePath());
		cmdArgs.add("-destination");
		cmdArgs.add(destinationDir.getAbsolutePath());
		cmdArgs.add("-profile");
		cmdArgs.add("WAMASProfile");
		cmdArgs.add("-roaming");
		cmdArgs.add("-profileProperties");
		cmdArgs.add("org.eclipse.update.install.features=true");
		cmdArgs.add("-p2.os");
		cmdArgs.add(platform.os);
		cmdArgs.add("-p2.ws");
		cmdArgs.add(platform.ws);
		cmdArgs.add("-p2.arch");
		cmdArgs.add(platform.arch);

		DirectorApplication directorApplication = new DirectorApplication();
		directorApplication.setLog(logger);
		Object result = directorApplication.run(cmdArgs.toArray(new String[cmdArgs.size()]));
		if (!IApplication.EXIT_OK.equals(result)) {
			throw new RuntimeException("Error occured during product export '" + result + "'");
		}
	}

	/** Creates an archive containing all files of the generated product */
	protected void createArchive(ZipExecFactory zip, File sourceDir, File archiveFile) throws Exception {
		final ZipExec zipExec = zip.createZipExec();
		zipExec.setZipFile(archiveFile);

		final ZipExecPart part = new ZipExecPart();
		for (File file : sourceDir.listFiles()) {
			part.sourceDirectory = sourceDir;
			part.relativePaths.add(file.getName());
		}
		zipExec.addPart(part);
		zipExec.createZip();
	}

}
