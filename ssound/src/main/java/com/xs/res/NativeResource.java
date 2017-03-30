package com.xs.res;

public class NativeResource {

    public static String vadResourceName = "vad.0.1.bin";
    public static String zipResourceName = "resource.zip";


    public static String native_zip_res_path = "{"
            + "\"en.sent.score\":{\"res\": \"%s/eval/bin/eng.snt.pydnn.16bit\"}"
            + ",\"en.word.score\":{\"res\": \"%s/eval/bin/eng.wrd.pydnn.16bit\"}"
            + "}";

    public static String[] native_zip_file_names = {
            "/eval/db/GIZA.db",
            "/eval/db/comb.db",
            "/eval/db/common.bin",
            "/eval/bin/eng.snt.pydnn.16bit/eng.snt.pydnn.16bit.bin",
            "/eval/bin/eng.snt.pydnn.16bit/eng.snt.pydnn.16bit.cfg",
            "/eval/bin/eng.wrd.pydnn.16bit/eng.wrd.pydnn.16bit.bin",
            "/eval/bin/eng.wrd.pydnn.16bit/eng.wrd.pydnn.16bit.cfg"};
}
