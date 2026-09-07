package com.quickbite.quickbite.common.routing;

import com.quickbite.quickbite.common.config.property.RoutingProperties;
import com.quickbite.quickbite.common.routing.adapter.OsrmRoutingAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OsrmRoutingAdapterTest {

    @Test
    @DisplayName("doRoute correctly parses OSRM route JSON response")
    void doRoute_success() throws Exception {
        RoutingProperties properties = new RoutingProperties(
                true,
                "http://osrm-test:5000",
                false,
                null,
                Duration.ofSeconds(3)
        );
        OsrmRoutingAdapter adapter = new OsrmRoutingAdapter(properties);

        // Inject RestClient with MockRestServiceServer
        RestClient.Builder builder = RestClient.builder().baseUrl("http://osrm-test:5000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        Field clientField = OsrmRoutingAdapter.class.getDeclaredField("restClient");
        clientField.setAccessible(true);
        clientField.set(adapter, builder.build());

        String jsonResponse = """
                {
                  "code": "Ok",
                  "routes": [
                    {
                      "distance": 3520.5,
                      "duration": 540.2
                    }
                  ]
                }
                """;

        server.expect(requestTo("http://osrm-test:5000/route/v1/driving/77.5946,12.9716;77.6408,12.9784?overview=false"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        RouteResult result = adapter.route(
                GeoPoint.of(12.9716, 77.5946),
                GeoPoint.of(12.9784, 77.6408)
        );

        assertThat(result.distanceMeters()).isEqualTo(3520.5);
        assertThat(result.durationSeconds()).isEqualTo(540L);
        server.verify();
    }

    @Test
    @DisplayName("doTravelTimes correctly parses OSRM table JSON response")
    void doTravelTimes_success() throws Exception {
        RoutingProperties properties = new RoutingProperties(
                true,
                "http://osrm-test:5000",
                false,
                null,
                Duration.ofSeconds(3)
        );
        OsrmRoutingAdapter adapter = new OsrmRoutingAdapter(properties);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://osrm-test:5000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        Field clientField = OsrmRoutingAdapter.class.getDeclaredField("restClient");
        clientField.setAccessible(true);
        clientField.set(adapter, builder.build());

        String jsonResponse = """
                {
                  "code": "Ok",
                  "durations": [
                    [312.4, 645.8]
                  ]
                }
                """;

        server.expect(requestTo("http://osrm-test:5000/table/v1/driving/77.5946,12.9716;77.6,12.97;77.61,12.98?sources=0&destinations=1;2&annotations=duration"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<Long> times = adapter.travelTimes(
                GeoPoint.of(12.9716, 77.5946),
                List.of(
                        GeoPoint.of(12.97, 77.6),
                        GeoPoint.of(12.98, 77.61)
                )
        );

        assertThat(times).containsExactly(312L, 646L);
        server.verify();
    }
}
