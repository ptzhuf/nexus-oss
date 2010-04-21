/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.sonatype.nexus.index;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.nexus.index.context.NexusAnalyzer;

/**
 * A default {@link QueryCreator} constructs Lucene query for provided query text.
 * <p>
 * By default wildcards are created such as query text matches beginning of the field value or beginning of the
 * class/package name segment for {@link ArtifactInfo#NAMES NAMES} field. But it can be controlled by using special
 * markers:
 * <ul>
 * <li>* - any character</li>
 * <li>'^' - beginning of the text</li>
 * <li>'$' or '&lt;' or ' ' end of the text</li>
 * </ul>
 * For example:
 * <ul>
 * <li>junit - matches junit and junit-foo, but not foo-junit</li>
 * <li>*junit - matches junit, junit-foo and foo-junit</li>
 * <li>^junit$ - matches junit, but not junit-foo, nor foo-junit</li>
 * </ul>
 * 
 * @author Eugene Kuleshov
 */
@Component( role = QueryCreator.class )
public class DefaultQueryCreator
    implements QueryCreator
{
    @Requirement
    private Logger logger;

    protected Logger getLogger()
    {
        return logger;
    }

    // ==
    public Query constructQuery( IndexerField field, String query )
    {
        return constructQuery( field.getName(), query );
    }

    public Query constructQuery( String field, String query )
    {
        Query result = null;

        if ( ArtifactInfoRecord.FLD_GROUP_ID_KW.getName().equals( field )
            || ArtifactInfoRecord.FLD_ARTIFACT_ID_KW.getName().equals( field )
            || ArtifactInfoRecord.FLD_VERSION_KW.getName().equals( field )
            || ArtifactInfoRecord.FLD_CLASSNAMES_KW.getName().equals( field ) )
        {
            // these are special untokenized fields, kept for use cases like TreeView is (exact matching).
            result = legacyConstructQuery( field, query );
        }
        else
        {
            QueryParser qp = new QueryParser( field, new NexusAnalyzer() );

            // small cheap trick
            // if a query is not "exper" (does not contain field:val kind of expression)
            // but it contains star and/or punctuation chars, example: "common-log*"
            if ( !query.contains( ":" ) )
            {
                if ( query.contains( "*" ) && query.matches( ".*(\\.|-|_).*" ) )
                {
                    query =
                        query.toLowerCase().replaceAll( "\\*", "X" ).replaceAll( "\\.|-|_", " " ).replaceAll( "X", "*" );
                }
            }

            try
            {
                result = qp.parse( query );
            }
            catch ( ParseException e )
            {
                getLogger().info(
                    "Query parsing with \"legacy\" method, we got ParseException from QueryParser: " + e.getMessage() );

                result = legacyConstructQuery( field, query );
            }
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Query parsed as: " + result.toString() );
        }

        return result;
    }

    public Query legacyConstructQuery( String field, String query )
    {
        if ( query == null || query.length() == 0 )
        {
            getLogger().info( "Empty or null query for field:" + field );

            return null;
        }

        String q = query.toLowerCase();

        char h = query.charAt( 0 );

        if ( ArtifactInfoRecord.FLD_CLASSNAMES_KW.getName().equals( field )
            || ArtifactInfoRecord.FLD_CLASSNAMES.getName().equals( field ) )
        {
            q = q.replaceAll( "\\.", "/" );

            if ( h == '^' )
            {
                q = q.substring( 1 );

                if ( q.charAt( 0 ) != '/' )
                {
                    q = '/' + q;
                }
            }
            else if ( h != '*' )
            {
                q = "*/" + q;
            }
        }
        else
        {
            if ( h == '^' )
            {
                q = q.substring( 1 );
            }
            else if ( h != '*' )
            {
                q = "*" + q;
            }
        }

        int l = q.length() - 1;
        char c = q.charAt( l );
        if ( c == ' ' || c == '<' || c == '$' )
        {
            q = q.substring( 0, q.length() - 1 );
        }
        else if ( c != '*' )
        {
            q += "*";
        }

        int n = q.indexOf( '*' );
        if ( n == -1 )
        {
            return new TermQuery( new Term( field, q ) );
        }
        else if ( n > 0 && n == q.length() - 1 )
        {
            return new PrefixQuery( new Term( field, q.substring( 0, q.length() - 1 ) ) );
        }

        return new WildcardQuery( new Term( field, q ) );
    }
}
