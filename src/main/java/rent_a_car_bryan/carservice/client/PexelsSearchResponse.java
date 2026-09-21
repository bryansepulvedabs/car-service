package rent_a_car_bryan.carservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Solo mapeamos los campos que usamos de GET https://api.pexels.com/v1/search
@JsonIgnoreProperties(ignoreUnknown = true)
public record PexelsSearchResponse(List<PexelsPhoto> photos) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PexelsPhoto(
            String url,                                          // página de la foto en Pexels
            String photographer,
            @JsonProperty("photographer_url") String photographerUrl,
            PexelsSrc src
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PexelsSrc(String landscape, String large, String medium) {}
}