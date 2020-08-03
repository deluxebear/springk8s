package com.jetems.springk8s;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscoveryService {
    private final DiscoveryClient discoveryClient;

    public DiscoveryService(DiscoveryClient discoveryClient){
        this.discoveryClient=discoveryClient;
    }

    @Scheduled(fixedDelay = 1000)
    public void getServiceInstance(){
        List<String> services=discoveryClient.getServices();
        for (String service:services
             ) {
            System.out.println("---------service name---------: "+service);
        }
    }
}
