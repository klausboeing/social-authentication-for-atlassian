package com.pawelniewiadomski.jira.openid.authentication.rest;

import com.atlassian.fugue.Either;
import com.atlassian.jira.rest.api.util.ErrorCollection;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.pawelniewiadomski.jira.openid.authentication.activeobjects.OpenIdDao;
import com.pawelniewiadomski.jira.openid.authentication.activeobjects.OpenIdProvider;
import com.pawelniewiadomski.jira.openid.authentication.rest.responses.BasicProviderBean;
import com.pawelniewiadomski.jira.openid.authentication.rest.responses.ProviderBean;
import com.pawelniewiadomski.jira.openid.authentication.services.OpenIdDiscoveryDocumentProvider;
import com.pawelniewiadomski.jira.openid.authentication.services.ProviderValidator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Path("providers")
@Produces({MediaType.APPLICATION_JSON})
public class ProvidersResource extends OpenIdResource {
    private static final Logger log = Logger.getLogger(ProvidersResource.class);

    @Autowired
    OpenIdDao openIdDao;

    @Autowired
    ProviderValidator validator;

    @POST
    public Response createProvider(final ProviderBean providerBean) {
        return permissionDeniedIfNotAdmin().getOrElse(
                new Supplier<javax.ws.rs.core.Response>() {
                    @Override
                    public javax.ws.rs.core.Response get() {
                        Either<ErrorCollection, OpenIdProvider> errorsOrProvider = validator.validateCreate(providerBean);

                        if (errorsOrProvider.isLeft()) {
                            return Response.ok(errorsOrProvider.left().get()).build();
                        } else {
                            return Response.ok(new ProviderBean(errorsOrProvider.right().get())).build();
                        }
                    }
                }
        );
    }

    @PUT
    @Path("/{providerId}")
    public Response updateProvider(@PathParam("providerId") final int providerId, final ProviderBean providerBean) {
        return permissionDeniedIfNotAdmin().getOrElse(
                new Supplier<javax.ws.rs.core.Response>() {
                    @Override
                    public javax.ws.rs.core.Response get() {
                        try {
                            final OpenIdProvider provider = openIdDao.findProvider(providerId);
                            final Either<ErrorCollection, OpenIdProvider> errorsOrProvider = validator.validateUpdate(provider, providerBean);

                            if (errorsOrProvider.isLeft()) {
                                return Response.ok(errorsOrProvider.left().get()).build();
                            } else {
                                return Response.ok(new ProviderBean(errorsOrProvider.right().get())).build();
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
    }

    @DELETE
    @Path("/{providerId}")
    public Response deleteProvider(@PathParam("providerId") final int providerId) {
        return permissionDeniedIfNotAdmin().getOrElse(new Supplier<javax.ws.rs.core.Response>() {
            @Override
            public Response get() {
                try {
                    openIdDao.deleteProvider(providerId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("/moveUp/{providerId}")
    public Response moveProviderUp(@PathParam("providerId") final int providerId) {
        return permissionDeniedIfNotAdmin().getOrElse(new Supplier<Response>() {
            @Override
            public Response get() {
                try {
                    final List<OpenIdProvider> providers = openIdDao.findAllProviders();
                    if (providers.size() > 1 && providerId != providers.get(0).getID()) {
                        for (int i = 1, s = providers.size(); i < s; ++i) {
                            final OpenIdProvider currentProvider = providers.get(i);
                            if (currentProvider.getID() == providerId) {
                                final OpenIdProvider previousProvider = providers.get(i - 1);
                                final int order = currentProvider.getOrdering();

                                currentProvider.setOrdering(previousProvider.getOrdering());
                                previousProvider.setOrdering(order);

                                currentProvider.save();
                                previousProvider.save();
                                break;
                            }
                        }
                    }
                } catch (SQLException e) {
                    log.warn("Unable to modify Providers", e);
                }

                return getProvidersResponse();
            }
        });
    }

    @POST
    @Path("/moveDown/{providerId}")
    public Response moveProviderDown(@PathParam("providerId") final int providerId) {
        return permissionDeniedIfNotAdmin().getOrElse(new Supplier<Response>() {
            @Override
            public Response get() {
                try {
                    final List<OpenIdProvider> providers = openIdDao.findAllProviders();
                    if (providers.size() > 1 && providerId != providers.get(providers.size() - 1).getID()) {
                        for (int i = 0, s = providers.size() - 1; i < s; ++i) {
                            final OpenIdProvider currentProvider = providers.get(i);
                            if (currentProvider.getID() == providerId) {
                                final OpenIdProvider nextProvider = providers.get(i + 1);
                                final int order = currentProvider.getOrdering();

                                currentProvider.setOrdering(nextProvider.getOrdering());
                                nextProvider.setOrdering(order);

                                currentProvider.save();
                                nextProvider.save();
                                break;
                            }
                        }
                    }
                } catch (SQLException e) {
                    log.warn("Unable to modify Providers", e);
                }
                return getProvidersResponse();
            }
        });
    }

    @POST
    @Path("/{providerId}/state")
    public Response setState(@PathParam("providerId") final int providerId, final Map<String, Boolean> params) {
        return permissionDeniedIfNotAdmin().getOrElse(new Supplier<Response>() {
            @Override
            public Response get() {
                try {
                    OpenIdProvider provider = openIdDao.findProvider(providerId);
                    if (provider != null) {
                        provider.setEnabled(params.get("enabled"));
                        provider.save();
                    }
                } catch (SQLException e) {
                    log.warn("Unable to modify Providers", e);
                }
                return getProvidersResponse();
            }
        });
    }

    protected Response getProvidersResponse() {
        try {
            return Response.ok(Lists.newArrayList(
                    Iterables.transform(openIdDao.findAllProviders(),
                            new Function<OpenIdProvider, BasicProviderBean>() {
                                @Override
                                public BasicProviderBean apply(@Nullable final OpenIdProvider input) {
                                    return new ProviderBean(input);
                                }
                            }))).cacheControl(never()).build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    public Response getOpenIdProviders() {
        return permissionDeniedIfNotAdmin().getOrElse(new Supplier<Response>() {
            @Override
            public Response get() {
                return getProvidersResponse();
            }
        });
    }
}
