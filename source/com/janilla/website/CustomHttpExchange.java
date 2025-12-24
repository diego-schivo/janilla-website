/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
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
package com.janilla.website;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.janilla.http.SimpleHttpExchange;
import com.janilla.http.HttpCookie;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.json.Jwt;
import com.janilla.persistence.Persistence;
import com.janilla.web.UnauthorizedException;

public class CustomHttpExchange extends SimpleHttpExchange {

	public Properties configuration;

	public Persistence persistence;

	protected final Map<String, Object> session = new HashMap<>();

	public CustomHttpExchange(HttpRequest request, HttpResponse response) {
		super(request, response);
	}

	public String sessionEmail() {
		if (!session.containsKey("sessionEmail")) {
			var t = request().getHeaderValues("cookie").map(HttpCookie::parse)
					.filter(x -> x.name().equals("janilla-website-token")).findFirst().orElse(null);
			Map<String, ?> p;
			try {
				p = t != null ? Jwt.verifyToken(t.value(), configuration.getProperty("janilla-website.jwt.key")) : null;
			} catch (IllegalArgumentException e) {
				p = null;
			}
			session.put("sessionEmail", p != null ? p.get("loggedInAs") : null);
		}
		return (String) session.get("sessionEmail");
	}

	public User sessionUser() {
		if (!session.containsKey("sessionUser")) {
			var uc = persistence.crud(User.class);
			var se = sessionEmail();
			session.put("sessionUser", uc.read(se != null ? uc.find("email", se) : 0));
		}
		return (User) session.get("sessionUser");
	}

	public void requireSessionEmail() {
		if (sessionEmail() == null)
			throw new UnauthorizedException();
	}

	public void setSessionCookie(String value) {
		var c = HttpCookie.of("janilla-website-token", value).withPath("/").withHttpOnly(true).withSameSite("Lax");
		if (value != null && !value.isEmpty())
			c = c.withExpires(ZonedDateTime.now(ZoneOffset.UTC).plusHours(2));
		else
			c = c.withMaxAge(0);
		response().setHeaderValue("set-cookie", c.format());
	}
}
