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
package org.eclipse.tea.core.ui.config;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.di.extensions.Service;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tea.core.TaskingInjectionHelper;
import org.eclipse.tea.core.internal.TaskingConfigurationStore;
import org.eclipse.tea.core.internal.config.TaskingDevelopmentConfig;
import org.eclipse.tea.core.services.TaskingConfigurationExtension;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfig;
import org.eclipse.tea.core.services.TaskingConfigurationExtension.TaskingConfigProperty;
import org.eclipse.tea.core.ui.Activator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class TaskingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Inject
	@Service
	private List<TaskingConfigurationExtension> extensions;

	@Inject
	private TaskingDevelopmentConfig config;

	private final List<Composite> allFieldEditorParents = new ArrayList<>();
	Image logoImage;

	public TaskingPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		noDefaultButton();

		setPreferenceStore(Activator.getInstance().getPreferenceStore());
		ContextInjectionFactory.inject(this,
				TaskingInjectionHelper.createConfiguredContext(new TaskingEclipsePreferenceStore()));
	}

	@Override
	public void dispose() {
		super.dispose();
		disposeLogoImage();
	}

	private void disposeLogoImage() {
		if (logoImage != null) {
			logoImage.dispose();
		}
	}

	@Override
	protected void createFieldEditors() {
		ScrolledComposite scrolled = new ScrolledComposite(getFieldEditorParent(), SWT.V_SCROLL | SWT.H_SCROLL);
		GridLayoutFactory.fillDefaults().applyTo(scrolled);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(scrolled);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		Composite configComp = new Composite(scrolled, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(configComp);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(configComp);
		scrolled.setContent(configComp);

		Label logo = new Label(getFieldEditorParent(), SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).applyTo(logo);
		disposeLogoImage();
		logoImage = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "resources/tea-full60r.png").createImage();
		logo.setImage(logoImage);

		List<TaskingConfigurationExtension> sortedExtensions = new ArrayList<>(extensions);
		sortedExtensions.sort((a, b) -> {
			return calculateName(a, a.getClass().getAnnotation(TaskingConfig.class))
					.compareTo(calculateName(b, b.getClass().getAnnotation(TaskingConfig.class)));
		});

		for (TaskingConfigurationExtension ext : sortedExtensions) {
			if (!config.showHeadlessConfig && isAllHeadless(ext)) {
				continue;
			}

			Section s = new Section(configComp, ExpandableComposite.TWISTIE);
			TaskingConfig cfg = ext.getClass().getAnnotation(TaskingConfig.class);
			String name = calculateName(ext, cfg);
			s.setText(name + ":");

			s.addExpansionListener(new ExpansionAdapter() {
				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					s.getParent().layout(true, true);
					scrolled.setMinSize(configComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}
			});

			GridDataFactory.fillDefaults().grab(true, false).applyTo(s);

			Composite wrapper = new Composite(s, SWT.NONE);
			GridLayoutFactory.swtDefaults().extendedMargins(15, 0, 0, 0).applyTo(wrapper);
			s.setClient(wrapper);
			s.setBackground(scrolled.getBackground());

			Composite c = new Composite(wrapper, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(c);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(c);

			allFieldEditorParents.add(c);
			List<FieldEditor> editorsPerSection = new ArrayList<>();

			for (Field f : ext.getClass().getDeclaredFields()) {
				TaskingConfigProperty config = f.getAnnotation(TaskingConfigProperty.class);
				if (config == null) {
					continue;
				}

				if (config.headlessOnly() && !this.config.showHeadlessConfig) {
					continue;
				}

				String propertyName = TaskingConfigurationStore.getPropertyName(f);
				if (f.getType().equals(String.class)) {
					StringFieldEditor editor = new StringFieldEditor(propertyName, config.description(), c);
					Text t = editor.getTextControl(c);
					GridData d = (GridData) t.getLayoutData();
					d.widthHint = 30; // prevent text fields with a lot of text
										// from exploding the layout
					add(editor, editorsPerSection);
				} else if (f.getType().equals(Integer.class) || f.getType().equals(int.class)) {
					add(new IntegerFieldEditor(propertyName, config.description(), c), editorsPerSection);
				} else if (f.getType().equals(Long.class) || f.getType().equals(long.class)) {
					// XXX IntegerFieldEditor does not support long
					add(new IntegerFieldEditor(propertyName, config.description(), c), editorsPerSection);
				} else if (f.getType().equals(Boolean.class) || f.getType().equals(boolean.class)) {
					add(new BooleanFieldEditor(propertyName, config.description(), c), editorsPerSection);
				} else if (Enum.class.isAssignableFrom(f.getType())) {
					List<?> enumConstants = Arrays.asList(f.getType().getEnumConstants());
					String[][] entryNamesAndValues = getEntryNamesAndValues(enumConstants);
					add(new ComboFieldEditor(propertyName, config.description(), entryNamesAndValues, c),
							editorsPerSection);
				}

			}

			Button defPerSection = new Button(wrapper, SWT.PUSH);
			defPerSection.setText(JFaceResources.getString("defaults"));
			Dialog.applyDialogFont(defPerSection);
			GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(defPerSection);
			defPerSection.addSelectionListener(widgetSelectedAdapter(e -> {
				for (FieldEditor editor : editorsPerSection) {
					editor.loadDefault();
				}
				checkState();
				updateApplyButton();
			}));
		}
		scrolled.setMinSize(configComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private String calculateName(TaskingConfigurationExtension ext, TaskingConfig cfg) {
		if (cfg == null) {
			return "Unnamed Configuration (" + ext + ")";
		} else {
			return cfg.description();
		}
	}

	private void add(FieldEditor editor, List<FieldEditor> perSection) {
		perSection.add(editor);
		addField(editor);
	}

	private String[][] getEntryNamesAndValues(List<?> enumConstants) {
		String[][] table = new String[enumConstants.size()][2];
		for (int i = 0; i < enumConstants.size(); i++) {
			table[i][0] = enumConstants.get(i).toString();
			table[i][1] = enumConstants.get(i).toString();
		}
		return table;
	}

	@Override
	protected void adjustGridLayout() {
		super.adjustGridLayout();
		int targetColumns = ((GridLayout) getFieldEditorParent().getLayout()).numColumns;

		((GridLayout) getFieldEditorParent().getLayout()).numColumns = 2;
		for (Composite c : allFieldEditorParents) {
			((GridLayout) c.getLayout()).numColumns = targetColumns;
		}
	}

	private boolean isAllHeadless(TaskingConfigurationExtension ext) {
		for (Field f : ext.getClass().getDeclaredFields()) {
			TaskingConfigProperty prop = f.getAnnotation(TaskingConfigProperty.class);
			if (prop == null || prop.headlessOnly()) {
				continue;
			}

			return false;
		}
		return true;
	}

}
