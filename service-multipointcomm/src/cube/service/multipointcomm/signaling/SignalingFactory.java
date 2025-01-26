/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
