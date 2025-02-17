package com.linkedin.datahub.graphql.types.glossary;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.GlossaryTermUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.StringArray;
import com.linkedin.datahub.graphql.QueryContext;
import com.linkedin.datahub.graphql.generated.EntityType;
import com.linkedin.datahub.graphql.types.BrowsableEntityType;
import com.linkedin.datahub.graphql.types.SearchableEntityType;
import com.linkedin.datahub.graphql.generated.AutoCompleteResults;
import com.linkedin.datahub.graphql.generated.BrowsePath;
import com.linkedin.datahub.graphql.generated.BrowseResults;
import com.linkedin.datahub.graphql.generated.GlossaryTerm;
import com.linkedin.datahub.graphql.generated.FacetFilterInput;
import com.linkedin.datahub.graphql.generated.SearchResults;
import com.linkedin.datahub.graphql.types.glossary.mappers.GlossaryTermSnapshotMapper;
import com.linkedin.datahub.graphql.types.mappers.AutoCompleteResultsMapper;
import com.linkedin.datahub.graphql.types.mappers.BrowsePathsMapper;
import com.linkedin.datahub.graphql.resolvers.ResolverUtils;
import com.linkedin.datahub.graphql.types.mappers.BrowseResultMapper;
import com.linkedin.datahub.graphql.types.mappers.UrnSearchResultsMapper;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.entity.Entity;
import com.linkedin.metadata.extractor.AspectExtractor;
import com.linkedin.metadata.browse.BrowseResult;
import com.linkedin.metadata.query.AutoCompleteResult;
import com.linkedin.metadata.search.SearchResult;

import graphql.execution.DataFetcherResult;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.linkedin.datahub.graphql.Constants.BROWSE_PATH_DELIMITER;

public class GlossaryTermType implements SearchableEntityType<GlossaryTerm>, BrowsableEntityType<GlossaryTerm> {

    private static final Set<String> FACET_FIELDS = ImmutableSet.of("");

    private final EntityClient _glossaryTermsClient;

    public GlossaryTermType(final EntityClient glossaryTermsClient) {
        _glossaryTermsClient = glossaryTermsClient;
    }

    @Override
    public Class<GlossaryTerm> objectClass() {
        return GlossaryTerm.class;
    }

    @Override
    public EntityType type() {
        return EntityType.GLOSSARY_TERM;
    }

    @Override
    public List<DataFetcherResult<GlossaryTerm>> batchLoad(final List<String> urns, final QueryContext context) {
        final List<GlossaryTermUrn> glossaryTermUrns = urns.stream()
                .map(GlossaryTermUtils::getGlossaryTermUrn)
                .collect(Collectors.toList());

        try {
            final Map<Urn, Entity> glossaryTermMap = _glossaryTermsClient.batchGet(glossaryTermUrns
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()),
                context.getActor());

            final List<Entity> gmsResults = new ArrayList<>();
            for (GlossaryTermUrn urn : glossaryTermUrns) {
                gmsResults.add(glossaryTermMap.getOrDefault(urn, null));
            }
            return gmsResults.stream()
                    .map(gmsGlossaryTerm ->
                        gmsGlossaryTerm == null ? null
                            : DataFetcherResult.<GlossaryTerm>newResult()
                                .data(GlossaryTermSnapshotMapper.map(gmsGlossaryTerm.getValue().getGlossaryTermSnapshot()))
                                .localContext(AspectExtractor.extractAspects(gmsGlossaryTerm.getValue().getGlossaryTermSnapshot()))
                                .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to batch load GlossaryTerms", e);
        }
    }

    @Override
    public SearchResults search(@Nonnull String query,
                                @Nullable List<FacetFilterInput> filters,
                                int start,
                                int count,
                                @Nonnull final QueryContext context) throws Exception {
        final Map<String, String> facetFilters = ResolverUtils.buildFacetFilters(filters, FACET_FIELDS);
        final SearchResult searchResult = _glossaryTermsClient.search(
            "glossaryTerm", query, facetFilters, start, count, context.getActor());
        return UrnSearchResultsMapper.map(searchResult);
    }

    @Override
    public AutoCompleteResults autoComplete(@Nonnull String query,
                                            @Nullable String field,
                                            @Nullable List<FacetFilterInput> filters,
                                            int limit,
                                            @Nonnull final QueryContext context) throws Exception {
        final Map<String, String> facetFilters = ResolverUtils.buildFacetFilters(filters, FACET_FIELDS);
        final AutoCompleteResult result = _glossaryTermsClient.autoComplete(
            "glossaryTerm", query, facetFilters, limit, context.getActor());
        return AutoCompleteResultsMapper.map(result);
    }

    @Override
    public BrowseResults browse(@Nonnull List<String> path,
                                @Nullable List<FacetFilterInput> filters,
                                int start,
                                int count,
                                @Nonnull final QueryContext context) throws Exception {
        final Map<String, String> facetFilters = ResolverUtils.buildFacetFilters(filters, FACET_FIELDS);
        final String pathStr = path.size() > 0 ? BROWSE_PATH_DELIMITER + String.join(BROWSE_PATH_DELIMITER, path) : "";
        final BrowseResult result = _glossaryTermsClient.browse(
                "glossaryTerm",
                pathStr,
                facetFilters,
                start,
                count,
            context.getActor());
        return BrowseResultMapper.map(result);
    }

    @Override
    public List<BrowsePath> browsePaths(@Nonnull String urn, @Nonnull final QueryContext context) throws Exception {
        final StringArray result = _glossaryTermsClient.getBrowsePaths(GlossaryTermUtils.getGlossaryTermUrn(urn), context.getActor());
        return BrowsePathsMapper.map(result);
    }

}
