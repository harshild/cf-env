package io.pivotal.labs.cfenv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class CloudFoundryEnvironment {

    public static final String VCAP_SERVICES = "VCAP_SERVICES";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, CloudFoundryService> services = new HashMap<>();

    public CloudFoundryEnvironment(Environment environment) throws CloudFoundryEnvironmentException {
        String vcapServices = environment.lookup(VCAP_SERVICES);

        JsonNode rootNode = parse(vcapServices);

        rootNode.forEach(serviceTypeNode -> {
            serviceTypeNode.forEach(serviceInstanceNode -> {
                CloudFoundryService service = createService(serviceInstanceNode);
                services.put(service.getName(), service);
            });
        });
    }

    private JsonNode parse(String json) throws CloudFoundryEnvironmentException {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (IOException e) {
            throw new CloudFoundryEnvironmentException("error parsing JSON: " + json, e);
        }
    }

    private CloudFoundryService createService(JsonNode serviceInstanceNode) {
        String name = serviceInstanceNode.get("name").asText();
        String label = serviceInstanceNode.get("label").asText();
        String plan = asOptional(serviceInstanceNode.get("plan"), JsonNode::asText);
        Set<String> tags = asSet(serviceInstanceNode.get("tags"), JsonNode::asText);
        Map<String, Object> credentials = asMap(serviceInstanceNode.get("credentials"), JsonNode::asText);
        return new CloudFoundryService(name, label, plan, tags, credentials);
    }

    private <E> E asOptional(JsonNode node, Function<JsonNode, E> conversion) {
        return node != null ? conversion.apply(node) : null;
    }

    private <E> Set<E> asSet(JsonNode node, Function<JsonNode, E> conversion) {
        Set<E> set = new HashSet<>();
        node.forEach(child -> set.add(conversion.apply(child)));
        return set;
    }

    private <E> Map<String, E> asMap(JsonNode node, Function<JsonNode, E> conversion) {
        HashMap<String, E> map = new HashMap<>();
        node.fields().forEachRemaining(entry -> map.put(entry.getKey(), conversion.apply(entry.getValue())));
        return map;
    }

    public Set<String> getServiceNames() {
        return services.keySet();
    }

    public CloudFoundryService getService(String serviceName) {
        CloudFoundryService service = services.get(serviceName);
        if (service == null) throw new NoSuchElementException("no such service: " + serviceName);
        return service;
    }

}
