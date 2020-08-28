package org.eclipse.californium.benchmark;

import java.security.GeneralSecurityException;

import org.eclipse.leshan.core.json.JsonRootObject;
import org.eclipse.leshan.core.json.LwM2mJsonException;
import org.eclipse.leshan.core.json.jackson.LwM2mJsonJacksonEncoderDecoder;
import org.eclipse.leshan.core.json.minimaljson.LwM2mJsonMinimalEncoderDecoder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class Lwm2mJsonBench {

    @State(Scope.Benchmark)
    public static class MyState {

        public LwM2mJsonMinimalEncoderDecoder minimal;
        public LwM2mJsonJacksonEncoderDecoder jackson;
        public String smallPayload;
        public String payload;
        public String bigPayload;
        public JsonRootObject smallRootObject;
        public JsonRootObject rootObject;
        public JsonRootObject bigRootObject;

        public MyState() {
            try {

                // decoder
                minimal = new LwM2mJsonMinimalEncoderDecoder();
                jackson = new LwM2mJsonJacksonEncoderDecoder();

                // string to decode
                StringBuilder b;
                b = new StringBuilder();
                b.append("{\"e\":[");
                b.append("{\"n\":\"1/2\",\"v\":24.1,\"t\":-50}],");
                b.append("\"bt\":25462634}");
                smallPayload = b.toString();
                smallRootObject = jackson.fromJsonLwM2m(smallPayload);
                minimal.fromJsonLwM2m(smallPayload);

                b = new StringBuilder();
                b.append("{\"e\":[");
                b.append("{\"n\":\"0\",\"sv\":\"Open Mobile Alliance\"},");
                b.append("{\"n\":\"1\",\"sv\":\"Lightweight M2M Client\"},");
                b.append("{\"n\":\"2\",\"sv\":\"345000123\"},");
                b.append("{\"n\":\"3\",\"sv\":\"1.0\"},");
                b.append("{\"n\":\"6/0\",\"v\":1},");
                b.append("{\"n\":\"6/1\",\"v\":5},");
                b.append("{\"n\":\"7/0\",\"v\":3800},");
                b.append("{\"n\":\"7/1\",\"v\":5000},");
                b.append("{\"n\":\"8/0\",\"v\":125},");
                b.append("{\"n\":\"8/1\",\"v\":900},");
                b.append("{\"n\":\"9\",\"v\":100},");
                b.append("{\"n\":\"10\",\"v\":15},");
                b.append("{\"n\":\"11/0\",\"v\":0},");
                b.append("{\"n\":\"13\",\"v\":1.367491215E9},");
                b.append("{\"n\":\"14\",\"sv\":\"+02:00\"},");
                b.append("{\"n\":\"15\",\"sv\":\"U\"}]}");
                payload = b.toString();
                rootObject = jackson.fromJsonLwM2m(payload);
                minimal.fromJsonLwM2m(payload);

                b = new StringBuilder();
                b.append("{\"e\":[");
                b.append("{\"n\":\"0\",\"sv\":\"Open Mobile Alliance\"},");
                b.append("{\"n\":\"1\",\"sv\":\"Lightweight M2M Client\"},");
                b.append("{\"n\":\"2\",\"sv\":\"345000123\"},");
                b.append("{\"n\":\"3\",\"sv\":\"1.0\"},");
                b.append("{\"n\":\"6/0\",\"v\":1},");
                b.append("{\"n\":\"6/1\",\"v\":5},");
                for (int i = 0; i < 1000; i++) {
                    b.append("{\"n\":\"7/");
                    b.append(i);
                    b.append("\",\"v\":");
                    b.append(i);
                    b.append("},");
                }
                b.append("{\"n\":\"7/0\",\"v\":3800},");
                b.append("{\"n\":\"8/0\",\"v\":125},");
                b.append("{\"n\":\"8/1\",\"v\":900},");
                b.append("{\"n\":\"9\",\"v\":100},");
                b.append("{\"n\":\"10\",\"v\":15},");
                b.append("{\"n\":\"11/0\",\"v\":0},");
                b.append("{\"n\":\"13\",\"v\":1.367491215E9},");
                b.append("{\"n\":\"14\",\"sv\":\"+02:00\"},");
                b.append("{\"n\":\"15\",\"sv\":\"U\"}]}");
                bigPayload = b.toString();
                bigRootObject = jackson.fromJsonLwM2m(bigPayload);
                minimal.fromJsonLwM2m(bigPayload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Benchmark
    public void jacksonDecodeSmall(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.jackson.fromJsonLwM2m(state.smallPayload);
    }

    @Benchmark
    public void jacksonDecode(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.jackson.fromJsonLwM2m(state.payload);
    }

    @Benchmark
    public void jacksonDecodeBig(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.jackson.fromJsonLwM2m(state.bigPayload);
    }

    @Benchmark
    public void minimalDecodeSmall(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.minimal.fromJsonLwM2m(state.smallPayload);
    }

    @Benchmark
    public void minimalDecode(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.minimal.fromJsonLwM2m(state.payload);
    }

    @Benchmark
    public void minimalDecodeBig(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.minimal.fromJsonLwM2m(state.bigPayload);
    }

    @Benchmark
    public void jacksonEncodeSmall(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.jackson.toJsonLwM2m(state.smallRootObject);
    }

    @Benchmark
    public void jacksonEncode(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.jackson.toJsonLwM2m(state.rootObject);
    }

    @Benchmark
    public void jacksonEncodeBig(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.jackson.toJsonLwM2m(state.bigRootObject);
    }

    @Benchmark
    public void minimalEncodeSmall(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.minimal.toJsonLwM2m(state.smallRootObject);
    }

    @Benchmark
    public void minimalEncode(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.minimal.toJsonLwM2m(state.rootObject);
    }

    @Benchmark
    public void minimalEncodeBig(MyState state) throws GeneralSecurityException, LwM2mJsonException {
        state.minimal.toJsonLwM2m(state.bigRootObject);
    }
}
