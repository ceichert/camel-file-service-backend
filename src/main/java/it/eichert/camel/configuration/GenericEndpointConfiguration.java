package it.eichert.camel.configuration;

import lombok.Data;

@Data
public class GenericEndpointConfiguration {
    private String uri;
    private String scheme;
    private String authority;
    private String path;
    private String query;
}
