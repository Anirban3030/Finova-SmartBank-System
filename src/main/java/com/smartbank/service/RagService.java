package com.smartbank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class RagService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final String KNOWLEDGE_BASE = "classpath:knowledge-base/**/*.txt";
    private static final String CHUNKING_VERSION = "1";   // change this if the chunking logic below changes
    private static final int BATCH_SIZE = 5;
    private static final int MAX_HEADING_LENGTH = 80;

    private final SimpleVectorStore vectorStore;
    private final ResourcePatternResolver resolver;
    private final Path storeDir;
    private final String embeddingModel;

    public RagService(SimpleVectorStore vectorStore, ResourcePatternResolver resolver,
                      @Value("${app.rag.store-dir}") String storeDir,
                      @Value("${spring.ai.google.genai.embedding.text.options.model}") String embeddingModel) {
        this.vectorStore = vectorStore;
        this.resolver = resolver;
        this.storeDir = Path.of(storeDir);
        this.embeddingModel = embeddingModel;
    }

    // Runs once after startup, on its own thread so it does not block other startup tasks
    @Override
    public void run(ApplicationArguments args) {
        new Thread(this::ingest, "rag-ingestion").start();
    }

    private void ingest() {
        try {
            Resource[] files = resolver.getResources(KNOWLEDGE_BASE);
            Arrays.sort(files, Comparator.comparing(f -> String.valueOf(f.getFilename())));

            List<Document> chunks = new ArrayList<>();
            for (Resource file : files) {
                chunks.addAll(toChunks(file));
            }
            if (chunks.isEmpty()) {
                log.warn("RAG ingestion: no knowledge-base files found");
                return;
            }

            String fingerprint = fingerprint(files);
            Path storeFile = storeDir.resolve("vector-store.json");
            Path fingerprintFile = storeDir.resolve("vector-store.sha256");

            // Same files, same model, same chunking as last time: reuse the saved vectors
            if (isSavedStoreCurrent(storeFile, fingerprintFile, fingerprint)) {
                try {
                    vectorStore.load(storeFile.toFile());
                    log.info("RAG ingestion: loaded {} chunks from the saved store (no embedding calls)", chunks.size());
                    return;
                } catch (RuntimeException e) {
                    log.warn("RAG ingestion: saved store could not be loaded ({}), embedding again", e.getMessage());
                }
            }

            log.info("RAG ingestion: starting, {} chunks from {} files", chunks.size(), files.length);

            // Embed in small batches so progress is visible and a failure shows where it happened
            for (int start = 0; start < chunks.size(); start += BATCH_SIZE) {
                int end = Math.min(start + BATCH_SIZE, chunks.size());
                vectorStore.add(chunks.subList(start, end));
                log.info("RAG ingestion: embedded {}/{} chunks", end, chunks.size());
            }

            // Save only after everything succeeded. The fingerprint is written last, so a crash
            // in between simply means "embed again" on the next start.
            Files.createDirectories(storeDir);
            vectorStore.save(storeFile.toFile());
            Files.writeString(fingerprintFile, fingerprint);
            log.info("RAG ingestion: finished, stored {} chunks from {} files and saved them to {}",
                    chunks.size(), files.length, storeDir);
        } catch (Exception e) {
            // The bank must keep working even if the AI part cannot start
            log.error("RAG ingestion failed: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private boolean isSavedStoreCurrent(Path storeFile, Path fingerprintFile, String fingerprint) throws IOException {
        return Files.isRegularFile(storeFile)
                && Files.isRegularFile(fingerprintFile)
                && fingerprint.equals(Files.readString(fingerprintFile).strip());
    }

    // A hash of everything that decides what the vectors look like
    private String fingerprint(Resource[] files) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update((CHUNKING_VERSION + "|" + embeddingModel + "|").getBytes(StandardCharsets.UTF_8));
        for (Resource file : files) {
            digest.update(String.valueOf(file.getFilename()).getBytes(StandardCharsets.UTF_8));
            digest.update(file.getContentAsByteArray());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    // One chunk per section: "<document title> + <heading> + <section text>"
    private List<Document> toChunks(Resource file) throws IOException {
        String text = file.getContentAsString(StandardCharsets.UTF_8).strip();
        String[] blocks = text.split("\\R\\s*\\R");   // a blank line separates blocks
        String title = blocks[0].strip();              // first block is the document title

        List<Document> chunks = new ArrayList<>();
        String pendingHeading = null;

        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i].strip();

            // A heading is a single short line that does not end like a sentence
            if (pendingHeading == null && i < blocks.length - 1 && isHeading(block)) {
                pendingHeading = block;
                continue;
            }

            String body = (pendingHeading == null) ? block : pendingHeading + "\n" + block;
            pendingHeading = null;

            chunks.add(new Document(title + "\n" + body, Map.of("source", String.valueOf(file.getFilename()))));
        }
        return chunks;
    }

    private boolean isHeading(String block) {
        if (block.contains("\n") || block.length() > MAX_HEADING_LENGTH) {
            return false;
        }
        char last = block.charAt(block.length() - 1);
        return last != '.' && last != '!' && last != '?' && last != ':';
    }
}