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
    public Map<String, Object> receiveWebhook(
            @RequestParam(value = "timestamp", required = false) Long timestamp,
            @RequestParam(value = "sign", required = false) String sign,
            @RequestBody String body) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (timestamp != null && sign != null) {
                boolean isValid = signUtil.verifySign(timestamp, sign);
                if (!isValid) {
                    log.warn("timestamp={}, sign={}", timestamp, sign);
                    response.put("errcode", 40001);
                    response.put("errmsg", "fail");
                    return response;
                }
                log.info("sign verify success");
            }

            log.info("receive webhook msg: {}", body);


            response.put("errcode", 0);
            response.put("errmsg", "ok");
        } catch (Exception e) {
            log.error("process web msg fail", e);
            response.put("errcode", 500);
            response.put("errmsg", "fail: " + e.getMessage());
        }

        return response;
    }
}
