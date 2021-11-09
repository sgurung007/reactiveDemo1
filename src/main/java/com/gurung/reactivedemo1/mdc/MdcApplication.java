package com.gurung.reactivedemo1.mdc;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootApplication
@Log4j2
@RestController
public class MdcApplication {
    private final ResturantService resturantService;

    public MdcApplication(ResturantService resturantService) {
        this.resturantService = resturantService;
    }

    public static void main(String[] args) {
        SpringApplication.run(MdcApplication.class, args);
    }

    @GetMapping("/{uid}/restaurants/{price}")
    Flux<Resturant> resturantFlux(@PathVariable String uid, @PathVariable Double price) {
        return adaptResults(this.resturantService.getByMaxPrice(price), uid, price);
    }

    Flux<Resturant> adaptResults(Flux<Resturant> in, String uid, double price) {
        return Mono.just(String.format("finding resturant having price lower than $%.2f for %s", price, uid))
                .doOnEach(logOnNext(log::info))
                .thenMany(in)
                .doOnEach(logOnNext(r -> log.info("found resturant {} for ${}", r.getName(), r.getPricePerPerson())))
                .subscriberContext(Context.of("uid", uid));
    }

    private final static String UID = "uid";

    private static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
        return signal -> {
            if (!signal.isOnNext()) {
                return;
            }

            Optional<String> uidOptional = signal.getContext().getOrEmpty(UID);

            Runnable orElse = () -> logStatement.accept(signal.get());

            Consumer<String> ifPresent = uid -> {
                try (MDC.MDCCloseable closeable = MDC.putCloseable(UID, uid)) {
                    orElse.run();
                    ;
                }
            };

            uidOptional.ifPresentOrElse(ifPresent, orElse);
        };

    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Resturant {
    private double pricePerPerson;
    private String name;
}

@Service
class ResturantService {

    private final Collection<Resturant> resturants = new ConcurrentSkipListSet<>(new Comparator<Resturant>() {
        @Override
        public int compare(Resturant o1, Resturant o2) {
            Double one = o1.getPricePerPerson();
            Double two = o2.getPricePerPerson();

            return one.compareTo(two);
        }
    });

    ResturantService() {
        IntStream.range(0, 1000)
                .mapToObj(Integer::toString)
                .map(i -> "resturant # " + i)
                .map(str -> new Resturant(new Random().nextDouble() * 100, str))
                .forEach(this.resturants::add);
    }

    Flux<Resturant> getByMaxPrice(double maxPrice) {
        Stream<Resturant> res = this.resturants.parallelStream()
                .filter(resturant -> resturant.getPricePerPerson() <= maxPrice);
        return Flux.fromStream(res);
    }
}