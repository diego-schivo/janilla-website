/*
 * MIT License
 *
 * Copyright (c) 2024 Diego Schivo
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

import java.util.Map;
import java.util.function.Supplier;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpServer;
import com.janilla.util.EntryList;
import com.janilla.util.Lazy;

class CustomHttpServer extends HttpServer {

	JanillaWebsite application;

	Supplier<Map<String, Supplier<HttpExchange>>> hostExchange = Lazy.of(() -> {
		return Map.of(application.getConfiguration().getProperty("website.commerce.host"),
				() -> application.getCommerce().new Exchange(),
				application.getConfiguration().getProperty("website.conduit.backend.host"),
				() -> application.getConduitBackend().new Exchange(),
				application.getConfiguration().getProperty("website.eshopweb.api.host"),
				() -> application.getEShopApi().new Exchange());
	});

	public void setApplication(JanillaWebsite application) {
		this.application = application;
	}

	@Override
	protected HttpExchange createExchange(HttpRequest request) {
		EntryList<String, String> hh;
		try {
			hh = request.getHeaders();
		} catch (NullPointerException e) {
			hh = null;
		}
		var h = hh != null ? hh.get("Host") : null;
		var s = h != null ? hostExchange.get().get(h) : null;
		return s != null ? s.get() : super.createExchange(request);
	}
}
