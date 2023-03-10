package com.example.currencyexchange.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.currencyexchange.models.AuditInfo;
import com.example.currencyexchange.models.AuditInfo.RequestStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ExchangeServiceImpl implements ExchangeService{

    @Autowired
    private AuditInfoService auditInfoService;

    @Override
    public String getData(String currency) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
		.uri(URI.create("https://api.apilayer.com/exchangerates_data/convert?to=USD&from="+currency+"&amount=1"))
		.header("apiKey", "VidfAtuWPAScMaYmDdjHGvHamwU9XT6W")
		.method("GET", HttpRequest.BodyPublishers.noBody())
		.build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }


    @Override
    public Map<String, Double> fetchExchangeRates(String date, List<String> currencies)
            throws IOException, InterruptedException {

        Map<String, Double> exchangeRates = new HashMap<>();
        String BASE_CURRENCY = "USD";

        for(String currency : currencies) {
            AuditInfo auditInfo = new AuditInfo();
            auditInfo.setRequest("https://api.apilayer.com/exchangerates_data/"+date+"?symbols="+ currency +"&base="+BASE_CURRENCY+"");
            auditInfo.setStatus(RequestStatus.SENT_REQUEST);
            Date d = new Date();
            auditInfo.setCreatedTimestamp(Date.from(d.toInstant()));
            auditInfoService.createLog(auditInfo);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.apilayer.com/exchangerates_data/"+date+"?symbols="+ currency +"&base="+BASE_CURRENCY+""))
                    .header("apiKey", "VidfAtuWPAScMaYmDdjHGvHamwU9XT6W")
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = new ObjectMapper().readTree(response.body());
            Double rate = jsonNode.get("rates").get(currency).asDouble();
            exchangeRates.put(currency, rate);

            auditInfo.setResponse(exchangeRates.toString());
            auditInfo.setStatus(RequestStatus.RECEIVED_RESPONSE);
            auditInfo.setUpdatedTimestamp(Date.from(d.toInstant()));
            auditInfoService.updateLog(auditInfo);
        }
        
        return exchangeRates;
    }

}

