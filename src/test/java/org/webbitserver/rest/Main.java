package org.webbitserver.rest;

import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.rest.resteasy.ResteasyHandler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public class Main {

    @Path("/hello")
    public static class HelloResource {
        @GET
        @Produces("text/plain")
        public String get() {
            return "Hello";
        }
    }

    public static void main(String[] args) throws Exception {
        WebServer webServer = WebServers.createWebServer(9877);
        webServer.add(new ResteasyHandler());
        webServer.start();

        System.out.println("Try this: curl " + webServer.getUri() + "hello");
    }
}
