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

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

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
			}
			var jw = new JanillaWebsite(pp);
			Server s;
			{
				var a = new InetSocketAddress(Integer.parseInt(jw.configuration.getProperty("website.server.port")));
				var kp = jw.configuration.getProperty("website.ssl.keystore.path");
				var kp2 = jw.configuration.getProperty("website.ssl.keystore.password");
				if (kp != null && kp.startsWith("~"))
					kp = System.getProperty("user.home") + kp.substring(1);
				SSLContext sc;
				try (var is = kp != null && kp.length() > 0 ? Files.newInputStream(Path.of(kp))
						: Net.class.getResourceAsStream("testkeys")) {
					sc = Net.getSSLContext(kp != null && kp.toLowerCase().endsWith(".p12") ? "PKCS12" : "JKS", is,
							(kp2 != null && kp2.length() > 0 ? kp2 : "passphrase").toCharArray());
				}
				var p = jw.factory.create(HttpProtocol.class,
						Map.of("handler", jw.handler, "sslContext", sc, "useClientMode", false));
				s = new Server(a, p);
			}
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
