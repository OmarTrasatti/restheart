/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.handlers.collection;

import com.mongodb.BasicDBObject;
import com.softinstigate.restheart.db.CollectionDAO;
import com.softinstigate.restheart.handlers.injectors.LocalCachesSingleton;
import com.softinstigate.restheart.handlers.PipedHttpHandler;
import com.softinstigate.restheart.utils.HttpStatus;
import com.softinstigate.restheart.handlers.RequestContext;
import com.softinstigate.restheart.handlers.document.DocumentRepresentationFactory;
import com.softinstigate.restheart.utils.RequestHelper;
import com.softinstigate.restheart.utils.ResponseHelper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uji
 */
public class DeleteCollectionHandler extends PipedHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(DeleteCollectionHandler.class);

    /**
     * Creates a new instance of DeleteCollectionHandler
     */
    public DeleteCollectionHandler() {
        super(null);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        ObjectId etag = RequestHelper.getWriteEtag(exchange);

        if (etag == null) {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_CONFLICT, "the " + Headers.ETAG + " header must be provided");
            logger.warn("error. you must provide the {} header", Headers.ETAG);
            return;
        }

        int SC = CollectionDAO.deleteCollection(context.getDBName(), context.getCollectionName(), etag);

        // send the warnings if any (and in case no_content change the return code to ok
        if (context.getWarnings() != null && !context.getWarnings().isEmpty()) {
            if (SC == HttpStatus.SC_NO_CONTENT) {
                exchange.setResponseCode(HttpStatus.SC_OK);
            }
            else {
                exchange.setResponseCode(SC);
            }

            DocumentRepresentationFactory.sendDocument(exchange.getRequestPath(), exchange, context, new BasicDBObject());
        }
        else {
            exchange.setResponseCode(SC);
        }

        exchange.endExchange();

        LocalCachesSingleton.getInstance().invalidateCollection(context.getDBName(), context.getCollectionName());
    }
}
