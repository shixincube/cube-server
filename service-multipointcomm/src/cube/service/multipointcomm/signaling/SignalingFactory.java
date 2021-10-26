/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.service.multipointcomm.signaling;

import cube.common.action.MultipointCommAction;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 信令工厂。
 */
public class SignalingFactory {

    private final static SignalingFactory instance = new SignalingFactory();

    private SignalingFactory() {
    }

    public final static SignalingFactory getInstance() {
        return SignalingFactory.instance;
    }

    public Signaling createSignaling(JSONObject json) {
        try {
            String name = json.getString("name");
            if (MultipointCommAction.Offer.name.equals(name)) {
                OfferSignaling offerSignaling = new OfferSignaling(json);
                return offerSignaling;
            }
            else if (MultipointCommAction.Answer.name.equals(name)) {
                AnswerSignaling answerSignaling = new AnswerSignaling(json);
                return answerSignaling;
            }
            else if (MultipointCommAction.Candidate.name.equals(name)) {
                CandidateSignaling candidateSignaling = new CandidateSignaling(json);
                return candidateSignaling;
            }
            else if (MultipointCommAction.Bye.name.equals(name)) {
                ByeSignaling byeSignaling = new ByeSignaling(json);
                return byeSignaling;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
