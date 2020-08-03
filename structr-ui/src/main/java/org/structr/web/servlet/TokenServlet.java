/*
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.servlet;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.resource.LoginResource;
import org.structr.web.resource.TokenResource;

public class TokenServlet extends LoginServlet{
    @Override
    protected String getUriPart() {
        return "token";
    }

    @Override
    protected LoginResource getLoginResource() {
        return new TokenResource();
    }
}
