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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.03.21 at 04:58:18 PM CET 
//

package org.sonatype.nexus.timeline.feeds.rest.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "feedEntries", propOrder = {
    "feedEntries"
})
@XmlRootElement(name = "feedEntries")
public class FeedEntriesXO
{

  @XmlElement(name = "feedEntry", required = true)
  @JsonProperty("feedEntries")
  protected List<FeedEntryXO> feedEntries;

  public List<FeedEntryXO> getFeedEntries() {
    if (feedEntries == null) {
      feedEntries = new ArrayList<FeedEntryXO>();
    }
    return this.feedEntries;
  }

  public void setFeedEntries(List<FeedEntryXO> value) {
    this.feedEntries = null;
    List<FeedEntryXO> draftl = this.getFeedEntries();
    draftl.addAll(value);
  }
}
