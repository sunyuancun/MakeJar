package com.tt;

class NativeResource {

    static String vadResourceName = "vad.0.1.bin";
    static String zipResourceName = "resource.zip";


    static String native_zip_res_path = "{"
            + "\"en.sent.score\":{\"res\": \"%s/eval/bin/eng.snt.pydnn.16bit\"}"
            + ",\"en.word.score\":{\"res\": \"%s/eval/bin/eng.wrd.pydnn.16bit\"}"
            + "}";

    static String[] native_zip_file_names = {
            "/eval/db/comb.db",
            "/eval/db/common.bin",
            "/eval/bin/eng.snt.pydnn.16bit/eng.snt.pydnn.16bit.bin",
            "/eval/bin/eng.snt.pydnn.16bit/eng.snt.pydnn.16bit.cfg",
            "/eval/bin/eng.wrd.pydnn.16bit/eng.wrd.pydnn.16bit.bin",
            "/eval/bin/eng.wrd.pydnn.16bit/eng.wrd.pydnn.16bit.cfg"};
}
