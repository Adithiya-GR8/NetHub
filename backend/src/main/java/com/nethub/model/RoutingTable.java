package com.nethub.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RoutingTable {
    private final List<RouteEntry> routes = new ArrayList<>();

    public void addRoute(RouteEntry route) {
        routes.add(route);
    }

    public List<RouteEntry> getAllRoutes() {
        return new ArrayList<>(routes);
    }

    public void clear() {
        routes.clear();
    }
}
