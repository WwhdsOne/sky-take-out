package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MyTask {

    @Scheduled(cron = "0 * * * * ?")
    public void excuteTask(){
        log.info("LOL:{}", LocalDateTime.now());
    }
}
