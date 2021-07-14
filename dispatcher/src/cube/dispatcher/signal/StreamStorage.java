/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
}
