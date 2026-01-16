import { Button, Checkbox, Icon, TextField } from "@vaadin/react-components";
import Quote from "Frontend/generated/com/example/quotes/domain/Quote";
import { useState } from "react";
import QuoteCard from "Frontend/components/QuoteCard";
import { AutoCrud } from "@vaadin/hilla-react-crud";
import QuoteModel from "Frontend/generated/com/example/quotes/domain/QuoteModel";
import { QuoteEndpoint } from "Frontend/generated/endpoints";
import "@vaadin/icons";

export default function QuotesView() {
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [author, setAuthor] = useState("");
  const [book, setBook] = useState("");
  const [showCrud, setShowCrud] = useState(false);

  return (
    <div className="quotes-app">
      {/* Header */}
      <header className="app-header">
        <h1>Literary Quotes</h1>
        <p className="subtitle">Wisdom from the Written Word</p>
        <div className="header-divider" aria-hidden="true"></div>
      </header>

      {/* Search Sections */}
      <div className="search-sections">
        {/* Search by Author */}
        <section className="search-section">
          <div className="section-icon">
            <Icon icon="vaadin:user" />
          </div>
          <span className="section-label">Database</span>
          <TextField
            value={author}
            onChange={(e) => setAuthor(e.target.value)}
            label="Book Author"
            placeholder="Enter author name..."
          />
          <Button
            onClick={() => QuoteEndpoint.quoteByAuthor(author).then(setQuotes)}
          >
            Search by Author
          </Button>
        </section>

        {/* Search by Book */}
        <section className="search-section">
          <div className="section-icon">
            <Icon icon="vaadin:book" />
          </div>
          <span className="section-label">Database</span>
          <TextField
            value={book}
            onChange={(e) => setBook(e.target.value)}
            label="Book Name"
            placeholder="Enter book title..."
          />
          <Button
            onClick={() => QuoteEndpoint.quoteByBook(book).then(setQuotes)}
          >
            Search by Book
          </Button>
        </section>

        {/* Random from Database */}
        <section className="search-section">
          <div className="section-icon">
            <Icon icon="vaadin:random" />
          </div>
          <span className="section-label">Database</span>
          <Button
            onClick={() =>
              QuoteEndpoint.randomQuote().then((q) => setQuotes([q]))
            }
          >
            Get Random Quote
          </Button>
        </section>

        {/* LLM Generated Quotes */}
        <section className="search-section">
          <div className="section-icon llm-icon">
            <Icon icon="vaadin:magic" />
          </div>
          <span className="section-label llm-label">AI Generated</span>
          <Button
            className="llm-button"
            onClick={() =>
              QuoteEndpoint.randomLLMQuote().then((q) => setQuotes([q]))
            }
          >
            Gemini Flash 3.0 Preview
          </Button>
          <Button
            className="llm-button"
            onClick={() =>
              QuoteEndpoint.randomLLMInVertexQuote().then((q) => setQuotes([q]))
            }
          >
            LLama 3.1 on Vertex AI
          </Button>
        </section>

        {/* CRUD Toggle */}
        <div className="crud-toggle">
          <Checkbox
            label="Manage quotes in database"
            checked={showCrud}
            onCheckedChanged={(e) => setShowCrud(e.detail.value)}
          />
        </div>
      </div>

      {/* Quote Results */}
      {!!quotes.length && !showCrud && (
        <section className="quotes-container">
          {quotes.map((quote, index) => (
            <QuoteCard key={quote.id} quote={quote} index={index} />
          ))}
        </section>
      )}

      {/* CRUD Interface */}
      {showCrud && (
        <AutoCrud
          service={QuoteEndpoint}
          model={QuoteModel}
          className="flex-grow self-stretch"
        />
      )}

      {/* Footer */}
      <footer className="built-with">
        UI built in Java with{" "}
        <a href="https://vaadin.com/" target="_blank" rel="noopener noreferrer">
          Vaadin
        </a>
      </footer>
    </div>
  );
}
