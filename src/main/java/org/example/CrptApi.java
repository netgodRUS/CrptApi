package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class CrptApi {
    private final HttpClient httpClient;
    private final BlockingQueue<Object> requestQueue;
    private final int requestLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.requestQueue = new LinkedBlockingQueue<>();
        this.requestLimit = requestLimit;


        new Thread(this::processRequests).start();


        long period = switch (timeUnit) {
            case SECONDS -> 1;
            case MINUTES -> 60;
            case HOURS -> 3600;
            default -> throw new IllegalArgumentException("Unsupported time unit");
        };


        scheduleQueueClearTask(period);
    }

    private void processRequests() {
        while (true) {
            try {
                Object request = requestQueue.take();
                sendRequest(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sendRequest(Object request) {

        URI uri = URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create");
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createDocument(Object document, String signature) {
        try {

            requestQueue.put(document);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void scheduleQueueClearTask(long period) {
        Runnable task = () -> {
            requestQueue.clear();
        };

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(task, period, period, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {

        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);


        Object document = createDocumentObject();


        String signature = "example_signature";


        crptApi.createDocument(document, signature);
    }

    private static Object createDocumentObject() {
        Map<String, Object> description = new HashMap<>();
        description.put("participantInn", "exampleInn");

        Map<String, Object> product = new HashMap<>();
        product.put("certificate_document", "exampleCertificateDoc");
        product.put("certificate_document_date", "2020-01-23");
        

        List<Map<String, Object>> products = Collections.singletonList(product);

        Map<String, Object> document = new HashMap<>();
        document.put("description", description);
        document.put("doc_id", "exampleDocId");
        document.put("doc_status", "exampleDocStatus");
        document.put("doc_type", "LP_INTRODUCE_GOODS");
        document.put("importRequest", true);
        document.put("owner_inn", "exampleOwnerInn");
        document.put("participant_inn", "exampleParticipantInn");
        document.put("producer_inn", "exampleProducerInn");
        document.put("production_date", "2020-01-23");
        document.put("production_type", "exampleProductionType");
        document.put("products", products);
        document.put("reg_date", "2020-01-23");
        document.put("reg_number", "exampleRegNumber");

        return Collections.<String, Object>singletonMap("document", document);
    }


}

