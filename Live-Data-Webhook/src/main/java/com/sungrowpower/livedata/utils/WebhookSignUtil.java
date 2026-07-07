package com.sungrowpower.livedata.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

@Component
public class WebhookSignUtil {

    @Value("${sungrow.webhook.secret}")
    private String secret;

    public String generateSign(long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = new String(Base64.encodeBase64(signData), StandardCharsets.UTF_8);
        return URLEncoder.encode(sign, "UTF-8");
    }

    public boolean verifySign(long timestamp, String sign) throws Exception {
        String expectedSign = generateSign(timestamp);
        return expectedSign.equals(sign);
    }
}
