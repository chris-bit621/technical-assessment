package com.eatclub.client;

import com.eatclub.exception.ChallengeDataException;
import com.eatclub.model.source.ChallengeData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class HttpChallengeDataClient implements ChallengeDataClient {

    private final RestClient restClient;
    private final URI url;

    public HttpChallengeDataClient(
            RestClient.Builder builder,
            @Value("${eatclub.challenge-data.url}") URI url,
            @Value("${eatclub.challenge-data.connect-timeout}") Duration connectTimeout,
            @Value("${eatclub.challenge-data.read-timeout}") Duration readTimeout
    ) {
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.requestFactory(requestFactory).build();
        this.url = url;
    }

    @Override
    public ChallengeData fetch() {
        try {
            var data = restClient.get().uri(url).retrieve().body(ChallengeData.class);
            if (data == null || data.restaurants() == null) {
                throw new ChallengeDataException("Challenge data is missing its restaurant list");
            }
            return data;
        } catch (RestClientException exception) {
            throw new ChallengeDataException("Could not fetch challenge data", exception);
        }
    }
}
