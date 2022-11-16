# grapqhl java pojo annotation framework

Main differences over Netflix DGS:
 - code-first schema
 - aspect-oriented filters over GraphQL queries
 - automatic dataloader using references

1. Code-first schema
    1. Type mapping
    2. Custom scalars
    3. Interfaces
    4. Unions
2. Context injection, validation and aspects
    1. Graphql filter
    2. Parameter validation
    3. Authorization, aspects, etc.
3. Data fetching
    1. Detached fields
    2. References
    3. Data loaders
    4. Subscriptions
4. Setup and Spring wiring

## Code-first schema

Define your resources, objects and schema gets generated

```java
// define resource
@GraphQLResource
public class MyGraphQLResource {

    @GraphQlQuery("myQuery")
    public String query(@Nonnull @GraphQLParam("paramName") String paramName,
                        @GraphQLParam int defaultParamName,
                        @ContextInject(optional = true) User loggedUser) {
        ...
    }

    @GraphQlMutation("myMutation")
    public String mutation(@GraphQLParam("articleId") int articleId,
                           @ContextInject(optional = false) User loggedUser) {
        ...
    }

    @GraphQlSubscription("mySubscription")
    public Flowable<Change> subscription(@GraphQLParam("workspaceId") int workspaceId,
                                         @GraphQLParam("topics") List<String> topicIds,
                                         @ContextInject User loggedUser) {
        ...
    }
}
```

You can use `@GraphQLDto` annotation and automatically include all getXXX, isXXXX bean methods in your graphql schema.
Framework removes `is`/`get` prefix and uses `Introspector.decapitalize` to extract field name. Or uses field name
from `@GraphQLField` annotation if present.   
To ignore certain field in DTO object, use `@GraphQLIgnore` annotation.

```java

@GraphQLDto("PojoObject")
public class MyPojoObject {

    // translates to PojoObject.name
    public String getName() {
       ...
    }

    // ignore field if needed
    @GraphQLIgnore
    public String getIgnoredField() {
        ...
    }

    // rename field if needeed
    @GraphQLField("renamedField")
    public int getField() {
        ...
    }
}
```

### Type mapping

`int -> Int!`  
`Integer` -> `Int`  
`long` -> `Long!`   
`Long` -> `Long`   
etc., this library is using extended scalars from graphql-java.  
See source for `MoreScalars` class for implementation.

Input type definition is just plain Pojo with annotated constructor or factory method:

```java

@GraphQLTypeName(inputName = "PojoRequest")
public class PojoRequest {

    private final int id;

    // use constructor
    @JsonCreator
    public PojoRequest(@JsonProperty("id") final int id) {
        this.id = id;
    }

    // or factory method
    // if both constructor and factory method is present, constructor is used
    public static PojoRequest create(@JsonProperty("id") final int id) {
        return new PojoRequest(id);
    }
}
```

To define non-null parameter, use `@Nonnull` annotation

### Custom scalars

Please see graphql-java documentation for how to define custom scalars.  
See `Setup and Spring wiring` on how to add them to schema.

https://www.graphql-java.com/documentation/scalars/

### Interfaces

Define your grapqhl interfaces as java interfaces.

```java

@GraphQLTypeName("Accessible")
public interface Accessible {
    @GraphQLField
    boolean isAccessible();
}
```

Then use them on your DTOs. Everything gets projected into schema.

```java

@GraphQLDto("Document")
public class Document implements Accessible {
    @Override
    public boolean isAccessible() {
        return value;
    }
}
```

### Unions

To implement union you can either implement `GraphQLUnion` interface or extend `AbstractGraphQLUnion` abstract class.
`@GraphQLReturnTypes` annotation is a hint for TypeResolver (which gets created automatically) and for schema to define
possible union values.

```java

import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.invoke.ContextInject;
import cz.atlascon.graphql.ng.GraphQLParam;

@GraphQLDto("DocumentUnion")
@GraphQLReturnTypes(values = {PublicDoc.class, ErrorDoc.class})
public class DocumentUnion extends AbstractGraphQLUnion {

    public DocumentUnion(Object value) {
        super(value);
    }

}

@GraphQLDto("PublicDoc")
public static class PublicDoc {
    private final String name;

    public PublicDoc(String name) {
        this.name = name;
    }
}

@GraphQLDto("ErrorDoc")
public static class ErrorDoc {
    private final String errorReason;

    public ErrorDoc(String errorReason) {
        this.errorReason = errorReason;
    }
}

@GraphQlQuery("getSomething")
public DocumentUnion(@Nonnull @GraphQLParam("docId") String docId,
                     @ContextInject(optional = true) User loggedUser) {
    if (!canAccessDoc(docId, loggedUser)) {
        return new DocumentUnion(new ErrorDoc("No right to do that!"));
    } else {
        return new DocumentUnin(new PublicDoc("Correct doc"));
    }
}

```

## Context injection, filters, validation and aspects

Besides references the main reason for the existence of this code-first library is context injection and aspects introduced
by `GraphQLFilter` mechanism.    
Filter intercepts resource and pojo method calls and can verify and/or modify method parameters. 
For example you can secure certain methods by forcing logged-in user. 

Anything can be injected into GraphQLContext and into method parameters. 

Add filter to support context injection

```java
   GraphQLCreator.newBuilder()
                .addFilter(new ContextParamInjectFilter());
        ...
```

Create context for each execution

```java
    @POST
    public Response graphql(Map qm, @Context HttpServletRequest servletRequest,
                            @Context HttpServletResponse servletResponse) throws IOException {
        final GraphqlRequest request = new GraphqlRequest(qm);
        // get user from header        
        final Optional<User> user = this.authService.getUserFromAuthHeader(servletRequest.getHeader(HttpHeaders.AUTHORIZATION));
        Map<Class<?>, Object> ctx = Maps.newHashMap();
        if (user.isPresent()) {
            ctx.put(UserDto.class, user.map(value -> new UserDto(value.getId(), value.getName(), value.getEmail(), value.getAvatar(), value.getAccessibleParts())).get());
        }
        // pust servlet request, response to context
        ctx.put(servletRequest.getClass(), servletRequest);
        ctx.put(servletResponse.getClass(), servletResponse);

        final ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                    .operationName(request.getOperationName())
                    .variables(request.getVars())         
                    .graphQLContext(ctx)
                    .build();

        final ExecutionResult executionResult = graphQL.execute(execInput);
        return graphQLExceptionHandler.getResult(executionResult);
    }
```

Create authorization filter

```java
public class AuthorizedGraphQLFilter implements GraphQLFilter {
    
    @Override
    public void onBeforeInvoke(DataFetchingEnvironment environment, Object source, ResourceMethod method, Object[] params) {
        for (int i = 0; i < method.getMethod().getParameters().length; i++) {
            // go through method parameters
            final Parameter param = method.getMethod().getParameters()[i];
            final Authorized auth = param.getAnnotation(Authorized.class);
            // if parameter is annotated with @Authorized annotation and is null, exception is thrown
            if (auth != null && params[i] == null) {
                LOGGER.warn("Unauthorized method call - {}", method);
                throw new UnauthorizedException("Unauthorized method call");
            }
        }
    }
}
```

You can now rely on fact that `UserDto` gets injected into every authorized request and that exception is throw if not.

```java
@GraphQLResource
public class FavoriteDocMutator {

    private final FavoriteDocsService favoriteDocsService;
    private final WorkspaceAclService acl;
    
    @GraphQlMutation("addFavourite")
    public GlobalDataItemUnion addFavourite(final @Nonnull @GraphQLParam("docId") String docId,
                                            final @Nonnull @GraphQLParam("workspaceId") String workspaceId,
                                            final @ContextInject @Authorized UserDto user) {
        this.acl.checkPermissions(workspaceId, user.getId(), WorkspacePermission.EDIT);
        this.favouriteDocsService.addFavourite(docId, workspaceId, user.getId());
        return GlobalDataItemUnion.from(docId);
    }
}
```

You can go a bit further and move all ACL aspects into special filter. 
In the example above we were checking whether specific user has permissions to EDIT workspace. 
This can be processed by introducing `@WorkspaceId` annotation and writing proper filter:

```java
@GraphQLResource
public class FavoriteDocMutator {

   private final FavoriteDocsService favoriteDocsService;

   @GraphQlMutation("addFavourite")
   public GlobalDataItemUnion addFavourite(final @Nonnull @GraphQLParam("docId") String docId,
                                           final @Workspace(requiredPermission = EDIT) @Nonnull @GraphQLParam("workspaceId") String workspaceId,
                                           final @ContextInject @Authorized UserDto user) {
      this.favouriteDocsService.addFavourite(docId, workspaceId, user.getId());
      return GlobalDataItemUnion.from(docId);
   }
}
```

```java
public class WorkspaceAuthorizationFilter implements GraphQLFilter {

   private final WorkspaceAclService acl;
    
    @Override
    public void onBeforeInvoke(DataFetchingEnvironment environment, Object source, ResourceMethod method, Object[] params) {
       // find user in method parameters
       // find workspace annotated method parameter
       // call acl service, check
       // continue if OK or throw an exception
    }
}
```

## Data fetching

### Detached fields

You can detach (override) any field from any Object. If done so, detached field is called;
This works the same way as in Netflix DGS (https://netflix.github.io/dgs/datafetching/) with full support for context injections, filters, etc. 

```java
@GraphQLResource
public class DetachedGQLResource {
   @GraphQLField(parentType = "PojoObject", value = "infoList")
   public List<String> getInfoList(DataFetchingEnvironment dataFetchingEnvironment,
                                   @ContextInject InjectedObj injected,
                                   @ContextInject String username) {
      return List.of("success", "calling", "detached", "field", "from", "resource");
   }
}
```

You can define multiple fields for single method using `@GraphQLFields` annotation with multiple `@GraphQLField` arguments

```java
@GraphQLResource
public class DetachedGQLResource {
    @GraphQLFields(value = {
            @GraphQLField(parentType = "cz_atlascon_graphql_pojo_PojoObject", value = "infoList2"),
            @GraphQLField(parentType = "cz_atlascon_graphql_pojo_PojoObject", value = "infoList3")
    })
    public List<String> getInfoList2_3(DataFetchingEnvironment dataFetchingEnvironment,
                                       @ContextInject InjectedObj injected,
                                       @ContextInject String username) {
        return List.of("success23", "calling23", "field23", "from23", "resource23");
    }
}
```



### References

To simplify using of data loaders and referencing another objects, a concept of graphql reference is introduced.   
Using GraphQLReference in method output or as resource result in schema being crated for referenced object. 

```java
public interface GraphQLReference<OUTPUT> {

}
```

Example:

```java

@GraphQLResource
public class ReferenceResource {

    public static class DocRef implements GraphQLReference<Doc> {
        private final String id;
        
        public DocRef(final String id) {
            this.id = id;
        }
    }

    @GraphQLQuery("getDocs")
    public List<DocRef> getDocs(DataFetchingEnvironment dataFetchingEnvironment,
                                @ContextInject User user) {
        return docService.getDocIdsForUser(user).stream()
                .map(DocRef::new)
                .collect(Collectors.toList());
    }
}
```

References are resolved (loaded) using data loaders (see below); If there is a schema query with single input of reference type, then this query is used to resolve reference if no data loader of correct type is used. 

### Data loaders

Define your data loaders with annotation `@GraphQLDataLoader`, all context parameters are available in context

```java

@GraphQLDataLoader(referenceType = DocRef.class)
public class DocLoader implements BatchLoaderWithContext<DocRef, Doc> {

    private final DocService docService;

    @Override
    public CompletionStage<List<Doc>> load(List<DocRef> keys, BatchLoaderEnvironment environment) {
        // in case we need something from context...
        final UserDto user = (UserDto) ((Map) environment.getContext()).get(UserDto.class);
        return CompletableFuture.supplyAsync(() -> docService.loadDocs(keys, user));
    }
}
```

### Subscriptions

Use `@GraphQlSubscription` annotation and return Flowable.   
See Setup and Spring wiring for implementation of Websocket subscriptions in Spring

```java
    @GraphQlSubscription("testSub")
    public Flowable<String> get(@ContextInject(optional = true) UserDto userDto,
                                @ContextInject WebSocketSession webSocketSession) {
        return Flowable.interval(100, TimeUnit.MILLISECONDS).map(tick -> tick + "");
    }
```

## Setup and Spring wiring

This part is a bit complicated (so far) so I will just put an example from our spring boot project

BeanConfig

```java
    @Bean
    @Inject
    public GraphQL graphQLCreator(ApplicationContext applicationContext) {
        // find DTOs
        Reflections reflections = new Reflections("com.your.app.item");
        final Set<Class<?>> dtos = reflections.getTypesAnnotatedWith(GraphQLDto.class).stream()
                .filter(c -> !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toSet());
        final GraphQLCreator builder = GraphQLCreator.newBuilder();
        dtos.forEach(builder::addOutputType);
        // find resources
        final Map<String, Object> resources = applicationContext.getBeansWithAnnotation(GraphQLResource.class);
        resources.values().forEach(builder::addGqlResource);
        // add filters
        builder.addFilter(new ContextParamInjectFilter())
                .addFilter(new AuthorizedGraphQLFilter());
        // customize your graphql further
        return builder.buildWithCustomization(gqlBuilder -> {
            gqlBuilder.instrumentation(new AccessibleContentInstrumentation());
        });
    }
```

GraphQL resource

```java
@Path("/graphql")
@Singleton
public class GraphQLResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLResource.class);

    private final GraphQL graphQL;
    private final AuthService authService;
    private final GraphQLExceptionHandler graphQLExceptionHandler;
    private final List<MappedBatchLoaderWithContext> mappedBatchLoadersWithCtx;


    @Inject
    public GraphQLResource(final GraphQL graphQL,
                           final AuthService authService,
                           final GraphQLExceptionHandler graphQLExceptionHandler,
                           final ApplicationContext applicationContext) {
        this.graphQL = graphQL;
        this.authService = authService;
        this.graphQLExceptionHandler = graphQLExceptionHandler;
        // data loaders
        final Map<String, Object> dataLoaders = applicationContext.getBeansWithAnnotation(GraphQLDataLoader.class);
        this.mappedBatchLoadersWithCtx = dataLoaders.values().stream()
                .filter(o -> o instanceof MappedBatchLoaderWithContext)
                .map(MappedBatchLoaderWithContext.class::cast)
                .collect(Collectors.toList());
    }

    @POST
    public Response graphql(Map qm, @Context HttpServletRequest servletRequest,
                            @Context HttpServletResponse servletResponse) throws IOException {
        final GraphqlRequest request = new GraphqlRequest(qm);
        final Optional<User> user = this.authService.getUserFromAuthHeader(servletRequest.getHeader(HttpHeaders.AUTHORIZATION));
        final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();

        // context
        Map<Class<?>, Object> ctx = Maps.newHashMap();
        if (user.isPresent()) {
            ctx.put(UserDto.class, user.map(value -> new UserDto(value.getId(), value.getName(), value.getEmail(), value.getAvatar(), value.getAccessibleParts())).get());
        }
        ctx.put(servletRequest.getClass(), servletRequest);
        ctx.put(servletResponse.getClass(), servletResponse);

        // data loaders
        mappedBatchLoadersWithCtx.forEach(bl -> {
            final String name = DataLoaders.getDataLoaderName(bl);
            final DataLoader dl = DataLoaders.create(bl, ctx);
            dataLoaderRegistry.register(name, dl);
        });
        
        try {
            final ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                    .operationName(request.getOperationName())
                    .variables(request.getVars())
                    .dataLoaderRegistry(dataLoaderRegistry)
                    .graphQLContext(ctx)
                    .build();
            final ExecutionResult executionResult = graphQL.execute(execInput);
            return graphQLExceptionHandler.getResult(executionResult);
        } catch (Throwable t) {
            LOGGER.warn("Exception in GraphQL!", t);
            throw new RuntimeException(t);
        }
    }
}
```

Subscriptions:

```java
import static cz.atlascon.graphql.OperationMessage.*;

@Component
public class SubscriptionHandler extends TextWebSocketHandler implements SubProtocolCapable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private Map<WebSocketSession, Set<String>> subscriptionMap = Maps.newConcurrentMap();
    private Map<WebSocketSession, Optional<UserDto>> authentications = Maps.newConcurrentMap();
    private DataLoadersFactory dataLoadersFactory;
    private final GraphQL graphQL;
    private final AuthService authService;

    @Inject
    public SubscriptionHandler(final DataLoadersFactory dataLoadersFactory,
                               final GraphQL graphQL,
                               final AuthService authService) {
        this.dataLoadersFactory = dataLoadersFactory;
        this.graphQL = graphQL;
        this.authService = authService;
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        final OperationMessage op = mapper.readValue(message.getPayload(), OperationMessage.class);
        switch (op.getType()) {
            case GQL_CONNECTION_TERMINATE:
                terminateSession(session);
                break;
            case GQL_START: {
                subscriptionMap.computeIfAbsent(session, s -> Sets.newConcurrentHashSet()).add(op.getId());
                final UserDto user = authentications.get(session).orElse(null);

                final Map<String, Object> payloadMap = mapper.convertValue(op.getPayload(), new TypeReference<Map<String, Object>>() {
                });
                // context
                final Map<Class<?>, Object> ctx = new HashMap<>();
                if (user != null) {
                    ctx.put(UserDto.class, user);
                }
                ctx.put(WebSocketSession.class, session);
                // request
                GraphqlRequest request = new GraphqlRequest(payloadMap);
                ExecutionInput executionInput = ExecutionInput.newExecutionInput(request.getQuery())
                        .operationName(request.getOperationName())
                        .graphQLContext(ctx)
                        .dataLoaderRegistry(dataLoadersFactory.createDataLoaderRegistery(ctx))
                        .variables(request.getVars())
                        .build();
                ExecutionResult result = graphQL.execute(executionInput);
                if (!result.getErrors().isEmpty()) {
                    LOGGER.warn("Got graphql errors {}", result.getErrors());
                    terminateSession(session);
                    return;
                }
                if (result.getData() instanceof Publisher) {
                    handlePublisher(session, result, op.getId());
                }
            }
            break;
            case GQL_STOP:
                subscriptionMap.computeIfAbsent(session, s -> Sets.newConcurrentHashSet()).remove(op.getId());
                break;
            case GQL_CONNECTION_INIT:
                final JsonNode node = mapper.readTree(message.getPayload());
                try {
                    if (node.has("payload") && node.get("payload").has("authToken") &&
                            !node.get("payload").get("authToken").isNull()) {
                        final String token = node.get("payload").get("authToken").asText();
                        final User user = authService.getUserByToken(token);
                        final UserDto userDto = new UserDto(user.getId(), user.getName(), user.getEmail(),
                                user.getAvatar(), user.getAccessibleParts());
                        authentications.put(session, Optional.of(userDto));
                        LOGGER.info("User {} authenticated for WS session {}", user.getId(), session.getId());
                    } else {
                        authentications.put(session, Optional.empty());
                    }
                    //
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(new OperationMessage(op.getId(), GQL_CONNECTION_ACK))));
                } catch (Exception e) {
                    LOGGER.warn("Exception in WS session init - {}", e.getMessage());
                    terminateSession(session);
                }
                break;
        }
    }

    private void terminateSession(final WebSocketSession session) {
        this.subscriptionMap.remove(session);
        final Optional<UserDto> usr = this.authentications.remove(session);
        if (usr != null) {
            usr.ifPresent(user -> LOGGER.info("Session for {} terminated", user.getId()));
        }
        try {
            session.close();
        } catch (Exception e) {
            LOGGER.warn("Exception closing WS session", e);
        }
    }

    private void handlePublisher(WebSocketSession session, ExecutionResult result, String id) {
        Publisher<ExecutionResult> stream = result.getData();
        final AtomicReference<Subscription> subsc = new AtomicReference<>();
        Subscriber<ExecutionResult> subscriber = new Subscriber<ExecutionResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                subsc.set(s);
                subscriptionMap.computeIfAbsent(session, ses -> Sets.newConcurrentHashSet()).add(id);
                try {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(new OperationMessage(mapper.createObjectNode(), id, GQL_DATA))));
                } catch (Exception e) {
                    LOGGER.error("ex", e);
                    terminateSession(session);
                }
                s.request(1);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                Set<String> subscribingIds = subscriptionMap.computeIfAbsent(session, ses -> Sets.newConcurrentHashSet());
                if (subscribingIds.contains(id)) {
                    try {
                        final ObjectNode n = (ObjectNode) mapper.valueToTree(executionResult.toSpecification());
                        if (executionResult.getErrors().isEmpty()) {
                            session.sendMessage(new TextMessage(mapper.writeValueAsString(new OperationMessage(n, id, GQL_DATA))));
                        } else {
                            session.sendMessage(new TextMessage(mapper.writeValueAsString(new OperationMessage(n, id, GQL_ERROR))));
                        }
                    } catch (Exception e) {
                        LOGGER.error("ex", e);
                        terminateSession(session);
                    }
                    subsc.get().request(1);
                }

            }

            @Override
            public void onError(Throwable t) {
                try {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(new OperationMessage(mapper.valueToTree(t), id, GQL_ERROR))));
                } catch (Exception e) {
                    LOGGER.error("ex", e);
                    terminateSession(session);
                }
            }

            @Override
            public void onComplete() {
                try {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(new OperationMessage(id, GQL_COMPLETE))));
                } catch (Exception e) {
                    LOGGER.error("ex", e);
                    terminateSession(session);
                }
            }
        };
        stream.subscribe(subscriber);
    }

    @Override
    public List<String> getSubProtocols() {
        return List.of("graphql-ws");
    }

}

```
