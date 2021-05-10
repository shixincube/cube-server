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

package cube.service.multipointcomm;

import cell.util.log.Logger;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.state.MultipointCommStateCode;
import org.json.JSONObject;
import org.kurento.client.KurentoClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Kurento 单元。
 */
public class KurentoCompositeMediaUnit extends AbstractCompositeMediaUnit {

    private final MultipointCommService service;

    private final Portal portal;

    private final String url;

    private final ExecutorService executor;

    private KurentoClient kurentoClient;

    /**
     * Comm Field 对应的 Media Pipeline 。
     */
    private final ConcurrentHashMap<Long, KurentoMediaPipelineWrapper> pipelineMap;

    public KurentoCompositeMediaUnit(MultipointCommService service, Portal portal, String url, ExecutorService executor) {
        this.service = service;
        this.portal = portal;
        this.url = url;
        this.executor = executor;

        try {
            this.kurentoClient = KurentoClient.create(url);
        } catch (Throwable e) {
            Logger.e(this.getClass(), "", e);
        }

        this.pipelineMap = new ConcurrentHashMap<>();
    }

    @Override
    public void preparePipeline(CommField commField, CommFieldEndpoint endpoint) {

    }

    @Override
    public MultipointCommStateCode subscribe(CommField commField, CommFieldEndpoint endpoint, CommFieldEndpoint target, String offerSDP, MediaUnitCallback callback) {
        return null;
    }

    @Override
    public MultipointCommStateCode unsubscribe(CommField commField, CommFieldEndpoint endpoint, CommFieldEndpoint target, MediaUnitCallback callback) {
        return null;
    }

    @Override
    public MultipointCommStateCode addCandidate(CommField commField, CommFieldEndpoint endpoint, CommFieldEndpoint target, JSONObject candidate) {
        return null;
    }

    @Override
    public MultipointCommStateCode removeEndpoint(CommField commField, CommFieldEndpoint endpoint) {
        return null;
    }

    @Override
    public MultipointCommStateCode release(CommField commField) {
        return null;
    }

    @Override
    public void destroy() {

    }
}
