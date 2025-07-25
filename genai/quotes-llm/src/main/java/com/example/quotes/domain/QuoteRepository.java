/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.quotes.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implments the Quote repository access
 */
public interface QuoteRepository extends JpaRepository<Quote,Long>, JpaSpecificationExecutor<Quote> {

    @Transactional(readOnly = true)
    @Cacheable(value = "quoteCache", key = "randomquote")
    @Query( nativeQuery = true, value =
        "SELECT id,quote,author,book FROM quotes ORDER BY RANDOM() LIMIT 1")
    Quote findRandomQuote();

    @Transactional(readOnly = true)
    @Cacheable(value = "quoteCache", key = "id")
    Optional<Quote> findById(Long id);

    @Transactional(readOnly = true)
    @Cacheable(value = "quoteCache", key = "author")
    List<Quote> findByAuthor(String author);

}
