package com.nourtech.ecareplus;

import com.nourtech.ecareplus.calling.infrastructure.livekit.FournisseurLiveKit;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class LiveKitClosureContractTests {
    @Test void fermetureSupprimeLaSalleMemeSiUnParticipantEstDejaAbsent() throws Exception {
        var methods = new ArrayList<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/twirp/livekit.RoomService/",exchange->{
            String path=exchange.getRequestURI().getPath();methods.add(path.substring(path.lastIndexOf('/')+1));
            exchange.getRequestBody().readAllBytes();
            boolean missing=path.endsWith("RemoveParticipant");
            byte[] body=(missing?"{\"code\":\"not_found\",\"msg\":\"participant does not exist\"}":"{}").getBytes();
            exchange.sendResponseHeaders(missing?404:200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        try {
            var provider=new FournisseurLiveKit("ws://127.0.0.1:"+server.getAddress().getPort(),"test-key","local-test-secret-at-least-32-bytes",true,new ObjectMapper());
            assertThatCode(()->provider.fermerSalle("private-test-room",UUID.randomUUID(),UUID.randomUUID())).doesNotThrowAnyException();
            assertThat(methods).containsExactly("RemoveParticipant","RemoveParticipant","DeleteRoom");
        } finally {server.stop(0);}
    }
}
