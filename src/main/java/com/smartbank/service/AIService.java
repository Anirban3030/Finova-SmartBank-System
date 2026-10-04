package com.smartbank.service;

import com.smartbank.exception.AiUnavailableException;
import com.smartbank.exception.TooManyRequestsException;
import com.smartbank.security.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final String UNAVAILABLE = "The AI assistant is temporarily unavailable. Please try again later.";
    private static final int TOP_K = 4;                     // how many chunks to retrieve
    private static final double SIMILARITY_THRESHOLD = 0.5; // ignore weak matches

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private static final int AI_PER_MINUTE = 5;
    private static final int AI_PER_DAY = 50;
    private final RateLimiter rateLimiter;

    public AIService(ChatClient chatClient, VectorStore vectorStore, RateLimiter rateLimiter) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.rateLimiter = rateLimiter;
    }

    public String ask(String user, String question) {
        enforceLimits(user);

        String answer;
        try {
            String prompt = buildPrompt(question);
            answer = chatClient.prompt().user(prompt).call().content();
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            log.error("AI request failed ({} chars): {} | root cause: {}: {}", question.length(),
                    e.getMessage(), root.getClass().getSimpleName(), root.getMessage());
            throw new AiUnavailableException(UNAVAILABLE);
        }

        if (answer == null || answer.isBlank()) {
            log.error("AI returned an empty answer");
            throw new AiUnavailableException(UNAVAILABLE);
        }
        return answer;
    }

    private void enforceLimits(String user) {
        String minuteKey = "ai-min:" + user;
        if (!rateLimiter.tryAcquire(minuteKey, AI_PER_MINUTE, Duration.ofMinutes(1))) {
            throw new TooManyRequestsException("Too many questions. Please wait a moment.",
                    rateLimiter.retryAfterSeconds(minuteKey, Duration.ofMinutes(1)));
        }
        String dayKey = "ai-day:" + user;
        if (!rateLimiter.tryAcquire(dayKey, AI_PER_DAY, Duration.ofDays(1))) {
            throw new TooManyRequestsException("Daily question limit reached. Please try again tomorrow.",
                    rateLimiter.retryAfterSeconds(dayKey, Duration.ofDays(1)));
        }
    }

    // Retrieval: find the most relevant policy chunks and put them in front of the question
    private String buildPrompt(String question) {
        List<Document> chunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build());

        int found = (chunks == null) ? 0 : chunks.size();
        log.info("RAG retrieval: {} chunk(s) above threshold {}", found, SIMILARITY_THRESHOLD);

        StringBuilder prompt = new StringBuilder("CONTEXT:\n");
        if (found == 0) {
            prompt.append("(no relevant SmartBank policy excerpt was found)\n");
        } else {
            for (int i = 0; i < found; i++) {
                Document chunk = chunks.get(i);
                log.info("RAG chunk {}: score={} source={}", i + 1, chunk.getScore(), chunk.getMetadata().get("source"));
                prompt.append("[").append(i + 1).append("] ").append(chunk.getText()).append("\n\n");
            }
        }
        prompt.append("\nQUESTION:\n").append(question);
        return prompt.toString();
    }
}