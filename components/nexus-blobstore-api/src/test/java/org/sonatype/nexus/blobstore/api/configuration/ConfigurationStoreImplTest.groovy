package org.sonatype.nexus.blobstore.api.configuration

import org.sonatype.nexus.orient.DatabaseInstanceRule
import org.sonatype.nexus.orient.HexRecordIdObfuscator
import org.sonatype.sisu.litmus.testsupport.TestSupport

import com.google.inject.util.Providers
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.junit.Assert.fail

class ConfigurationStoreImplTest
    extends TestSupport
{

  @Rule
  public DatabaseInstanceRule database = new DatabaseInstanceRule('test')

  private ConfigurationStoreImpl underTest

  @Before
  void setup() {
    def entityAdapter = new ConfigurationEntityAdapter()
    entityAdapter.installDependencies(new HexRecordIdObfuscator())
    underTest = new ConfigurationStoreImpl(Providers.of(database.instance), entityAdapter)
    underTest.start()
  }

  @After
  void tearDown() {
    if (underTest) {
      underTest.stop()
      underTest = null
    }
  }

  @Test
  public void 'Can create a new BlobStoreConfiguration'() throws Exception {
    createConfig()
  }

  @Test
  public void 'Can update an existing BlobStoreConfiguration'() throws Exception {
    BlobStoreConfiguration entity = createConfig()
    def version = entity.entityMetadata.version.value
    entity.name = 'baz'
    underTest.update(entity)

    assert entity.entityMetadata.version.value == '2'
    assert version != entity.entityMetadata.version.value
  }

  @Test
  public void 'Can list the persisted configurations'() throws Exception {
    BlobStoreConfiguration entity = createConfig()
    List<BlobStoreConfiguration> list = underTest.list()
    assert list.size() == 1
    assert list[0].name == entity.name
    assert list[0].path == entity.path
  }


  @Test
  public void 'Can delete an existing BlobStoreConfiguration'() throws Exception {
    BlobStoreConfiguration entity = createConfig()
    assert underTest.list()
    underTest.delete(entity)
    assert !underTest.list()
  }

  @Test
  public void 'Names are unique'() throws Exception {
    BlobStoreConfiguration entity = createConfig()

    try {
      createConfig(entity.name, 'path2')
      fail()
    }
    catch (ORecordDuplicatedException e) {
      assert e.toString().contains('blobstore_name_idx')
    }
  }

  @Test
  public void 'Paths are unique'() throws Exception {
    BlobStoreConfiguration entity = createConfig()

    try {
      createConfig('name2', entity.path)
      fail()
    }
    catch (ORecordDuplicatedException e) {
      assert e.toString().contains('blobstore_path_idx')
    }
  }

  private BlobStoreConfiguration createConfig(name = 'foo', path = 'bar') {
    def entity = new BlobStoreConfiguration(
        name: name,
        path: path
        //TODO - where to enforce path validation rules, and are these rules already defined somewhere in Nexus for 
        // reuse?
    )
    assert entity.entityMetadata == null

    underTest.create(entity)

    assert entity.entityMetadata != null
    assert entity.entityMetadata.version.value == '1'
    return entity
  }
}
