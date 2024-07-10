import {Button, Checkbox, TextField} from "@vaadin/react-components";
import Quote from "Frontend/generated/com/example/quotes/domain/Quote";
import {useState} from "react";
import QuoteCard from "Frontend/components/QuoteCard";
import {AutoCrud} from "@vaadin/hilla-react-crud";
import QuoteModel from "Frontend/generated/com/example/quotes/domain/QuoteModel";
import {QuoteEndpoint} from "Frontend/generated/endpoints";

export default function QuotesView() {
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [author, setAuthor] = useState("");
  const [showCrud, setShowCrud] = useState(false);

  return (
    <div className="p-m flex flex-col items-start gap-m h-full box-border">
      <div className="flex gap-xl items-baseline">
        <div className="flex gap-s items-baseline">
          <TextField
            value={author}
            onChange={e => setAuthor(e.target.value)}
            label="Book Author"
          />
          <Button
            onClick={e => QuoteEndpoint.quoteByAuthor(author).then(setQuotes)}>
            Search by Author
          </Button>
        </div>
        <Button
          onClick={e => QuoteEndpoint.randomQuote().then(q => setQuotes([q]))}>
          Get random book quote
        </Button>
        <Checkbox
          label="Manage quotes"
          checked={showCrud}
          onCheckedChanged={e => setShowCrud(e.detail.value)}/>
      </div>

      {(!!quotes.length && !showCrud) && quotes.map(quote => <QuoteCard key={quote.id} quote={quote}/>)}
      {showCrud && <AutoCrud service={QuoteEndpoint} model={QuoteModel} className="flex-grow self-stretch"/>}
    </div>
  );
};