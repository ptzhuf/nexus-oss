package org.sonatype.nexus.blobstore.api.configuration;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

public class ConfigurationStoreImpl
    extends StateGuardLifecycleSupport
    implements ConfigurationStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final ConfigurationEntityAdapter entityAdapter;

  @Inject
  public ConfigurationStoreImpl(final @Named("config") Provider<DatabaseInstance> databaseInstance,
                                final ConfigurationEntityAdapter entityAdapter)
  {
    this.databaseInstance = databaseInstance;
    this.entityAdapter = entityAdapter;
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  private ODatabaseDocumentTx openDb() {
    return databaseInstance.get().acquire();
  }

  @Override
  @Guarded(by = STARTED)
  public List<BlobStoreConfiguration> list() {
    try (ODatabaseDocumentTx db = openDb()) {
      return entityAdapter.browse(db, Lists.<BlobStoreConfiguration>newArrayList());
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void create(final BlobStoreConfiguration configuration) {
    checkNotNull(configuration);

    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.add(db, configuration);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final BlobStoreConfiguration configuration) {
    checkNotNull(configuration);

    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.edit(db, configuration);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final BlobStoreConfiguration configuration) {
    checkNotNull(configuration);

    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.delete(db, configuration);
    }
  }
}
