package com.sonatype.nexus.repository.nuget.internal;

import com.sonatype.nexus.repository.nuget.NugetContentDeletedEvent;
import com.sonatype.nexus.repository.nuget.NugetContentEvent;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.repository.search.ComponentMetadataFactory;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies component deletion from a nuget gallery facet.
 */
public class NugetGalleryFacetImplDeleteTest
    extends TestSupport
{
  
  @Test
  public void deleteRemovesComponentAssetAndBlob() throws Exception {
    final String packageId = "screwdriver";
    final String version = "0.1.1";

    final EventBus eventBus = mock(EventBus.class);
    final NugetGalleryFacetImpl galleryFacet = Mockito.spy(new NugetGalleryFacetImpl(
        mock(ComponentMetadataFactory.class)
    )
    {
      @Override
      protected EventBus getEventBus() {
        return eventBus;
      }
    });
    final StorageTx tx = mock(StorageTx.class);
    doReturn(tx).when(galleryFacet).openStorageTx();

    final OrientVertex component = mock(OrientVertex.class);
    final OrientVertex asset = mock(OrientVertex.class);
    final BlobRef blobRef = new BlobRef("local", "default", "a34af31");

    // Wire the mock vertices together: component has asset, asset has blobRef
    doReturn(component).when(galleryFacet).findComponent(tx, packageId, version);
    when(tx.findAssets(eq(component))).thenReturn(asList(asset));
    when(asset.getProperty(eq(StorageFacet.P_BLOB_REF))).thenReturn(blobRef.toString());
    doNothing().when(galleryFacet).deleteFromIndex(any(OrientVertex.class));

    galleryFacet.delete(packageId, version);

    // Verify that everything got deleted
    verify(tx).deleteVertex(component);
    verify(tx).deleteVertex(asset);
    verify(tx).deleteBlob(eq(blobRef));
    ArgumentCaptor<NugetContentEvent> o = ArgumentCaptor.forClass(NugetContentEvent.class);
    verify(eventBus, times(1)).post(o.capture());
    NugetContentEvent actual = o.getValue();
    assertThat(actual, instanceOf(NugetContentDeletedEvent.class));
    assertThat(actual.getId(), is(packageId));
    assertThat(actual.getVersion(), is(version));
  }
}
