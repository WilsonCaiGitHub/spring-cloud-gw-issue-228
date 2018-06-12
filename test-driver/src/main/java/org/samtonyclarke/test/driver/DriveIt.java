package org.samtonyclarke.test.driver;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class DriveIt
{
    private AtomicInteger failed = new AtomicInteger();

    private AtomicInteger passed = new AtomicInteger();

    public static void main(String args[]) throws InterruptedException
    {
        SpringApplication.run(DriveIt.class, args);
    }

    
    public DriveIt()
    {
        Flux<Integer> intFlux = Flux.range(1, 20000000);
        intFlux.parallel().subscribe(i -> {
            callOnce();
            if (passed.get() % 1000 == 0)
            {
                System.out.println("Processed " + passed.get() + " calls with success");
                System.out.println("Processed " + failed.get() + " calls with error");
            }
            if (failed.get() > 100)
            {
                System.out.println("Ending test due to high error count: "+failed.get());
                System.exit(-1);
            }
        });
    }
    
    private void callOnce()
    {
        Mono<ClientResponse> exchange = WebClient.create("http://localhost:8060").get().uri("integrationTest/1.0/app-name")
                .exchange().doOnSuccess(f -> passed.incrementAndGet());
        exchange.subscribe(response -> {
            HttpStatus statusCode = response.statusCode();
            if (!statusCode.is2xxSuccessful())
            {
                // error has occurred we can bail out
                System.out.println("Not 2xx success call");
                System.out.println("Status Code: " + statusCode);
                failed.incrementAndGet();
            }
        }, t -> {
            System.out.println("Error on subscribe");
            t.printStackTrace();
            failed.incrementAndGet();
        });
    }

}
