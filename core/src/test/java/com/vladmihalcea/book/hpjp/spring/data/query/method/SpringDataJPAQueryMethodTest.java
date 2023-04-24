package com.vladmihalcea.book.hpjp.spring.data.query.method;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.book.hpjp.spring.data.query.method.config.SpringDataJPAQueryMethodConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.query.method.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.query.method.domain.PostComment;
import com.vladmihalcea.book.hpjp.spring.data.query.method.domain.PostCommentDTO;
import com.vladmihalcea.book.hpjp.spring.data.query.method.domain.Tag;
import com.vladmihalcea.book.hpjp.spring.data.query.method.repository.PostCommentRepository;
import com.vladmihalcea.book.hpjp.spring.data.query.method.repository.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAQueryMethodConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAQueryMethodTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final int POST_COUNT = 2;
    public static final int POST_COMMENT_COUNT = 10;
    public static final int TAG_COUNT = 10;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Before
    public void init() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                List<Tag> tags = new ArrayList<>();

                for (long i = 1; i <= TAG_COUNT; i++) {
                    Tag tag = new Tag()
                        .setId(i)
                        .setName(String.format("Tag nr. %d", i));

                    entityManager.persist(tag);
                    tags.add(tag);
                }

                LocalDateTime timestamp = LocalDateTime.of(
                    2023, 3, 15, 12, 0, 0, 0
                );

                long commentId = 0;

                for (long postId = 1; postId <= POST_COUNT; postId++) {
                    Post post = new Post()
                        .setId(postId)
                        .setTitle(String.format("Post nr. %d", postId));

                    PostComment parent = null;

                    for (long i = 1; i <= POST_COMMENT_COUNT; i++) {
                        PostComment comment = new PostComment()
                            .setId(++commentId)
                            .setParent(parent)
                            .setReview(i % 7 == 0 ? "Spam comment" : String.format("Comment %d", i))
                            .setStatus(PostComment.Status.PENDING)
                            .setCreatedOn(timestamp.plusMinutes(postId))
                            .setVotes((int) (i % 7));

                        if(i == 2 || i == 4 || i == 8) {
                            parent = comment;
                        }
                        post.addComment(comment);
                    }

                    for (int i = 0; i < TAG_COUNT; i++) {
                        post.getTags().add(tags.get(i));
                    }

                    entityManager.persist(post);
                }

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testFindByPost() {
        Post post = postRepository.getReferenceById(1L);

        List<PostComment> comments = postCommentRepository.findAllByPost(post);
        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);

        List<PostComment> comments = postCommentRepository.findAllByPostOrderByCreatedOn(post);
        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostAndStatusOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);

        List<PostComment> comments = postCommentRepository.findAllByPostAndStatusOrderByCreatedOn(
            post,
            PostComment.Status.PENDING
        );
        assertEquals(POST_COMMENT_COUNT, comments.size());
    }

    @Test
    public void testFindByPostAndStatusAndReviewLikeOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);
        String reviewPattern = "Spam";

        List<PostComment> comments = postCommentRepository.findAllByPostAndStatusAndReviewLikeOrderByCreatedOn(
            post,
            PostComment.Status.PENDING,
            reviewPattern
        );
        assertTrue(comments.isEmpty());
    }

    @Test
    public void testFindByPostAndStatusAndReviewLikeAndVotesGreaterThanEqualOrderByCreatedOn() {
        Post post = postRepository.getReferenceById(1L);
        String reviewPattern = "Spam%";
        int minVotes = 1;

        int expectedCommentCount = 0;
        {
            List<PostComment> comments = postCommentRepository.findAllByPostAndStatusAndReviewLikeAndVotesGreaterThanEqualOrderByCreatedOn(
                post,
                PostComment.Status.PENDING,
                reviewPattern,
                minVotes
            );

            expectedCommentCount = comments.size();
        }

        List<PostComment> comments = postCommentRepository.findAllByPostStatusReviewAndMinVotes(
            post,
            PostComment.Status.PENDING,
            reviewPattern,
            minVotes
        );

        assertEquals(expectedCommentCount, comments.size());
    }

    @Test
    public void testFindHierarchy() throws JsonProcessingException {
        Post post = postRepository.getReferenceById(1L);

        List<PostCommentDTO> commentRoots = postCommentRepository.findCommentHierarchy(post);
        String json = new ObjectMapper().writeValueAsString(commentRoots);
    }
}

