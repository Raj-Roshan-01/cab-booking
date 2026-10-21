package com.cab.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class Notifier {
    private final SimpMessagingTemplate template;
    public void send(String destination, Object payload) { template.convertAndSend(destination, payload); }
}
