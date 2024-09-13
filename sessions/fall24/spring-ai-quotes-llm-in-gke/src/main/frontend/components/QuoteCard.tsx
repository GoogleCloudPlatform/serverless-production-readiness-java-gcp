import Quote from "Frontend/generated/com/example/quotes/domain/Quote";

interface QuoteCardProps {
  quote: Quote;
}
export default function QuoteCard({quote}: QuoteCardProps) {

  return (
    <div className="rounded-s shadow-s p-m">
      <p className="text-l" style={{fontVariant: "italic"}}>{quote.quote}</p>
      <p className="text-s">— {quote.author}, {quote.book}</p>
    </div>
  );
}