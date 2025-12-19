import Quote from "Frontend/generated/com/example/quotes/domain/Quote";

interface QuoteCardProps {
  quote: Quote;
  index?: number;
}

export default function QuoteCard({ quote, index = 0 }: QuoteCardProps) {
  const animationDelay = `${index * 100}ms`;

  return (
    <article className="quote-card" style={{ animationDelay }}>
      <blockquote className="quote-text">
        {quote.quote}
      </blockquote>
      <footer className="quote-attribution">
        <span className="quote-author">{quote.author}</span>
        <span className="quote-divider" aria-hidden="true"></span>
        <cite className="quote-book">{quote.book}</cite>
      </footer>
    </article>
  );
}
