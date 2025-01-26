/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.signal;

import cell.core.talk.PrimitiveInputStream;
import cell.util.collection.FlexibleByteBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流存储器。
 */
public class StreamStorage {

    private final static StreamStorage instance = new StreamStorage();

    private Map<String, List<FlexibleByteBuffer>> bufferMap;

    private StreamStorage() {
        this.bufferMap = new ConcurrentHashMap<>();
    }

    public static StreamStorage getInstance() {
        return StreamStorage.instance;
    }

    public void process(PrimitiveInputStream inputStream) {
        FlexibleByteBuffer buf = new FlexibleByteBuffer();
        try {
            int length = 0;
            byte[] bytes = new byte[2048];
            while ((length = inputStream.read(bytes)) > 0) {
                buf.put(bytes, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String name = inputStream.getName();
        List<FlexibleByteBuffer> list = this.bufferMap.get(name);
        if (null == list) {
            list = new Vector<>();
            this.bufferMap.put(name, list);
        }
        list.add(buf);
    }

    private void write(String name, FlexibleByteBuffer buffer) {

    }
}
