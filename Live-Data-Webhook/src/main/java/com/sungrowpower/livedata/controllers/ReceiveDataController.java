package com.sungrowpower.livedata.controllers;

import com.sungrowpower.livedata.utils.WebhookSignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/webhook")
public class ReceiveDataController {

    private static final Logger log = LoggerFactory.getLogger(ReceiveDataController.class);

    @Autowired
    private WebhookSignUtil signUtil;

    @PostMapping("/receive")
    public void receiveDataWithoutSign(@RequestBody String body) {
        log.info("receive webhook msg: {}", body);
    }

    @PostMapping("/receive/sign")
    public void receiveWebhook(
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "sign", required = false) String sign,
            @RequestBody String body) {

        try {
            if (timestamp != null && sign != null) {
                boolean isValid = signUtil.verifySign(Long.valueOf(timestamp), sign);
                if (!isValid) {
                    log.warn("timestamp={}, sign={}", timestamp, sign);
                }
                log.info("sign verify success");
            }

            log.info("receive webhook msg: {}", body);


        } catch (Exception e) {
            log.error("process web msg fail", e);
        }
    }
}
