package de.fleximove.vehicle.service.services;

import de.fleximove.vehicle.service.dto.GeoResult;
import de.fleximove.vehicle.service.dto.ReverseGeoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import de.fleximove.vehicle.service.domain.valueobject.Location;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeocodingService {
    private static final String API_KEY = "682a05be63f3b537943526zbg7bed1c";
    private static final String GEOCODE_URL = "https://geocode.maps.co/search?q={address}&api_key=" + API_KEY;
    private static final String REVERSE_GEOCODE_URL = "https://geocode.maps.co/reverse?lat={latitude}&lon={longitude}&api_key=" + API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    public Location geocodeAddress(String address) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                .encode();

        ResponseEntity<GeoResult[]> response = restTemplate.getForEntity(
                builder.buildAndExpand(address).toUri(), GeoResult[].class
        );

        GeoResult[] results = response.getBody();

        if (results != null && results.length > 0) {
            double lat = Double.parseDouble(results[0].getLat());
            double lon = Double.parseDouble(results[0].getLon());
            return new Location(lat, lon);
        }

        throw new IllegalArgumentException("Address not found: " + address);
    }

    public String reverseGeocode(Location location) {
        String url = UriComponentsBuilder.fromHttpUrl(REVERSE_GEOCODE_URL)
                .queryParam("lat", location.getLatitude())
                .queryParam("lon", location.getLongitude())
                .queryParam("api_key", API_KEY)
                .toUriString();

        ResponseEntity<ReverseGeoResponse> response = restTemplate.getForEntity(url, ReverseGeoResponse.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getDisplay_name();
        }
        return "Unbekannte Adresse";
    }

}
