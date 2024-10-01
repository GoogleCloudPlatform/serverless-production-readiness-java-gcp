import {Button, Checkbox, Icon, TextField} from "@vaadin/react-components";
import Quote from "Frontend/generated/com/example/quotes/domain/Quote";
import {useState} from "react";
import QuoteCard from "Frontend/components/QuoteCard";
import {AutoCrud} from "@vaadin/hilla-react-crud";
import QuoteModel from "Frontend/generated/com/example/quotes/domain/QuoteModel";
import {QuoteEndpoint} from "Frontend/generated/endpoints";
import "@vaadin/icons";

export default function QuotesView() {
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [author, setAuthor] = useState("");
  const [showCrud, setShowCrud] = useState(false);

  return (
      <div className="p-m flex flex-col items-start gap-m h-full box-border">
        <div className="flex flex-col gap-m">
          <div
              className="flex gap-s items-baseline border border-b border-dashed border-contrast-50 p-l rounded-l">
            <Icon icon="vaadin:database"/>
            <TextField
                value={author}
                onChange={e => setAuthor(e.target.value)}
                label="Book Author"
            />
            <Button
                onClick={e => QuoteEndpoint.quoteByAuthor(author).then(setQuotes)}>
              Search by Author in database
            </Button>
            <Button
                onClick={e => QuoteEndpoint.randomQuote().then(q => setQuotes([q]))}>
              Get random quote from database
            </Button>
          </div>
          <div
              className="flex gap-s items-baseline border border-b border-dashed border-contrast-50 p-l rounded-l">
            <Icon icon="vaadin:cloud"/>
            <Button
                onClick={e => QuoteEndpoint.randomLLMQuote().then(q => setQuotes([q]))}>
              Random Quote from Gemini Pro Model in VertexAI
            </Button>
          </div>
          <div
              className="flex gap-s items-baseline border border-b border-dashed border-contrast-50 p-l rounded-l">
            <Icon icon="vaadin:cloud"/>
            <Button
                onClick={e => QuoteEndpoint.randomLLMInVertexQuote().then(q => setQuotes([q]))}>
              Random quote from LLama3.1 Open-Model LLM in VertexAI
            </Button>
            <Button
                onClick={e => QuoteEndpoint.randomLLMInGKEQuote().then(q => setQuotes([q]))}>
              Random quote from LLama 3.1 Open-Model LLM in GKE
            </Button>
          </div>
          <div>
            <Checkbox
                label="Manage book quotes in DB"
                checked={showCrud}
                onCheckedChanged={e => setShowCrud(e.detail.value)}/>
          </div>
        </div>
        {(!!quotes.length && !showCrud) && quotes.map(quote => <QuoteCard
            key={quote.id} quote={quote}/>)}
        {showCrud && <AutoCrud service={QuoteEndpoint} model={QuoteModel}
                               className="flex-grow self-stretch"/>}
      </div>
  );
};
