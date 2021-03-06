package org.webbitserver.rest.chat;

import com.google.gson.Gson;
import org.webbitserver.EventSourceConnection;
import org.webbitserver.EventSourceHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.netty.contrib.EventSourceMessage;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class Chatroom implements EventSourceHandler {
    private final List<EventSourceConnection> connections = new ArrayList<EventSourceConnection>();
    private final Gson json = new Gson();

    static class Outgoing {
        enum Action {JOIN, LEAVE, SAY}

        Action action;
        String username;
        String message;
    }

    @Path("/sessions")
    public class Session {
        @POST
        public Response create(String username) {
            return Response.ok().cookie(new NewCookie("username", username)).build();
        }
    }

    @Path("/messages")
    public class Message {
        @POST
        public void create(@Context HttpHeaders hh, String message) {
            // TODO: Why isn't @CookieParam working?
            Cookie cookie = hh.getCookies().get("username");
            if (cookie != null) {
                say(cookie.getValue(), message);
            } else {
                System.err.println("Someone unauthenticated tried to post a message: " + message);
            }
        }
    }

    public Object[] resources() {
        return new Object[]{new Session(), new Message()};
    }

    @Override
    public void onOpen(EventSourceConnection connection) throws Exception {
        HttpRequest httpRequest = connection.httpRequest();
        connection.data("username", httpRequest.cookieValue("username"));
        connections.add(connection);

        Outgoing outgoing = new Outgoing();
        outgoing.action = Outgoing.Action.JOIN;
        outgoing.username = (String) connection.data("username");
        broadcast(outgoing);
    }

    @Override
    public void onClose(EventSourceConnection connection) throws Exception {
        connections.remove(connection);
        Outgoing outgoing = new Outgoing();
        outgoing.action = Outgoing.Action.LEAVE;
        outgoing.username = (String) connection.data("username");
        broadcast(outgoing);
    }

    private void say(String username, String message) {
        Outgoing outgoing = new Outgoing();
        outgoing.action = Outgoing.Action.SAY;
        outgoing.username = username;
        outgoing.message = message;
        broadcast(outgoing);
    }

    private void broadcast(Outgoing outgoing) {
        String jsonStr = this.json.toJson(outgoing);
        for (EventSourceConnection connection : connections) {
            EventSourceMessage message = new EventSourceMessage(jsonStr).retry(3000L);
            connection.send(message);
        }
    }
}
