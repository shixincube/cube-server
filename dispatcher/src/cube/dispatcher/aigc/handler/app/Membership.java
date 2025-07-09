/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.aigc.handler.AIGCHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 会员信息数据。
 */
public class Membership extends ContextHandler {

    public Membership() {
        super("/app/membership/");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) {
            try {
                String token = this.getApiToken(request);
                if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                    this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                    this.complete();
                    return;
                }

                JSONObject memberCardConfig = new JSONObject();
                memberCardConfig.put("ordinaryMonthlyPrice", 49.9);
                memberCardConfig.put("ordinaryMonthlyPublishedPrice", 72.0);
                memberCardConfig.put("ordinaryAnnualPrice", 399.0);
                memberCardConfig.put("ordinaryAnnualPublishedPrice", 792.0);
                memberCardConfig.put("premiumMonthlyPrice", 59.9);
                memberCardConfig.put("premiumMonthlyPublishedPrice", 86.0);
                memberCardConfig.put("premiumAnnualPrice", 499.0);
                memberCardConfig.put("premiumAnnualPublishedPrice", 946.0);

                JSONObject responseData = new JSONObject();
                responseData.put("memberCardConfig", memberCardConfig);

                this.respondOk(response, responseData);
                this.complete();
            } catch (Exception e) {
                Logger.w(this.getClass(), "#doGet", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            try {
                String token = this.getApiToken(request);
                if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                    this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                    this.complete();
                    return;
                }

                JSONObject data = this.readBodyAsJSONObject(request);
                String channel = data.getString("channel");
                String invitation = data.getString("invitation");

                cube.common.entity.Membership membership = Manager.getInstance().activateMembership(token, channel, invitation);
                this.respondOk(response, membership.toCompactJSON());
                this.complete();
            } catch (Exception e) {
                Logger.w(this.getClass(), "#doPost", e);
                this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                this.complete();
            }
        }
    }
}
