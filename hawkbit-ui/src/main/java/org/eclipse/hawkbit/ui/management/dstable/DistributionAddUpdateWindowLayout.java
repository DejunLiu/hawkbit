/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.Collections;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.dstable.DistributionSetTable;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Sets;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * WindowContent for adding/editing a Distribution
 */
public class DistributionAddUpdateWindowLayout extends CustomComponent {

    private static final long serialVersionUID = -5602182034230568435L;

    private final I18N i18n;
    private final UINotification notificationMessage;
    private final transient EventBus.UIEventBus eventBus;
    private final transient DistributionSetManagement distributionSetManagement;
    private final transient SystemManagement systemManagement;
    private final transient EntityFactory entityFactory;

    private final DistributionSetTable distributionSetTable;

    private TextField distNameTextField;
    private TextField distVersionTextField;
    private TextArea descTextArea;
    private CheckBox reqMigStepCheckbox;
    private ComboBox distsetTypeNameComboBox;
    private boolean editDistribution;
    private Long editDistId;

    private FormLayout formLayout;

    public DistributionAddUpdateWindowLayout(final I18N i18n, final UINotification notificationMessage,
            final UIEventBus eventBus, final DistributionSetManagement distributionSetManagement,
            final SystemManagement systemManagement, final EntityFactory entityFactory,
            final DistributionSetTable distributionSetTable) {
        this.i18n = i18n;
        this.notificationMessage = notificationMessage;
        this.eventBus = eventBus;
        this.distributionSetManagement = distributionSetManagement;
        this.systemManagement = systemManagement;
        this.entityFactory = entityFactory;
        this.distributionSetTable = distributionSetTable;
        createRequiredComponents();
        buildLayout();
    }

    /**
     * Save or update distribution set.
     *
     */
    private final class SaveOnCloseDialogListener implements SaveDialogCloseListener {
        @Override
        public void saveOrUpdate() {
            if (editDistribution) {
                updateDistribution();
                return;
            }
            addNewDistribution();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return !isDuplicate();
        }

    }

    private void buildLayout() {
        addStyleName("lay-color");
        setSizeUndefined();

        formLayout = new FormLayout();
        formLayout.addComponent(distsetTypeNameComboBox);
        formLayout.addComponent(distNameTextField);
        formLayout.addComponent(distVersionTextField);
        formLayout.addComponent(descTextArea);
        formLayout.addComponent(reqMigStepCheckbox);

        setCompositionRoot(formLayout);
        distNameTextField.focus();
    }

    /**
     * Create required UI components.
     */
    private void createRequiredComponents() {
        distNameTextField = createTextField("textfield.name", UIComponentIdProvider.DIST_ADD_NAME);
        distVersionTextField = createTextField("textfield.version", UIComponentIdProvider.DIST_ADD_VERSION);

        distsetTypeNameComboBox = SPUIComponentProvider.getComboBox(i18n.get("label.combobox.type"), "", null, "",
                false, "", i18n.get("label.combobox.type"));
        distsetTypeNameComboBox.setImmediate(true);
        distsetTypeNameComboBox.setNullSelectionAllowed(false);
        distsetTypeNameComboBox.setId(UIComponentIdProvider.DIST_ADD_DISTSETTYPE);

        descTextArea = new TextAreaBuilder().caption(i18n.get("textfield.description")).style("text-area-style")
                .prompt(i18n.get("textfield.description")).immediate(true).id(UIComponentIdProvider.DIST_ADD_DESC)
                .buildTextComponent();
        descTextArea.setNullRepresentation("");

        reqMigStepCheckbox = SPUIComponentProvider.getCheckBox(i18n.get("checkbox.dist.required.migration.step"),
                "dist-checkbox-style", null, false, "");
        reqMigStepCheckbox.addStyleName(ValoTheme.CHECKBOX_SMALL);
        reqMigStepCheckbox.setId(UIComponentIdProvider.DIST_ADD_MIGRATION_CHECK);
    }

    private TextField createTextField(final String in18Key, final String id) {
        final TextField buildTextField = new TextFieldBuilder().caption(i18n.get(in18Key)).required(true)
                .prompt(i18n.get(in18Key)).immediate(true).id(id).buildTextComponent();
        buildTextField.setNullRepresentation("");
        return buildTextField;
    }

    /**
     * Get the LazyQueryContainer instance for DistributionSetTypes.
     *
     * @return
     */
    private LazyQueryContainer getDistSetTypeLazyQueryContainer() {
        final BeanQueryFactory<DistributionSetTypeBeanQuery> dtQF = new BeanQueryFactory<>(
                DistributionSetTypeBeanQuery.class);
        dtQF.setQueryConfiguration(Collections.emptyMap());

        final LazyQueryContainer disttypeContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.DIST_TYPE_SIZE, SPUILabelDefinitions.VAR_ID), dtQF);

        disttypeContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", true, true);

        return disttypeContainer;
    }

    private DistributionSetType getDefaultDistributionSetType() {
        final TenantMetaData tenantMetaData = systemManagement.getTenantMetadata();
        return tenantMetaData.getDefaultDsType();
    }

    /**
     * Update Distribution.
     */
    private void updateDistribution() {

        if (isDuplicate()) {
            return;
        }
        final boolean isMigStepReq = reqMigStepCheckbox.getValue();
        final Long distSetTypeId = (Long) distsetTypeNameComboBox.getValue();

        final DistributionSet currentDS = distributionSetManagement
                .updateDistributionSet(entityFactory.distributionSet().update(editDistId)
                        .name(distNameTextField.getValue()).description(descTextArea.getValue())
                        .version(distVersionTextField.getValue()).requiredMigrationStep(isMigStepReq)
                        .type(distributionSetManagement.findDistributionSetTypeById(distSetTypeId)));
        notificationMessage.displaySuccess(i18n.get("message.new.dist.save.success",
                new Object[] { currentDS.getName(), currentDS.getVersion() }));
        // update table row+details layout
        eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.UPDATED_ENTITY, currentDS));

    }

    /**
     * Add new Distribution set.
     */
    private void addNewDistribution() {
        editDistribution = Boolean.FALSE;

        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(distVersionTextField.getValue());
        final Long distSetTypeId = (Long) distsetTypeNameComboBox.getValue();
        final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final boolean isMigStepReq = reqMigStepCheckbox.getValue();

        final DistributionSet newDist = distributionSetManagement
                .createDistributionSet(entityFactory.distributionSet().create().name(name).version(version)
                        .description(desc).type(distributionSetManagement.findDistributionSetTypeById(distSetTypeId))
                        .requiredMigrationStep(isMigStepReq));

        notificationMessage.displaySuccess(
                i18n.get("message.new.dist.save.success", new Object[] { newDist.getName(), newDist.getVersion() }));

        distributionSetTable.setValue(
                Sets.newHashSet(new DistributionSetIdName(newDist.getId(), newDist.getName(), newDist.getVersion())));
    }

    private boolean isDuplicate() {
        final String name = distNameTextField.getValue();
        final String version = distVersionTextField.getValue();

        final DistributionSet existingDs = distributionSetManagement.findDistributionSetByNameAndVersion(name, version);
        /*
         * Distribution should not exists with the same name & version. Display
         * error message, when the "existingDs" is not null and it is add window
         * (or) when the "existingDs" is not null and it is edit window and the
         * distribution Id of the edit window is different then the "existingDs"
         */
        if (existingDs != null && !existingDs.getId().equals(editDistId)) {
            distNameTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
            distVersionTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
            notificationMessage.displayValidationError(
                    i18n.get("message.duplicate.dist", new Object[] { existingDs.getName(), existingDs.getVersion() }));

            return true;
        }

        return false;

    }

    /**
     * clear all the fields.
     */
    public void resetComponents() {
        editDistribution = Boolean.FALSE;
        distNameTextField.clear();
        distNameTextField.removeStyleName("v-textfield-error");
        distVersionTextField.clear();
        distVersionTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
        distsetTypeNameComboBox.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
        descTextArea.clear();
        reqMigStepCheckbox.clear();
    }

    private void populateValuesOfDistribution(final Long editDistId) {
        this.editDistId = editDistId;

        if (editDistId == null) {
            return;
        }

        final DistributionSet distSet = distributionSetManagement.findDistributionSetByIdWithDetails(editDistId);
        if (distSet == null) {
            return;
        }

        editDistribution = Boolean.TRUE;
        distNameTextField.setValue(distSet.getName());
        distVersionTextField.setValue(distSet.getVersion());
        if (distSet.getType().isDeleted()) {
            distsetTypeNameComboBox.addItem(distSet.getType().getId());
        }
        distsetTypeNameComboBox.setValue(distSet.getType().getId());

        reqMigStepCheckbox.setValue(distSet.isRequiredMigrationStep());
        if (distSet.getDescription() != null) {
            descTextArea.setValue(distSet.getDescription());
        }
    }

    /**
     * Returns the dialog window for the distributions.
     * 
     * @param editDistId
     * @return window
     */
    public CommonDialogWindow getWindow(final Long editDistId) {
        resetComponents();
        populateDistSetTypeNameCombo();
        populateValuesOfDistribution(editDistId);
        return new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).caption(i18n.get("caption.add.new.dist"))
                .content(this).layout(formLayout).i18n(i18n).saveDialogCloseListener(new SaveOnCloseDialogListener())
                .buildCommonDialogWindow();
    }

    /**
     * Populate DistributionSet Type name combo.
     */
    private void populateDistSetTypeNameCombo() {
        distsetTypeNameComboBox.setContainerDataSource(getDistSetTypeLazyQueryContainer());
        distsetTypeNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        distsetTypeNameComboBox.setValue(getDefaultDistributionSetType().getId());
    }

}
