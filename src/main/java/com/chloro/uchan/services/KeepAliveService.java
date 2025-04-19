package com.chloro.uchan.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class KeepAliveService {

    @Scheduled(fixedRate = 1000 * 60)
    public void keepAlive() {
        System.out.println("Heartbeat: " + SimpleDateFormat.getDateTimeInstance().format(System.currentTimeMillis()));
    }

}
