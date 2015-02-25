/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype
 * .com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 
 * Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are 
 * trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.internal.blobstore

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.configuration.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.configuration.ConfigurationStore
import org.sonatype.nexus.configuration.application.ApplicationDirectories
import org.sonatype.sisu.litmus.testsupport.TestSupport

import com.google.common.collect.Lists
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class BlobStoreManagerImplTest
    extends TestSupport
{

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder()

  @Mock
  ApplicationDirectories directories

  @Mock
  ConfigurationStore store

  BlobStoreManagerImpl underTest

  @Before
  public void setup() {
    when(directories.getWorkDirectory(anyString())).thenReturn(temporaryFolder.root)
    underTest = spy(new BlobStoreManagerImpl(directories, store))
  }

  @Test
  public void 'Can start with nothing configured'() throws Exception {
    when(store.list()).thenReturn(Lists.newArrayList())
    underTest.doStart()
    assert !underTest.browse()
  }

  @Test
  public void 'Can start with existing configuration'() throws Exception {
    BlobStore blobStore = mock(BlobStore)
    doReturn(blobStore).when(underTest).newBlobStore(any(BlobStoreConfiguration))
    when(store.list()).thenReturn(
        Lists.newArrayList(new BlobStoreConfiguration(name: 'test', path: temporaryFolder.root.absolutePath)))
    
    underTest.doStart()
    
    assert underTest.browse().toList() == [blobStore] 
  }


  @Test
  public void 'Can create a BlobStore'() throws Exception {
    BlobStore blobStore = mock(BlobStore)
    doReturn(blobStore).when(underTest).newBlobStore(any(BlobStoreConfiguration))

    BlobStore createdBlobStore = underTest.create(
        new BlobStoreConfiguration(name: 'test', path: temporaryFolder.root.absolutePath)
    )

    assert createdBlobStore == blobStore
    verify(blobStore).start()
    assert underTest.browse().toList() == [blobStore]
    assert underTest.get('test') == blobStore
  }

  @Test
  public void 'Can update an existing BlobStore'() throws Exception {
    BlobStore blobStore = mock(BlobStore)
    doReturn(blobStore).when(underTest).blobStore('test')
    
  }

  @Test
  public void testCreate() throws Exception {

  }

  @Test
  public void testUpdate() throws Exception {

  }

  @Test
  public void testDelete() throws Exception {

  }

  @Test
  public void testGet() throws Exception {

  }
}
