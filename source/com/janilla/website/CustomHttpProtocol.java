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

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpProtocol;
import com.janilla.http.HttpRequest;

public class CustomHttpProtocol extends HttpProtocol {

	public Properties configuration;

	public JanillaWebsite application;

	public CustomHttpProtocol(HttpHandler handler, SSLContext sslContext, boolean useClientMode) {
		super(handler, sslContext, useClientMode);
	}

	Supplier<Map<String, Supplier<HttpExchange>>> hostExchange = Lazy.of(() -> {
		return Map.of(configuration.getProperty("website.acmedashboard.host"),
				() -> application.acmeDashboard.factory.create(HttpExchange.class),
//				configuration.getProperty("website.acmestore.host"),
//				() -> application.acmeStore.factory.create(HttpExchange.class),
				configuration.getProperty("website.address-book.host"),
				() -> application.addressBook.factory.create(HttpExchange.class),
				configuration.getProperty("website.conduit.backend.host"),
				() -> application.conduitBackend.factory.create(HttpExchange.class),
//				configuration.getProperty("website.eshopweb.api.host"),
//				() -> application.eShopApi.factory.create(HttpExchange.class),
//				configuration.getProperty("website.foodadvisor.api.host"),
//				() -> application.foodAdvisorApi.factory.create(HttpExchange.class),
//				configuration.getProperty("website.foodadvisor.client.host"),
//				() -> application.foodAdvisorClient.factory.create(HttpExchange.class),
//				configuration.getProperty("website.mystore.storefront.host"),
//				() -> application.myStoreStorefront.factory.create(HttpExchange.class),
//				configuration.getProperty("website.paymentcheckout.host"),
//				() -> application.paymentCheckout.factory.create(HttpExchange.class),
				configuration.getProperty("website.petclinic.host"),
				() -> application.petClinic.factory.create(HttpExchange.class));
	});

	@Override
	protected HttpExchange createExchange(HttpRequest request) {
		var s = hostExchange.get().get(request.getAuthority());
		return s != null ? s.get() : super.createExchange(request);
	}
}
