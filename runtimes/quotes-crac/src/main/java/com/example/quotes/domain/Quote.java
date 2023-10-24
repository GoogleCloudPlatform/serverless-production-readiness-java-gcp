package com.example.quotes.domain;
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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Describes a Quote entity
 */
@Entity
@Table(name = "quotes")
public class Quote {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quotes_id_generator")
  @SequenceGenerator(name = "quotes_id_generator", sequenceName = "quotes_id_seq",  allocationSize = 10)
  @Column(name = "id")
  private Long id;

  @Column(name = "quote")
  private String quote;

  @Column(name = "author")
  private String author;

  @Column(name = "book")
  private String book;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getQuote() {
    return quote;
  }

  public void setQuote(String quote) {
    this.quote = quote;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Quote quote1)) {
      return false;
    }

    if (!Objects.equals(id, quote1.id)) {
      return false;
    }
    if (!Objects.equals(quote, quote1.quote)) {
      return false;
    }
    if (!Objects.equals(author, quote1.author)) {
      return false;
    }
    return Objects.equals(book, quote1.book);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (quote != null ? quote.hashCode() : 0);
    result = 31 * result + (author != null ? author.hashCode() : 0);
    result = 31 * result + (book != null ? book.hashCode() : 0);
    return result;
  }

  public String getBook() {
    return book;
  }

  public void setBook(String book) {
    this.book = book;
  }

}
