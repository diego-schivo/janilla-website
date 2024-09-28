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

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import com.janilla.http.HttpRequest;
import com.janilla.http.HttpProtocol;
import com.janilla.http.HeaderField;
import com.janilla.http.HttpExchange;
import com.janilla.util.Lazy;

public class CustomHttp2Protocol extends HttpProtocol {

	public Properties configuration;

	public JanillaWebsiteApp application;

	Supplier<Map<String, Supplier<HttpExchange>>> hostExchange = Lazy.of(() -> {
		return Map.of(configuration.getProperty("website.acmestore.host"),
				() -> application.getAcmeStore().getFactory().create(HttpExchange.class),
				configuration.getProperty("website.conduit.backend.host"),
				() -> application.getConduitBackend().getFactory().create(HttpExchange.class),
				configuration.getProperty("website.eshopweb.api.host"),
				() -> application.getEShopApi().getFactory().create(HttpExchange.class),
				configuration.getProperty("website.foodadvisor.api.host"),
				() -> application.getFoodAdvisorApi().getFactory().create(HttpExchange.class),
				configuration.getProperty("website.foodadvisor.client.host"),
				() -> application.getFoodAdvisorClient().getFactory().create(HttpExchange.class),
				configuration.getProperty("website.mystore.storefront.host"),
				() -> application.getMyStoreStorefront().getFactory().create(HttpExchange.class),
				configuration.getProperty("website.paymentcheckout.host"),
				() -> application.getPaymentCheckout().getFactory().create(HttpExchange.class),
				configuration.getProperty("website.petclinic.host"),
				() -> application.getPetClinic().getFactory().create(HttpExchange.class));
	});

	@Override
	protected HttpExchange createExchange(HttpRequest request) {
		Collection<HeaderField> hh;
		try {
			hh = request.getHeaders();
		} catch (NullPointerException e) {
			hh = null;
		}
		var h = hh != null
				? hh.stream().filter(x -> x.name().equals("Host")).map(HeaderField::value).findFirst().orElse(null)
				: null;
		var s = h != null ? hostExchange.get().get(h) : null;
		return s != null ? s.get() : super.createExchange(request);
	}
}
