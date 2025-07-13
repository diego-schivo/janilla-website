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
import java.util.stream.Collectors;

import com.janilla.http.HeaderField;
import com.janilla.http.Http;
import com.janilla.http.HttpCookie;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.json.Jwt;
import com.janilla.persistence.Persistence;
import com.janilla.web.UnauthorizedException;

public class CustomHttpExchange extends HttpExchange.Base {

	public Properties configuration;

	public Persistence persistence;

	private Map<String, Object> map = new HashMap<>();

	public CustomHttpExchange(HttpRequest request, HttpResponse response) {
		super(request, response);
	}

	public String sessionEmail() {
		if (!map.containsKey("sessionEmail")) {
			var hh = request().getHeaders();
			var h = hh.stream().filter(x -> x.name().equals("cookie")).map(HeaderField::value)
					.collect(Collectors.joining("; "));
			var cc = h != null ? Http.parseCookieHeader(h) : null;
			var t = cc != null ? cc.get("janilla-website-token") : null;
			Map<String, ?> p;
			try {
				p = t != null ? Jwt.verifyToken(t, configuration.getProperty("janilla-website.jwt.key")) : null;
			} catch (IllegalArgumentException e) {
				p = null;
			}
			map.put("sessionEmail", p != null ? p.get("loggedInAs") : null);
		}
		return (String) map.get("sessionEmail");
	}

	public User sessionUser() {
		if (!map.containsKey("sessionUser")) {
			var uc = persistence.crud(User.class);
			var se = sessionEmail();
			map.put("sessionUser", uc.read(se != null ? uc.find("email", se) : 0));
		}
		return (User) map.get("sessionUser");
	}

	public void requireSessionEmail() {
		if (sessionEmail() == null)
			throw new UnauthorizedException();
	}

	public void setSessionCookie(String value) {
		var c = HttpCookie.of("janilla-website-token", value).withPath("/").withHttpOnly(true).withSameSite("Lax");
		if (value != null && value.length() > 0)
			c = c.withExpires(ZonedDateTime.now(ZoneOffset.UTC).plusHours(2));
		else
			c = c.withMaxAge(0);
		response().setHeaderValue("set-cookie", c.format());
	}

	@Override
	public HttpExchange withException(Exception exception) {
		this.exception = exception;
		return this;
	}
}
