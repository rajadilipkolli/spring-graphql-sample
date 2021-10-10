package com.example.demo.gql;

import com.example.demo.gql.types.*;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletionStage;

@Controller
@Validated
@Slf4j
public class PostsDataFetchingController {//spring boot stater created an `AnnotatedDataFetchersConfigurer` to register data fetchers from `@GraphQlController` clazz
    private final PostService postService;
    private final AuthorService authorService;

    public PostsDataFetchingController(PostService postService, AuthorService authorService, BatchLoaderRegistry registry) {
        this.postService = postService;
        this.authorService = authorService;
        registry.forTypePair(String.class, Author.class)
                .registerBatchLoader((keys, batchLoaderEnvironment) ->
                        Flux.fromIterable(this.authorService.getAuthorByIdIn(keys))
                );

        registry.<String, List<Comment>>forName("commentsLoader")
                .registerMappedBatchLoader((Set<String> keys, BatchLoaderEnvironment environment) -> {
                    List<Comment> comments = postService.getCommentsByPostIdIn(keys);
                    log.info("comments of post: {}", comments);
                    Map<String, List<Comment>> mappedComments = new HashMap<>();
                    keys.forEach(
                            k -> mappedComments.put(k, comments
                                    .stream()
                                    .filter(c -> c.getPostId().equals(k)).toList())
                    );
                    log.info("mapped comments: {}", mappedComments);
                    return Mono.just(mappedComments);
                });
    }


    @QueryMapping
    public List<Post> allPosts() {
        return this.postService.getAllPosts();
    }

    @QueryMapping
    public Post postById(@Argument("postId") String id) {
        return this.postService.getPostById(id);
    }

    @MutationMapping
    public Post createPost(@Argument("createPostInput") @Validated CreatePostInput input) {
        var id = postService.createPost(input);
        return this.postService.getPostById(id.toString());
    }

    @MutationMapping
    public Comment addComment(@Argument CommentInput commentInput) {
        return this.postService.addComment(commentInput);
    }

    @SubscriptionMapping
    public Publisher<Comment> commentAdded() {
        return this.postService.commentAdded();
    }

    // resolving fields
    @SchemaMapping(field = "comments")
    public CompletionStage<List<Comment>> commentsOfPost(Post post, DataFetchingEnvironment dfe) {
        DataLoader<String, List<Comment>> dataLoader = dfe.getDataLoader("commentsLoader");
        return dataLoader.load(post.getId());
    }

    @SchemaMapping(field = "author")
    public CompletionStage<Author> authorOfPost(Post post, DataLoader<String, Author> authorDataLoader) {
        return authorDataLoader.load(post.getAuthorId());
    }
}
