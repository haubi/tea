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
package org.eclipse.tea.library.build.lcdsl.tasks.p2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.tasks.jar.TaskInitJarCache;
import org.eclipse.tea.library.build.tasks.p2.UpdateSiteZipsTask;

/**
 * Registration for product build definitions; maps products to update sites.
 */
public class ProductBuildRegistry {

	/** all product build definitions */
	private final List<AbstractProductBuild> products = new ArrayList<>();

	/** map from update site to products */
	private final Map<String, List<AbstractProductBuild>> site2product = new TreeMap<>();

	/**
	 * Adds a product build definition. It is not allowed to register more than
	 * one instance of the same sub-class of {@linkplain AbstractProductBuild}.
	 *
	 * @param product
	 *            product build definition
	 * @param sites
	 *            list of all update sites which contains the product; at least
	 *            one site is required
	 */
	public void add(AbstractProductBuild product, String... sites) {
		add(product, Arrays.asList(sites));
	}

	protected void add(AbstractProductBuild product, Collection<String> sites) {
		if (sites.isEmpty()) {
			throw new IllegalArgumentException("cannot create a product without an update site");
		}

		for (String site : sites) {
			List<AbstractProductBuild> list = site2product.get(site);
			if (list == null) {
				list = new ArrayList<>();
				site2product.put(site, list);
			}
			list.add(product);
		}

		products.add(product);
	}

	/**
	 * Adds tasks required to build the compound update site for a given
	 * definition.
	 *
	 * @param c
	 *            the context to add to
	 * @param dirs
	 *            {@link BuildDirectories} used to calculate cache directory.
	 * @param siteName
	 *            the site to build, <code>null</code> for all products
	 * @param withZip
	 *            whether the site is only used in follow up tasks, or should be
	 *            archived to a ZIP file prior to deleting the temporary target
	 *            directory.
	 */
	public void addAllUpdateSiteTasks(TaskExecutionContext c, BuildDirectories dirs, String siteName, boolean withZip) {
		String[] sites = siteName == null ? new String[] { "all" } : new String[] { siteName };
		Collection<AbstractProductBuild> prods = siteName == null ? getAllProducts() : site2product.get(siteName);
		TaskInitJarCache cache = new TaskInitJarCache(dirs.getNewCacheDirectory("jar"));
		c.addTask(cache);
		for (AbstractProductBuild product : prods) {
			product.addUpdateSiteTasks(c, sites);
		}
		if (withZip) {
			c.addTask(UpdateSiteZipsTask.class);
		}
		c.addTask(cache.getCleanup());
	}

	/**
	 * Adds tasks required to build the according product to the given context.
	 * <p>
	 * Caller <b>must</b> make sure that
	 * {@link #addAllUpdateSiteTasks(TaskExecutionContext, BuildDirectories, String, boolean)}
	 * or similar is called for the given product before this method.
	 *
	 * @param c
	 *            the context to contribute to
	 * @param siteName
	 *            the definition to add product tasks for, <code>null</code> for
	 *            all products
	 */
	public void addAllProductTasks(TaskExecutionContext c, String siteName) {
		Collection<AbstractProductBuild> prods = siteName == null ? getAllProducts() : site2product.get(siteName);
		for (AbstractProductBuild product : prods) {
			product.addProductTasks(c, siteName);
		}
	}

	public AbstractProductBuild findProductBuild(String productName) {
		for (AbstractProductBuild product : products) {
			if (product.getOfficialName().equals(productName)) {
				return product;
			}
		}
		return null;
	}

	public Collection<AbstractProductBuild> getAllProducts() {
		return products;
	}

	public Collection<AbstractProductBuild> getSiteProducts(String siteName) {
		if (siteName == null) {
			return getAllProducts();
		}
		return site2product.get(siteName);
	}

	public Collection<String> getSites() {
		return site2product.keySet();
	}

}
