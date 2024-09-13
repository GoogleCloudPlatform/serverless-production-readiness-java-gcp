import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export default function QuoteCard({ quote }) {
    return (_jsxs("div", { className: "rounded-s shadow-s p-m", children: [_jsx("p", { className: "text-l", style: { fontVariant: "italic" }, children: quote.quote }), _jsxs("p", { className: "text-s", children: ["\u2014 ", quote.author, ", ", quote.book] })] }));
}
//# sourceMappingURL=QuoteCard.js.map