package com.vladmihalcea.book.hpjp.spring.data.query.method.repository;

import com.vladmihalcea.book.hpjp.spring.data.query.method.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.query.method.domain.PostCommentDTO;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public interface CustomPostCommentRepository {

    List<PostCommentDTO> findCommentHierarchy(Post post);
}
