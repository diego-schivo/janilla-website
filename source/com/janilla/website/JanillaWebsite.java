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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import com.janilla.acmedashboard.AcmeDashboard;
import com.janilla.addressbook.AddressBook;
import com.janilla.conduit.backend.ConduitBackend;
import com.janilla.conduit.frontend.ConduitFrontend;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpProtocol;
import com.janilla.net.Net;
import com.janilla.net.Server;
import com.janilla.petclinic.PetClinicApplication;
import com.janilla.reflect.Factory;
import com.janilla.todomvc.TodoMvc;
import com.janilla.util.Util;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class JanillaWebsite {

	public static void main(String[] args) {
		try {
			var pp = new Properties();
			try (var is = JanillaWebsite.class.getResourceAsStream("configuration.properties")) {
				pp.load(is);
				if (args.length > 0) {
					var p = args[0];
					if (p.startsWith("~"))
						p = System.getProperty("user.home") + p.substring(1);
					pp.load(Files.newInputStream(Path.of(p)));
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			var a = new JanillaWebsite(pp);

			var hp = a.factory.create(HttpProtocol.class);
			var p = a.configuration.getProperty("website.ssl.keystore.path");
			var q = a.configuration.getProperty("website.ssl.keystore.password");
			if (p != null && p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			try (var is = p != null && p.length() > 0 ? Files.newInputStream(Path.of(p))
					: Net.class.getResourceAsStream("testkeys")) {
				hp.setSslContext(Net.getSSLContext(p != null && p.toLowerCase().endsWith(".p12") ? "PKCS12" : "JKS", is,
						(q != null && q.length() > 0 ? q : "passphrase").toCharArray()));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			hp.setHandler(a.handler);

			var s = new Server();
			s.setAddress(new InetSocketAddress(Integer.parseInt(a.configuration.getProperty("website.server.port"))));
			s.setProtocol(hp);
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Properties configuration;

	public Factory factory;

	public HttpHandler handler;

	public AcmeDashboard acmeDashboard;

//	public AcmeStoreApp acmeStore;

	public AddressBook addressBook;

	public ConduitBackend conduitBackend;

	public ConduitFrontend conduitFrontend;

//	public EShopApiApp eShopApi;
//
//	public EShopWebApp eShopWeb;
//
//	public FoodAdvisorApiApp foodAdvisorApi;
//
//	public FoodAdvisorClientApp foodAdvisorClient;
//
//	public MyStoreAdminApp myStoreAdmin;
//
//	public MyStoreStorefrontApp myStoreStorefront;
//
//	public PaymentCheckoutApp paymentCheckout;

	public PetClinicApplication petClinic;

	public TodoMvc todoMvc;

//	public UXPatternsApp uxPatterns;

	public JanillaWebsite(Properties configuration) {
		this.configuration = configuration;

		factory = new Factory();
		factory.setTypes(Util.getPackageClasses(getClass().getPackageName()).toList());
		factory.setSource(this);

		acmeDashboard = new AcmeDashboard(configuration);
//		acmeStore = new AcmeStoreApp(configuration);
		addressBook = new AddressBook(configuration);
		conduitBackend = new ConduitBackend(configuration);
		conduitFrontend = new ConduitFrontend(configuration);
//		eShopApi = new EShopApiApp(configuration);
//		eShopWeb = new EShopWebApp(configuration);
//		foodAdvisorApi = new FoodAdvisorApiApp(configuration);
//		foodAdvisorClient = new FoodAdvisorClientApp(configuration);
//		myStoreAdmin = new MyStoreAdminApp(configuration);
//		myStoreStorefront = new MyStoreStorefrontApp(configuration);
//		paymentCheckout = new PaymentCheckoutApp(configuration);
		petClinic = new PetClinicApplication(configuration);
		todoMvc = new TodoMvc(configuration);
//		uxPatterns = new UXPatternsApp(configuration);

		{
			var hb = factory.create(ApplicationHandlerBuilder.class);
			var h = hb.build();
			var hh = Map.ofEntries(
					Map.entry(configuration.getProperty("website.acmedashboard.host"), acmeDashboard.handler),
//					Map.entry(configuration.getProperty("website.acmestore.host"), acmeStore.handler),
					Map.entry(configuration.getProperty("website.address-book.host"), addressBook.handler),
					Map.entry(configuration.getProperty("website.conduit.backend.host"), conduitBackend.handler),
					Map.entry(configuration.getProperty("website.conduit.frontend.host"), conduitFrontend.handler),
//					Map.entry(configuration.getProperty("website.eshopweb.api.host"), eShopApi.handler),
//					Map.entry(configuration.getProperty("website.eshopweb.web.host"), eShopWeb.handler),
//					Map.entry(configuration.getProperty("website.foodadvisor.api.host"), foodAdvisorApi.handler),
//					Map.entry(configuration.getProperty("website.foodadvisor.client.host"), foodAdvisorClient.handler),
//					Map.entry(configuration.getProperty("website.mystore.admin.host"), myStoreAdmin.handler),
//					Map.entry(configuration.getProperty("website.mystore.storefront.host"), myStoreStorefront.handler),
//					Map.entry(configuration.getProperty("website.paymentcheckout.host"), paymentCheckout.handler),
					Map.entry(configuration.getProperty("website.petclinic.host"), petClinic.handler),
					Map.entry(configuration.getProperty("website.todomvc.host"), todoMvc.handler)
//					Map.entry(configuration.getProperty("website.uxpatterns.host"), uxPatterns.handler)
			);
			handler = x -> {
				var ex = (HttpExchange) x;
				var k = ex.getRequest().getAuthority();
				var j = k != null ? hh.get(k) : null;
				if (j == null)
					j = h;
				return j.handle(ex);
			};
		}
	}

	public JanillaWebsite application() {
		return this;
	}

	@Handle(method = "GET", path = "/")
	public Home home() {
		return new Home(x -> configuration.getProperty("website." + x + ".url"));
	}

	@Render(template = "home.html")
	public record Home(Function<String, String> demoUrl) {
	}
}
