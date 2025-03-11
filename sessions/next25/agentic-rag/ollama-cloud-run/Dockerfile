From ollama/ollama

ENV HOME /root

WORKDIR /

# Pull models from Ollama. See https://ollama.com/library for the full list of available models.
RUN ollama serve & sleep 10 && ollama pull llama3.1

Entrypoint ["ollama", "serve"]
