/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CypherBridgeProtocolTest {
    @Test
    void shouldParseBridgeRequest() {
        String raw = "{\"channel\":\"ja.query.execute\",\"payload\":{\"query\":\"MATCH (n) RETURN n\"}}";
        CypherBridgeProtocol.BridgeRequest request = CypherBridgeProtocol.parseRequest(raw);
        Assertions.assertEquals("ja.query.execute", request.channel());
        Assertions.assertEquals("MATCH (n) RETURN n", request.payload().getString("query"));
    }

    @Test
    void shouldRejectInvalidRequest() {
        CypherBridgeProtocol.BridgeException ex = Assertions.assertThrows(
                CypherBridgeProtocol.BridgeException.class,
                () -> CypherBridgeProtocol.parseRequest("{}")
        );
        Assertions.assertEquals("bridge_invalid_request", ex.code());
    }

    @Test
    void shouldEncodeSuccessAndErrorEnvelope() {
        String success = CypherBridgeProtocol.success("ja.script.list", new JSONObject());
        JSONObject successObj = JSON.parseObject(success);
        Assertions.assertTrue(successObj.getBooleanValue("ok"));
        Assertions.assertEquals("ja.script.list", successObj.getString("channel"));

        String error = CypherBridgeProtocol.error("ja.query.execute", "cypher_query_error", "failed");
        JSONObject errorObj = JSON.parseObject(error);
        Assertions.assertFalse(errorObj.getBooleanValue("ok"));
        Assertions.assertEquals("cypher_query_error", errorObj.getString("code"));
        Assertions.assertEquals("failed", errorObj.getString("message"));
    }
}
