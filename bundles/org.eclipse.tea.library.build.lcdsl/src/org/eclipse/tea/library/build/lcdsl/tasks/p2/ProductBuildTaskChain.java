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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tea.core.TaskExecutionContext;
import org.eclipse.tea.core.annotations.TaskChainContextInit;
import org.eclipse.tea.core.annotations.TaskChainMenuEntry;
import org.eclipse.tea.core.services.TaskChain;
import org.eclipse.tea.core.services.TaskChain.TaskChainId;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.core.ui.annotations.TaskChainUiInit;
import org.eclipse.tea.library.build.config.BuildDirectories;
import org.eclipse.tea.library.build.config.TeaBuildConfig;
import org.eclipse.tea.library.build.menu.BuildLibraryMenu;
import org.eclipse.tea.library.build.model.PlatformTriple;
import org.eclipse.tea.library.build.tasks.jar.TaskInitJarCache;
import org.osgi.service.component.annotations.Component;

import com.google.common.base.Splitter;

/**
 * A task chain which can be used to provide the generation of a product as menu
 * entry in the IDE.
 */
@TaskChainId(description = "Export Products...", alias = "ProductBuildTaskChain")
@TaskChainMenuEntry(icon = "icons/product_xml_obj.png", path = BuildLibraryMenu.MENU_BUILD, groupingId = BuildLibraryMenu.GROUP_MISC)
@Component
public class ProductBuildTaskChain implements TaskChain {

	private static final String DUMMY_SITE = "dummy-site";

	protected List<AbstractProductBuild> builds = new ArrayList<>();

	public ProductBuildTaskChain() {
	}

	public ProductBuildTaskChain(AbstractProductBuild... build) {
		builds.addAll(Arrays.asList(build));
	}

	@TaskChainUiInit
	public void selectProducts(Shell parent, DynamicProductBuildRegistry reg) {
		builds.clear();

		ProductSelectionDialog dlg = new ProductSelectionDialog(parent, reg);
		if (dlg.open() != ProductSelectionDialog.OK) {
			throw new OperationCanceledException();
		}

		builds.addAll(dlg.getBuilds());

		if (builds.isEmpty()) {
			throw new OperationCanceledException();
		}
	}

	@TaskChainContextInit
	public void init(TaskExecutionContext c, TaskingLog log, TeaBuildConfig cfg, DynamicProductBuildRegistry registry,
			BuildDirectories dirs) {
		TaskInitJarCache cache = new TaskInitJarCache(dirs.getNewCacheDirectory("jar"));
		c.addTask(cache);
		if (builds.isEmpty()) {
			List<String> productList = cfg.productsToExport == null ? Collections.emptyList()
					: Splitter.on(',').splitToList(cfg.productsToExport);
			if (productList.isEmpty()) {
				log.error("Product list is empty nothing to build");
			}

			for (final String product : productList) {
				AbstractProductBuild productBuild = registry.findProductBuild(product);
				if (productBuild != null) {
					addProductBuildTasks(c, log, productBuild);
				} else {
					log.error("Cannot build product:" + product + " . Product not found.");
				}
			}
		} else {
			for (AbstractProductBuild build : builds) {
				addProductBuildTasks(c, log, build);
			}
		}
		c.addTask(cache.getCleanup());
	}

	private void addProductBuildTasks(TaskExecutionContext c, TaskingLog log, AbstractProductBuild productBuild) {
		productBuild.addUpdateSiteTasks(c, new String[] { DUMMY_SITE });
		productBuild.addProductTasks(c, DUMMY_SITE);
	}

	public static final class ProductSelectionDialog extends TitleAreaDialog {

		private CheckboxTableViewer tv;
		private List<AbstractProductBuild> selected;
		private final ProductBuildRegistry registry;

		public ProductSelectionDialog(Shell parentShell, ProductBuildRegistry registry) {
			super(parentShell);
			this.registry = registry;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 350);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

			getShell().setText("Product Export");
			setTitle("Select the products to export");
			setMessage("The selected products will be exported to zip files.", IMessageProvider.INFORMATION);

			tv = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getControl());

			tv.setContentProvider(new ArrayContentProvider());
			tv.setLabelProvider(new DelegatingStyledCellLabelProvider(new LblProvider()));
			List<AbstractProductBuild> input = new ArrayList<>(registry.getAllProducts());
			input.sort((a, b) -> a.getDescription().compareTo(b.getDescription()));
			tv.setInput(input);

			return parent;
		}

		@Override
		protected void okPressed() {
			selected = Arrays.stream(tv.getCheckedElements()).map(AbstractProductBuild.class::cast)
					.collect(Collectors.toList());

			super.okPressed();
		}

		public Collection<? extends AbstractProductBuild> getBuilds() {
			return selected;
		}

		private static final class LblProvider extends BaseLabelProvider implements IStyledLabelProvider {

			@Override
			public StyledString getStyledText(Object element) {
				AbstractProductBuild b = (AbstractProductBuild) element;
				StyledString result = new StyledString(b.getDescription());
				result.append(" - ", StyledString.DECORATIONS_STYLER);
				result.append(Arrays.stream(b.getPlatformsToBuild()).map(this::platformLabel)
						.collect(Collectors.joining(", ")), StyledString.DECORATIONS_STYLER);
				return result;
			}

			private String platformLabel(PlatformTriple triple) {
				if (triple == PlatformTriple.WIN32) {
					return "win32";
				} else if (triple == PlatformTriple.WIN64) {
					return "win64";
				} else if (triple == PlatformTriple.LINUX32) {
					return "linux32";
				} else if (triple == PlatformTriple.LINUX64) {
					return "linux64";
				} else {
					return "<unknown>";
				}
			}

			@Override
			public Image getImage(Object element) {
				return null;
			}

		}
	}

}
