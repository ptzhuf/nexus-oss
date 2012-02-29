package org.sonatype.sisu.ehcache;

import java.io.File;
import java.io.IOException;

import net.sf.ehcache.CacheManager;

/**
 * Provider responsible for creation, configuration and keeping a shared singleton EHCache CacheManager instance (to
 * make it usable across application, but also allow having more then 1 of them in whole JVM).
 * 
 * @author cstamas
 * @since 1.0
 */
public interface CacheManagerComponent
{
    /**
     * Returns the pre-configured singleton instance of CacheManager.
     * 
     * @return the configured cache manager singleton instance.
     */
    CacheManager getCacheManager();

    /**
     * Returns new instance of EHCache manager, usable to support uses cases described in
     * http://ehcache.org/documentation/faq#can-you-use-more-than-one-instance-of-ehcache-in-a-single-vm
     * 
     * @param file the EHCache XML configuration file to use for configuration, or {@code null} if you want defaults.
     * @return new CacheManager instance for every invocation.
     * @throws IOException if there is a fatal problem in accessing the provided configuration file.
     */
    CacheManager buildCacheManager( final File file )
        throws IOException;

    /**
     * Cleanly shuts down the EHCache, freeing all resources and allocated caches. Call of this method is a must in case
     * you are about to create another instance of this component (or any EHCache Manager instance, even manually
     * created one) that would have same name as this manager had (depends on how you configure it, it is usually left
     * as "default"), since EHCache (since version 2.5) is very strict and does not allow more than one instance having
     * same named manager within one JVM. If CacheManagerComponent is already stopped, this call does not have any
     * effect. The use of this method is only needed if you really want to control the "lifecycle" of this component
     * (like in a Unit Test or so) to start and stop multiple instances of Cache Manager or so in same JVM. In other
     * cases you should not tamper with these, as the component will cleanup itself.
     * 
     * @since 1.1
     */
    void shutdown();
}
