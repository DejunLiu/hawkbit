/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * JPA implementation of {@link LocalArtifact}.
 *
 */
@Table(name = "sp_artifact", indexes = { @Index(name = "sp_idx_artifact_01", columnList = "tenant,software_module"),
        @Index(name = "sp_idx_artifact_prim", columnList = "tenant,id") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaArtifact extends AbstractJpaTenantAwareBaseEntity implements Artifact {
    private static final long serialVersionUID = 1L;

    @Column(name = "gridfs_file_name", length = 40)
    @Size(max = 40)
    @NotEmpty
    private String gridFsFileName;

    @Column(name = "provided_file_name", length = 256)
    @Size(max = 256)
    @NotEmpty
    private String filename;

    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "software_module", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_assigned_sm"))
    private JpaSoftwareModule softwareModule;

    @Column(name = "sha1_hash", length = 40, nullable = true)
    private String sha1Hash;

    @Column(name = "md5_hash", length = 32, nullable = true)
    private String md5Hash;

    @Column(name = "file_size")
    private long size;

    /**
     * Default constructor.
     */
    public JpaArtifact() {
        super();
    }

    /**
     * Constructs artifact.
     *
     * @param gridFsFileName
     *            that is the link to the {@link DbArtifact} entity.
     * @param filename
     *            that is used by {@link DbArtifact} store.
     * @param softwareModule
     *            of this artifact
     */
    public JpaArtifact(@NotNull final String gridFsFileName, @NotNull final String filename,
            final SoftwareModule softwareModule) {
        setSoftwareModule(softwareModule);
        this.gridFsFileName = gridFsFileName;
        this.filename = filename;
    }

    @Override
    public String getMd5Hash() {
        return md5Hash;
    }

    @Override
    public String getSha1Hash() {
        return sha1Hash;
    }

    public void setMd5Hash(final String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public void setSha1Hash(final String sha1Hash) {
        this.sha1Hash = sha1Hash;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(final long size) {
        this.size = size;
    }

    @Override
    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    public final void setSoftwareModule(final SoftwareModule softwareModule) {
        this.softwareModule = (JpaSoftwareModule) softwareModule;
        this.softwareModule.addArtifact(this);
    }

    public String getGridFsFileName() {
        return gridFsFileName;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
