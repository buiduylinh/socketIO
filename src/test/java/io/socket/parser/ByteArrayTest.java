package io.socket.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ByteArrayTest {

    private static Parser.Encoder encoder = new IOParser.Encoder();

    @Test
    public void encodeByteArray() {
        Packet<JSONArray> packet = new Packet<>(Parser.BINARY_EVENT);
        packet.data = new JSONArray(asList("abc", "abc".getBytes(StandardCharsets.UTF_8)));
        packet.id = 23;
        packet.nsp = "/cool";
        Helpers.testBin(packet);
    }

    @Test
    public void encodeByteArray2() {
        Packet<JSONArray> packet = new Packet<>(Parser.BINARY_EVENT);
        packet.data = new JSONArray(asList("2", new byte[] { 0, 1 }));
        packet.id = 0;
        packet.nsp = "/";
        Helpers.testBin(packet);
    }

    @Test
    public void encodeByteArrayDeepInJson() throws JSONException {
        JSONArray data = new JSONArray("[{a: \"hi\", b: {}, c: {a: \"bye\", b: {}}}]");
        data.getJSONObject(0).getJSONObject("b").put("why", new byte[3]);
        data.getJSONObject(0).getJSONObject("c").getJSONObject("b").put("a", new byte[6]);

        Packet<JSONArray> packet = new Packet<>(Parser.BINARY_EVENT);
        packet.data = data;
        packet.id = 999;
        packet.nsp = "/deep";
        Helpers.testBin(packet);
    }

    @Test
    public void encodeDeepBinaryJSONWithNullValue() throws JSONException {
        JSONArray data = new JSONArray("[{a: \"b\", c: 4, e: {g: null}, h: null}]");
        data.getJSONObject(0).put("h", new byte[9]);

        Packet<JSONArray> packet = new Packet<>(Parser.BINARY_EVENT);
        packet.data = data;
        packet.nsp = "/";
        packet.id = 600;
        Helpers.testBin(packet);
    }

    @Test
    public void encodeBinaryAckWithByteArray() throws JSONException {
        JSONArray data = new JSONArray("[a, null, {}]");
        data.put(1, "xxx".getBytes(Charset.forName("UTF-8")));

        Packet<JSONArray> packet = new Packet<JSONArray>(Parser.BINARY_ACK);
        packet.data = data;
        packet.id = 127;
        packet.nsp = "/back";
        Helpers.testBin(packet);
    }

    @Test
    public void cleanItselfUpOnClose() {
        JSONArray data = new JSONArray();
        data.put(new byte[2]);
        data.put(new byte[3]);

        Packet<JSONArray> packet = new Packet<JSONArray>(Parser.BINARY_EVENT);
        packet.data = data;
        packet.id = 0;
        packet.nsp = "/";

        encoder.encode(packet, new Parser.Encoder.Callback() {
            @Override
            public void call(final Object[] encodedPackets) {
                final IOParser.Decoder decoder = new IOParser.Decoder();
                decoder.onDecoded(new Parser.Decoder.Callback() {
                    @Override
                    public void call(Packet packet) {
                        throw new RuntimeException("received a packet when not all binary data was sent.");
                    }
                });

                decoder.add((String)encodedPackets[0]);
                decoder.add((byte[]) encodedPackets[1]);
                decoder.destroy();
                assertThat(decoder.reconstructor.buffers.size(), is(0));
            }
        });
    }
}
