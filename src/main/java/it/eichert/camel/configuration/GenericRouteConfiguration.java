package it.eichert.camel.configuration;

import lombok.Data;

@Data
public class GenericRouteConfiguration {
    private String routeId;
    private GenericEndpointConfiguration from;
    private GenericEndpointConfiguration to;
}
