package com.example.demo;

import com.example.demo.gql.types.Post;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.boot.test.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
//@AutoConfigureMockMvc  // to make subscription work.
@AutoConfigureGraphQlTester
@Slf4j
public class QueryTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void allPosts() {
        when(postService.getAllPosts()).thenReturn(List.of(
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title")
                        .content("test content")
                        .build(),
                Post.builder().id(UUID.randomUUID().toString())
                        .title("test title2")
                        .content("test content2")
                        .build()
        ));

        var allPosts = """
                query posts{
                   allPosts{
                     id
                     title
                     content
                   }
                 }""";
        graphQlTester.query(allPosts)
                .execute()
                .path("allPosts[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).contains("test title", "test title2"));

        verify(postService, times(1)).getAllPosts();
        verifyNoMoreInteractions(postService);
    }

}
