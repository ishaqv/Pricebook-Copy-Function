package com.circlek.ngrp.pricebook;

import com.circlek.ngrp.pricebook.schema.EventSchema;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;


public class PricebookCopyFunction {
    private static final Set<String> INTEG_STORES = new HashSet<>(Arrays.asList(System.getenv("INTEG_STORES").split(",")));
    private static final Set<String> QA_STORES = new HashSet<>(Arrays.asList(System.getenv("QA_STORES").split(",")));

    private static final String PROD_HOST = System.getenv("PROD_HOST");

    private static final String QA_HOST = System.getenv("QA_HOST");

    private static final String INTEG_HOST = System.getenv("INTEG_HOST");

    private static final String PROD_TOKEN = System.getenv("PROD_TOKEN");

    private static final String QA_TOKEN = System.getenv("QA_TOKEN");

    private static final String INTEG_TOKEN = System.getenv("INTEG_TOKEN");

    private static final Set<String> ALLOWED_TENANTS = new HashSet<>(Arrays.asList(System.getenv("ALLOWED_TENANTS").split(",")));

    @FunctionName("pricebookCopy")
    public void run(
            @EventGridTrigger(
                    name = "event"
            )
            EventSchema event,
            final ExecutionContext context
    ) {
        Logger logger = context.getLogger();
        String subject = event.subject;
        String tenant = subject.split("/")[6];
        String store = subject.split("/")[7];
        if (ALLOWED_TENANTS.contains(tenant) && (INTEG_STORES.contains(store) || QA_STORES.contains(store))) {
            logger.info(format("Pricebook copier starting to copy pricebook for store - %s", store));
            Path pricebookPath = null;
            try {
                pricebookPath = downloadPB(PROD_HOST, PROD_TOKEN, store, tenant, logger);
            } catch (Exception e) {
                logger.log(Level.SEVERE, format("an error occurred while downloading pricebook for store:%s", store), e);
            }
            if (INTEG_STORES.contains(store) && pricebookPath != null) {
                logger.info(format("Trying to copy pricebook to integ for store[%s]", store));
                try {
                    uploadPB(INTEG_HOST, INTEG_TOKEN, store, tenant, pricebookPath, logger);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, format("an error occurred while uploading pricebook for integ store:%s", store), e);
                }
            }
            if (QA_STORES.contains(store) && pricebookPath != null) {
                logger.info(format("Trying to copy pricebook to qa for store[%s]", store));
                try {
                    uploadPB(QA_HOST, QA_TOKEN, store, tenant, pricebookPath, logger);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, format("an error occurred while uploading pricebook for qa store:%s", store), e);
                }
            }
        }
        else {
            logger.info(format("Skipped pricebook copy for tenant %s and store %s", tenant, store));
        }

    }

    void uploadPB(String host, String token, String store, String tenant, Path pricebookPath, Logger logger) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            File file = pricebookPath.toFile();
            HttpPost post = new HttpPost(format("https://%s/pdi/v1/%s/pricebook/%s", host, tenant, store));
            post.setHeader("Authorization", format("Bearer %s", token));
            FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("file", fileBody);
            HttpEntity entity = builder.build();
            post.setEntity(entity);
            CloseableHttpResponse response;
            response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == 201)
                logger.info(format("Successfully uploaded pricebook for store:%s", store));
            response.close();
        }
        deleteLocalPBFile(pricebookPath, logger);
    }

    Path downloadPB(String host, String token, String store, String tenant, Logger logger) throws IOException, InterruptedException {
        DecimalFormat df = new DecimalFormat("0.00");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(format("https://%s/pdi/v1/%s/pricebook?posSiteId=%s", host, tenant, store)))
                .header("Authorization", format("Bearer %s", token))
                .GET().build();
        Path path = Path.of(format("%s.xml.gz", store));
        deleteLocalPBFile(path, logger);
        Files.createFile(path);
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(path, StandardOpenOption.WRITE));
        if (response != null && response.statusCode() == 200)
            logger.info(format("Pricebook (%sKB) downloaded successfully for store:%s", df.format((float) Files.size(path) / 1024), store));
        return path;
    }

    void deleteLocalPBFile(Path pricebookPath, Logger logger) {
        if (Files.exists(pricebookPath)) {
            try {
                Files.delete(pricebookPath);
            } catch (IOException e) {
                logger.log(Level.SEVERE, format("Unable to remove file: %s", pricebookPath));
            }
        }
    }
}
