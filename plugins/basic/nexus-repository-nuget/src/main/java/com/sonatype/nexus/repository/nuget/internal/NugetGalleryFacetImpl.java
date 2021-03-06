/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package com.sonatype.nexus.repository.nuget.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Named;

import com.sonatype.nexus.repository.nuget.internal.odata.ComponentQuery;
import com.sonatype.nexus.repository.nuget.internal.odata.NugetPackageUtils;
import com.sonatype.nexus.repository.nuget.internal.odata.ODataFeedUtils;
import com.sonatype.nexus.repository.nuget.internal.odata.ODataTemplates;
import com.sonatype.nexus.repository.nuget.internal.odata.ODataUtils;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.util.NestedAttributesMap;
import org.sonatype.nexus.repository.view.Parameters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.odata4j.producer.InlineCount;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sonatype.nexus.repository.nuget.internal.NugetProperties.*;
import static java.util.Arrays.asList;
import static org.odata4j.producer.resources.OptionsQueryParser.parseInlineCount;
import static org.odata4j.producer.resources.OptionsQueryParser.parseSkip;
import static org.odata4j.producer.resources.OptionsQueryParser.parseTop;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_FORMAT;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_VERSION;

/**
 * @since 3.0
 */
@Named("local")
public class NugetGalleryFacetImpl
    extends FacetSupport
    implements NugetGalleryFacet
{
  public static final String NUGET = "nuget";

  public static final String NO_NAMESPACES = "";

  protected Clock clock = new Clock();

  private StorageFacet storage;

  private static final VersionScheme SCHEME = new GenericVersionScheme();

  @Override
  protected void doConfigure() throws Exception {
    storage = getRepository().facet(StorageFacet.class);
  }

  @Override
  protected void doDestroy() throws Exception {
    storage = null;
  }

  @Override
  @Guarded(by = STARTED)
  public int count(final String path, final Parameters parameters) {
    Map<String, String> query = asMap(parameters);
    log.debug("Count: " + query);

    final ComponentQuery componentQuery = ODataUtils.query(query, true);

    try (StorageTx storageTx = openStorageTx()) {

      int count = executeCount(componentQuery, storageTx);

      final Integer skip = parseSkip(query.get("$skip"));
      if (null != skip && skip >= 0) {
        // If we were asked to skip some values, deduct this from the total count reported by the query
        count = Math.max(0, count - skip);
      }
      final Integer top = parseTop(query.get("$top"));
      if (null != top && top >= 0) {
        // If we were asked for the top 'n' values, then cap the count at no more than this value
        count = Math.min(count, top.intValue());
      }

      storageTx.commit();
      return count;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public String feed(final String base, final String name, final Parameters parameters) {
    Map<String, String> query = asMap(parameters);

    log.debug("Select: " + query);

    final Map<String, String> extra =
        ImmutableMap.of("BASEURI", base, "ENDPOINT", name, LAST_UPDATED,
            ODataFeedUtils.datetime(System.currentTimeMillis()), "NAMESPACES", NO_NAMESPACES);

    final StringBuilder xml = new StringBuilder();
    xml.append(ODataTemplates.interpolate(ODataTemplates.NUGET_FEED, extra));


    // NEXUS-6822 Visual Studio doesn't send a sort order by default, leading to unusable results
    if (!query.containsKey("$orderby")) {
      query.put("$orderby", P_DOWNLOAD_COUNT + " desc");
    }

    ComponentQuery componentQuery = ODataUtils.query(query, false);

    try (StorageTx storageTx = openStorageTx()) {

      // NXCM-4502 add inlinecount only if requested
      if (inlineCountRequested(query)) {
        int inlineCount = executeCount(componentQuery, storageTx);
        xml.append(ODataTemplates.interpolate(ODataTemplates.NUGET_INLINECOUNT,
            ImmutableMap.of("COUNT", String.valueOf(inlineCount))));
      }

      final Iterable<OrientVertex> components = storageTx.findComponents(componentQuery.getWhere(),
          componentQuery.getParameters(), getRepositories(), componentQuery.getQuerySuffix());

      int n = 0;
      for (OrientVertex component : components) {
        n++;

        final Map<String, ?> data = toData(component, extra);

        xml.append(ODataTemplates.interpolate(ODataTemplates.NUGET_ENTRY, data));
        if (n == ODataUtils.PAGE_SIZE) {
          xml.append("  <link rel=\"next\" href=\"").append(base).append('/').append(name);
          xml.append('?').append(ODataFeedUtils.skipLink(data, query)).append("\"/>\n");
          break;
        }
      }

      storageTx.commit();
    }

    return xml.append("</feed>").toString();
  }

  private Map<String, String> asMap(final Parameters parameters) {
    Map<String, String> query = Maps.newHashMap();
    for (String param : parameters.names()) {
      query.put(param, parameters.get(param));
    }
    return query;
  }

  private Map<String, ?> toData(final OrientVertex component, Map<String, String> extra) {
    Map<String, Object> data = Maps.newHashMap();

    // TODO: Put all the Vertex properties in there

    for (String key : extra.keySet()) {
      data.put(key, extra.get(key));
    }

    return data;
  }

  @Override
  @Guarded(by = STARTED)
  public String entry(final String base, final String id, final String version) {
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public String locate(final String id, final String version) {
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public String[] identify(final String location) {
    return new String[0];
  }

  @Override
  @Guarded(by = STARTED)
  public void put(final InputStream inputStream) throws IOException, NugetPackageException {
    try (StorageTx storageTx = openStorageTx();
         TempStreamSupplier tempStream = new TempStreamSupplier(inputStream)) {

      final HashMap<String, String> recordMetadata = Maps.newHashMap();
      Map<String, String> packageMetadata;
      try (InputStream in = tempStream.get()) {
        packageMetadata = NugetPackageUtils.packageMetadata(in);
      }

      recordMetadata.putAll(packageMetadata);

      // TODO: Do something cleaner with this derived data, as well as the derived stuff inside createOrUpdateComponent
      // Note: These are defaults that hold for locally-published packages,
      // but should be overridden for remotely fetched content
      final String creationTime = ODataFeedUtils.datetime(clock.millis());
      recordMetadata.put(CREATED, creationTime);
      recordMetadata.put(LAST_UPDATED, creationTime);
      recordMetadata.put(PUBLISHED, creationTime);

      try (InputStream in = tempStream.get()) {
        createOrUpdatePackage(storageTx, recordMetadata, in);
      }

      maintainAggregateInfo(storageTx, recordMetadata.get(ID));

      storageTx.commit();
    }
  }

  @VisibleForTesting
  StorageTx openStorageTx() {
    return storage.openTx();
  }

  @VisibleForTesting
  void createOrUpdatePackage(final StorageTx storageTx, final Map<String, String> recordMetadata,
                             final InputStream packageStream)
  {
    final OrientVertex bucket = storageTx.getBucket();
    final OrientVertex component = createOrUpdateComponent(storageTx, bucket, recordMetadata);

    createOrUpdateAsset(storageTx, bucket, component, packageStream);
  }

  private String blobName(OrientVertex component) {
    return component.getProperty(P_NAME) + " " + component.getProperty(P_VERSION) + "@" + getRepository().getName();
  }

  /**
   * Ensure all the components for a given 'id':
   * - have up to date latest version/absolute latest version fields.
   * - have up to date aggregate download count info
   * (updating download counts is different for hosted and proxies; proxies possibly don't need to..)
   */
  protected void maintainAggregateInfo(final StorageTx storageTx, final String id) {
    maintainAggregateInfo(storageTx, findComponentsById(storageTx, id));
  }

  @VisibleForTesting
  void maintainAggregateInfo(final StorageTx storageTx, final Iterable<OrientVertex> versions) {
    long totalDownloadCount = 0;

    SortedSet<OrientVertex> releases = Sets.newTreeSet(new ComponentVersionComparator());
    SortedSet<OrientVertex> allReleases = Sets.newTreeSet(new ComponentVersionComparator());

    for (OrientVertex version : versions) {
      final NestedAttributesMap nugetAttributes = storageTx.getAttributes(version).child(NUGET);

      final boolean isPrerelease = checkNotNull(nugetAttributes.get(P_IS_PRERELEASE, Boolean.class));
      if (!isPrerelease) {
        releases.add(version);
      }
      allReleases.add(version);

      final int versionDownloadCount = checkNotNull(nugetAttributes.get(P_VERSION_DOWNLOAD_COUNT, Integer.class));
      totalDownloadCount += versionDownloadCount;
    }

    OrientVertex latestVersion = releases.isEmpty() ? null : releases.last();
    OrientVertex absoluteLatestVersion = allReleases.isEmpty() ? null : allReleases.last();

    for (OrientVertex component : allReleases) {
      final NestedAttributesMap nugetAttributes = storageTx.getAttributes(component).child(NUGET);

      nugetAttributes.set(P_IS_LATEST_VERSION, component.equals(latestVersion));
      nugetAttributes.set(P_IS_ABSOLUTE_LATEST_VERSION, component.equals(absoluteLatestVersion));

      nugetAttributes.set(P_DOWNLOAD_COUNT, totalDownloadCount);
    }
  }

  private Iterable<OrientVertex> findComponentsById(final StorageTx storageTx, final Object id) {
    final String whereClause = "name = :name";
    Map<String, Object> parameters = ImmutableMap.of(P_NAME, id);
    return storageTx.findComponents(whereClause, parameters, getRepositories(), null);
  }

  private OrientVertex createOrUpdateAsset(final StorageTx storageTx, final OrientVertex bucket,
                                           final OrientVertex component, final InputStream in)
  {
    OrientVertex asset = null;

    final List<OrientVertex> assets = storageTx.findAssets(component);
    if (assets.isEmpty()) {
      asset = storageTx.createAsset(bucket);
      asset.addEdge(StorageFacet.E_PART_OF_COMPONENT, component);
    }
    else {
      asset = assets.get(0);
    }

    final ImmutableMap<String, String> headers = ImmutableMap
        .of(BlobStore.BLOB_NAME_HEADER, blobName(component), BlobStore.CREATED_BY_HEADER, "unknown");

    storageTx.setBlob(in, headers, asset, Arrays.asList(HashAlgorithm.SHA512), "application/zip");

    return asset;
  }

  private String checkVersion(String stringValue) {
    try {
      SCHEME.parseVersion(checkNotNull(stringValue));
      return stringValue;
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException("Bad version syntax: " + stringValue);
    }
  }

  private OrientVertex createOrUpdateComponent(final StorageTx storageTx, final OrientVertex bucket,
                                               final Map<String, String> data)
  {
    final String id = checkNotNull(data.get(ID));
    final String version = checkVersion(data.get(VERSION));

    final OrientVertex component = findOrCreateComponent(storageTx, bucket, id, version);

    final boolean republishing = component.getIdentity().isNew();

    final NestedAttributesMap attributes = storageTx.getAttributes(component);
    final NestedAttributesMap nugetAttr = attributes.child(NUGET);

    setDerivedAttributes(data, nugetAttr, republishing);

    nugetAttr.set(P_AUTHORS, data.get(AUTHORS));
    nugetAttr.set(P_COPYRIGHT, data.get(COPYRIGHT));
    nugetAttr.set(P_DEPENDENCIES, data.get(DEPENDENCIES));
    nugetAttr.set(P_DESCRIPTION, data.get(DESCRIPTION));
    nugetAttr.set(P_GALLERY_DETAILS_URL, data.get(GALLERY_DETAILS_URL));
    nugetAttr.set(P_ICON_URL, data.get(ICON_URL));
    nugetAttr.set(P_ID, data.get(ID));
    nugetAttr.set(P_IS_PRERELEASE, Boolean.parseBoolean(data.get(IS_PRERELEASE)));
    nugetAttr.set(P_LANGUAGE, data.get(LANGUAGE));
    nugetAttr.set(P_LICENSE_URL, data.get(LICENSE_URL));
    nugetAttr.set(P_LOCATION, data.get(LOCATION));
    nugetAttr.set(P_PACKAGE_HASH, data.get(PACKAGE_HASH));
    nugetAttr.set(P_PACKAGE_HASH_ALGORITHM, data.get(PACKAGE_HASH_ALGORITHM));
    nugetAttr.set(P_PACKAGE_SIZE, data.get(PACKAGE_SIZE));
    nugetAttr.set(P_PROJECT_URL, data.get(PROJECT_URL));
    nugetAttr.set(P_RELEASE_NOTES, data.get(RELEASE_NOTES));
    nugetAttr.set(P_REPORT_ABUSE_URL, data.get(REPORT_ABUSE_URL));
    nugetAttr.set(P_REQUIRE_LICENSE_ACCEPTANCE, Boolean.parseBoolean(data.get(REQUIRE_LICENSE_ACCEPTANCE)));
    nugetAttr.set(P_SUMMARY, data.get(SUMMARY));
    nugetAttr.set(P_TAGS, data.get(TAGS));
    nugetAttr.set(P_TITLE, data.get(TITLE));
    nugetAttr.set(NugetProperties.P_VERSION, data.get(VERSION));

    return component;
  }

  @VisibleForTesting
  void setDerivedAttributes(final Map<String, String> incomingMetadata,
                            final NestedAttributesMap storedMetadata, final boolean republishing)
  {
    // Force the version download count to zero if it wasn't provided nor previously set
    if (!republishing) {
      storedMetadata.set(P_DOWNLOAD_COUNT, 0);
      storedMetadata.set(P_VERSION_DOWNLOAD_COUNT, 0);
    }

    // TODO: Make sure this is maintained correctly in the case of republished packages

    final Date now = new Date(clock.millis());

    storedMetadata.set(P_CREATED, now);
    storedMetadata.set(P_PUBLISHED, now);
    storedMetadata.set(P_LAST_UPDATED, now);
  }

  private OrientVertex findOrCreateComponent(final StorageTx storageTx, final OrientVertex bucket, final String name,
                                             final String version)
  {
    final OrientVertex found = findComponent(storageTx, name, version);
    if (found != null) {
      return found;
    }

    return createComponent(storageTx, bucket, name, version);
  }

  private OrientVertex findComponent(final StorageTx storageTx, final String name, final Object version) {
    final ImmutableMap<String, Object> params = ImmutableMap.of(P_VERSION, version, P_NAME, name);

    final Iterable<OrientVertex> components = storageTx
        .findComponents("version=:version AND name=:name", params, getRepositories(), null);

    final Iterator<OrientVertex> iterator = components.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    else {
      return null;
    }
  }

  private OrientVertex createComponent(final StorageTx storageTx, final OrientVertex bucket, final String name,
                                       final String version)
  {
    log.debug("Creating NuGet component {} v. {}", name, version);
    OrientVertex component = storageTx.createComponent(storageTx.getBucket());
    component.setProperty(P_FORMAT, NugetFormat.NAME);
    component.setProperty(P_NAME, name);
    // Nuget components don't have a group
    component.setProperty(P_VERSION, version);
    return component;
  }

  private boolean inlineCountRequested(Map<String, String> query) {
    return InlineCount.ALLPAGES.equals(parseInlineCount(query.get("$inlinecount")));
  }

  private int executeCount(final ComponentQuery query, final StorageTx storageTx)
  {
    return (int) storageTx.countComponents(query.getWhere(), query.getParameters(), getRepositories(),
        query.getQuerySuffix());
  }


  private List<Repository> getRepositories() {
    // TODO: Consider groups
    return asList(getRepository());
  }

  @VisibleForTesting
   static class ComponentVersionComparator
      implements Comparator<OrientVertex>
  {
    @Override
    public int compare(final OrientVertex o1, final OrientVertex o2) {
      try {
        Version v1 = SCHEME.parseVersion((String) o1.getProperty(P_VERSION));
        Version v2 = SCHEME.parseVersion((String) o2.getProperty(P_VERSION));
        return v1.compareTo(v2);
      }
      catch (InvalidVersionSpecificationException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
