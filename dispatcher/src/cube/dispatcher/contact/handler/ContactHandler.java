/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.contact.handler;

import cube.dispatcher.Performer;
import cube.util.CrossDomainHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * RESTful 接口基类。
 */
public abstract class ContactHandler extends CrossDomainHandler {

    protected final Performer performer;

    public ContactHandler(Performer performer) {
        super();
        this.performer = performer;
    }

    protected String getApiToken(HttpServletRequest request) {
        String token = this.getLastRequestPath(request);
        if (null == token || token.length() < 32) {
            try {
                token = request.getHeader("x-baize-api-token").trim();
            } catch (Exception e) {
                // Nothing
            }
        }
        return token;
    }

    protected String getRequestPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path.length() < 2) {
            return null;
        }

        path = path.substring(1).trim();
        int index = path.indexOf("/");
        if (index > 0) {
            path = path.substring(0, index);
        }
        return path;
    }

    protected String getLastRequestPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path.length() < 2) {
            return null;
        }

        path = path.substring(1).trim();
        String[] paths = path.split("/");
        path = paths[paths.length - 1];
        return path.trim();
    }
}
