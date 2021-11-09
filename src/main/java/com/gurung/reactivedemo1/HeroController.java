package com.gurung.reactivedemo1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

import java.util.UUID;
import java.util.function.Consumer;
@Slf4j
@RestController
@RequestMapping("/hero")
public class HeroController {

    @GetMapping("/get-hero")
    public Hero getHero(){
        Hero hero1 = Hero.builder().id(1).name("name1").build();
        return hero1;
    }

}
