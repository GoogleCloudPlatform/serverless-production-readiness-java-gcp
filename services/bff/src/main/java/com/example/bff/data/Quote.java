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
package com.example.bff.data;

import java.util.Objects;

public class Quote
{
    private Integer id;
    private String quote;
    private String author;
    private String book;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getBook() {
      return book;
    }

    public void setBook(String book) {
    this.book = book;
  }
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
        Quote quote1 = (Quote) o;
        return Objects.equals(id, quote1.getId()) &&
                Objects.equals(quote, quote1.getQuote()) &&
                Objects.equals(author, quote1.getAuthor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, quote, author);
    }
}
