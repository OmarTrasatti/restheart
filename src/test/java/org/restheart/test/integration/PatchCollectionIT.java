/*
 * RESTHeart - the Web API for MongoDB
 * Copyright (C) SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.test.integration;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import io.undertow.util.Headers;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import static org.junit.Assert.*;
import org.junit.Test;
import org.restheart.hal.Representation;
import static org.restheart.test.integration.HttpClientAbstactIT.adminExecutor;
import org.restheart.utils.HttpStatus;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class PatchCollectionIT extends HttpClientAbstactIT {

    public PatchCollectionIT() {
    }

    @Test
    public void testPatchCollection() throws Exception {
        Response resp;

        // *** PUT tmpdb
        resp = adminExecutor.execute(Request.Put(dbTmpUri).bodyString("{a:1}", halCT).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE));
        check("check put db", resp, HttpStatus.SC_CREATED);

        // *** PUT tmpcoll
        resp = adminExecutor.execute(Request.Put(collectionTmpUri).bodyString("{a:1}", halCT).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE));
        check("check put coll1", resp, HttpStatus.SC_CREATED);

        // try to patch without body
        resp = adminExecutor.execute(Request.Patch(collectionTmpUri).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE));
        check("check patch tmp doc without data", resp, HttpStatus.SC_NOT_ACCEPTABLE);

        // try to patch without etag forcing etag check
        resp = adminExecutor.execute(Request.Patch(addCheckEtag(collectionTmpUri)).bodyString("{a:1}", halCT).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE));
        check("check patch tmp doc without etag forcing checkEtag", resp, HttpStatus.SC_CONFLICT);

        // try to patch without etag without etag check
        resp = adminExecutor.execute(Request.Patch(collectionTmpUri).bodyString("{a:1}", halCT).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE));
        check("check patch tmp doc without etag", resp, HttpStatus.SC_OK);

        // try to patch with wrong etag
        resp = adminExecutor.execute(Request.Patch(collectionTmpUri).bodyString("{a:1}", halCT).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE).addHeader(Headers.IF_MATCH_STRING, "pippoetag"));
        check("check patch tmp doc with wrong etag", resp, HttpStatus.SC_PRECONDITION_FAILED);

        resp = adminExecutor.execute(Request.Get(collectionTmpUri).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE));

        JsonObject content = Json.parse(resp.returnContent().asString()).asObject();

        String etag = content.get("_etag").asObject().get("$oid").asString();

        // try to patch with correct etag
        resp = adminExecutor.execute(Request.Patch(collectionTmpUri).bodyString("{b:2}", halCT).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE).addHeader(Headers.IF_MATCH_STRING, etag));
        check("check patch tmp doc with correct etag", resp, HttpStatus.SC_OK);

        resp = adminExecutor.execute(Request.Get(collectionTmpUri).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE));

        content = Json.parse(resp.returnContent().asString()).asObject();
        assertNotNull("check patched content", content.get("a"));
        assertNotNull("check patched content", content.get("b"));
        assertTrue("check patched content", content.get("a").asInt() == 1 && content.get("b").asInt() == 2);
        etag = content.get("_etag").asObject().get("$oid").asString();

        // try to patch reserved field name
        resp = adminExecutor.execute(Request.Patch(collectionTmpUri).bodyString("{'_embedded':'a', 'a': 1}", halCT).addHeader(Headers.CONTENT_TYPE_STRING, Representation.HAL_JSON_MEDIA_TYPE).addHeader(Headers.IF_MATCH_STRING, etag));
        content = Json.parse(resp.returnContent().asString()).asObject();
        assertNotNull("check patched content", content.get("_embedded").asObject().get("rh:warnings").asArray());

    }
}
