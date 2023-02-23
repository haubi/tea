/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.p2;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.ConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.EquinoxLauncherCUAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

/**
 * Publishes a product definition to the update site.
 * <p>
 * When publishing a product together with other bundles and features to an
 * update site the original implementation adds dependencies against all bundles
 * to the product and so the product will fail to install. This version is taken
 * from buckminster in order to prevent this.
 * </p>
 * <p>
 * A configuration advice for the required bundles to startup equinox is added
 * to the publisher so that the configuration file is written correctly when
 * installing the product
 * </p>
 * <p>
 * The branding of the executables has troubles if the projects are not directly
 * inside of the workspace
 * </p>
 */
@SuppressWarnings("restriction")
public class TeaProductAction extends ProductAction implements IPDEBuildConstants {

	private final File platformExecutables;
	private final File overrideExecutables;

	/**
	 * Creates a new product publishing action
	 *
	 * @param product
	 *            the product to publish
	 * @param executables
	 *            the location of the feature containing the executables
	 */
	public TeaProductAction(IProductDescriptor product, File executables, File overrideExecutables) {
		super(null, product, "tooling", executables);

		this.platformExecutables = executables;
		this.overrideExecutables = overrideExecutables;
	}

	@Override
	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		final IPublisherResult innerResult = new PublisherResult();

		// we are creating a separate publisher information for the
		// product publishing as otherwise the resulting product
		// will have a dependency against every published bundle
		final PublisherInfo innerInfo = new PublisherInfo();
		innerInfo.setConfigurations(info.getConfigurations());
		innerInfo.setArtifactOptions(info.getArtifactOptions());
		innerInfo.setArtifactRepository(info.getArtifactRepository());
		innerInfo.setMetadataRepository(info.getMetadataRepository());
		innerInfo.setContextArtifactRepository(info.getContextArtifactRepository());
		innerInfo.setContextMetadataRepository(info.getContextMetadataRepository());

		// build up caches containing the required information indexed by the id
		final Map<String, BundleInfo> defaultBundles = getDefaultBundles();
		final Map<String, BundleInfo> productBundleInfos = getBundleInfos(product);

		// The publishing result for the product must see the following elements
		// * Launcher - Required to generate the correct configuration and
		// branding
		// * Default Bundles - Required in order to startup equinox
		// * Root Elements - The elements that make the product in order to
		// determine the version
		final ConfigData defaultStartInfos = new ConfigData(null, null, null, null);
		for (IInstallableUnit iu : results.getIUs(null, IPublisherResult.ROOT)) {
			final String symbolicId = iu.getId();

			// launchers must be added to the publisher so that the
			// ApplicationLauncherAction can
			// generate the correct configuration
			if (symbolicId.startsWith(EquinoxLauncherCUAction.ORG_ECLIPSE_EQUINOX_LAUNCHER)) {
				innerResult.addIU(iu, IPublisherResult.ROOT);
				continue;
			}

			// publish the default bundles
			final BundleInfo defaultBundle = defaultBundles.get(symbolicId);
			if (defaultBundle != null) {
				innerResult.addIU(iu, IPublisherResult.ROOT);
				defaultStartInfos.addBundle(defaultBundle);
			}

			// Check if we have a custom configuration for the given bundle
			final BundleInfo productBundle = productBundleInfos.get(symbolicId);
			if (productBundle != null) {
				innerResult.addIU(iu, IPublisherResult.ROOT);
			}
		}

		// add all root elements of the product so that
		// the correct version can be determined during the publishing
		for (IVersionedId feature : product.getFeatures()) {
			String featureId = feature.getId() + ".feature.group";
			IQuery<IInstallableUnit> iuQuery = QueryUtil.createIUQuery(featureId);
			Iterator<IInstallableUnit> itor = results.query(iuQuery, monitor).iterator();
			while (itor.hasNext()) {
				innerResult.addIU(itor.next(), IPublisherResult.ROOT);
			}
		}

		// add a configuration advice for the default bundles
		Assert.isLegal(defaultStartInfos.getBundles().length > 0, "Default bundles not found");
		for (String configSpec : innerInfo.getConfigurations()) {
			innerInfo.addAdvice(new ConfigAdvice(defaultStartInfos, configSpec));
		}

		// perform the publishing with the given configuration and return the
		// merged result
		final IStatus status = super.perform(innerInfo, innerResult, monitor);
		if (status.getSeverity() != IStatus.ERROR) {
			results.merge(innerResult, IPublisherResult.MERGE_MATCHING);
		}
		return status;
	}

	@Override
	protected IPublisherAction createApplicationExecutableAction(String[] configSpecs) {
		return new TeaApplicationLauncherAction(id, version, flavor, executableName, platformExecutables, configSpecs,
				overrideExecutables);
	}

	/**
	 * Returns a indexed map of bundles required to startup the equinox
	 * framework
	 */
	private static Map<String, BundleInfo> getDefaultBundles() {
		final Map<String, BundleInfo> bundles = new TreeMap<>();
		{
			String bundleId = BUNDLE_SIMPLE_CONFIGURATOR;
			bundles.put(bundleId, new BundleInfo(bundleId, null, null, 1, true));
		}
		{
			String bundleId = BUNDLE_EQUINOX_COMMON;
			bundles.put(bundleId, new BundleInfo(bundleId, null, null, 2, true));
		}
		{
			String bundleId = BUNDLE_OSGI;
			bundles.put(bundleId, new BundleInfo(bundleId, null, null, -1, true));
		}
		{
			String bundleId = BUNDLE_CORE_RUNTIME;
			bundles.put(bundleId, new BundleInfo(bundleId, null, null, 4, true));
		}
		{
			String bundleId = BUNDLE_DS;
			bundles.put(bundleId, new BundleInfo(bundleId, null, null, 2, true));
		}
		return bundles;
	}

	/** Returns a indexed map of bundles that have a custom configuration */
	private static Map<String, BundleInfo> getBundleInfos(IProductDescriptor product) {
		final Map<String, BundleInfo> bundles = new TreeMap<>();
		for (BundleInfo bundleInfo : product.getBundleInfos()) {
			bundles.put(bundleInfo.getSymbolicName(), bundleInfo);
		}
		return bundles;
	}

}
