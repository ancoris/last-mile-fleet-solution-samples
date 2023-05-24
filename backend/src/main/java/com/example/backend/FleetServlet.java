package com.example.backend;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.backend.auth.grpcservice.AuthenticatedGrpcServiceProvider;
import com.example.backend.json.BackendConfigGsonProvider;
import com.example.backend.utils.BackendConfigUtils;
import com.example.backend.utils.SampleBackendUtils;

import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.ListDeliveryVehiclesRequest;
import google.maps.fleetengine.delivery.v1.ListDeliveryVehiclesResponse;

@Singleton
public final class FleetServlet extends HttpServlet {
    private final ServletState servletState;
    private final AuthenticatedGrpcServiceProvider grpcServiceProvider;

    @Inject
    public FleetServlet(
            ServletState servletState, AuthenticatedGrpcServiceProvider grpcServiceProvider) {
        this.servletState = servletState;
        this.grpcServiceProvider = grpcServiceProvider;
    }

    private static final Logger logger = Logger.getLogger(FleetServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");

        List<DeliveryVehicle> deliveryVehicles = listDeliveryVehicles();
        if (request.getPathInfo() == null){
            response.setContentType("text/html");

            request.setAttribute("API_KEY", SampleBackendUtils.backendProperties.apiKey());
            request.setAttribute("deliveryVehicles", deliveryVehicles);
            RequestDispatcher view = request.getRequestDispatcher("/templates/fleet_tracking.jsp");
            view.forward(request, response);
        } else {
            response.setContentType("application/json");

            PrintWriter responseWriter = response.getWriter();
            responseWriter.print(BackendConfigGsonProvider.get().toJson(deliveryVehicles));
            responseWriter.flush();
        }
    }

    private List<DeliveryVehicle> listDeliveryVehicles() {
        ListDeliveryVehiclesRequest request = ListDeliveryVehiclesRequest.newBuilder()
                .setParent(BackendConfigUtils.PARENT)
                .build();
        DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService = grpcServiceProvider
                .getAuthenticatedDeliveryService();
        ListDeliveryVehiclesResponse response = authenticatedDeliveryService.listDeliveryVehicles(request);
        return response.getDeliveryVehiclesList();
    }
}
