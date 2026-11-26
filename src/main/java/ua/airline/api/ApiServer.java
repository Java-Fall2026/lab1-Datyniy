package ua.airline.api;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import ua.airline.repository.FlightRepository;
import ua.airline.repository.PassengerRepository;
import ua.airline.storage.JsonMappers;

import java.util.EnumSet;

public final class ApiServer {
    private final Server server;
    private final ServerConnector connector;
    private final ValidatorFactory validation;

    public ApiServer(int port, FlightRepository flights, PassengerRepository passengers) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        validation = Validation.buildDefaultValidatorFactory();
        JsonSupport json = new JsonSupport(JsonMappers.json(), validation.getValidator());
        server = new Server();
        HttpConfiguration http = new HttpConfiguration();
        // Identities are parsed from raw URI segments; encoded separators are never used as routes.
        http.setUriCompliance(UriCompliance.from("DEFAULT,AMBIGUOUS_PATH_SEPARATOR,AMBIGUOUS_PATH_ENCODING"));
        connector = new ServerConnector(server, new HttpConnectionFactory(http));
        connector.setPort(port);
        server.addConnector(connector);
        server.setStopTimeout(5000);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.getServletHandler().setDecodeAmbiguousURIs(true);
        context.addFilter(new RequestLoggingFilter(), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addFilter(new ErrorHandlingFilter(json), "/*", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new FlightServlet(flights, json), "/api/flights/*");
        context.addServlet(new PassengerServlet(passengers, json), "/api/passengers/*");
        context.addServlet(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) {
                throw ApiException.notFound();
            }
        }, "/");
        server.setHandler(context);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        try {
            server.stop();
        } finally {
            validation.close();
        }
    }

    public int port() {
        return connector.getLocalPort();
    }
}
