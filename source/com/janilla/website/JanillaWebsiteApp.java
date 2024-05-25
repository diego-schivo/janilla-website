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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.acmestore.AcmeStoreApp;
import com.janilla.conduit.backend.ConduitBackendApp;
import com.janilla.conduit.frontend.ConduitFrontendApp;
import com.janilla.eshopweb.api.EShopApiApp;
import com.janilla.eshopweb.web.EShopWebApp;
import com.janilla.foodadvisor.api.FoodAdvisorApiApp;
import com.janilla.foodadvisor.client.FoodAdvisorClientApp;
import com.janilla.http.HttpServer;
import com.janilla.mystore.admin.MyStoreAdminApp;
import com.janilla.mystore.storefront.MyStoreStorefrontApp;
import com.janilla.net.Net;
import com.janilla.payment.checkout.PaymentCheckoutApp;
import com.janilla.petclinic.PetClinicApplication;
import com.janilla.reflect.Factory;
import com.janilla.todomvc.TodoMVCApp;
import com.janilla.util.Lazy;
import com.janilla.util.Util;
import com.janilla.uxpatterns.UXPatternsApp;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.Render;
import com.janilla.web.WebHandler;

public class JanillaWebsiteApp {

	public static void main(String[] args) throws Exception {
		var a = new JanillaWebsiteApp();
		{
			var c = new Properties();
			try (var s = JanillaWebsiteApp.class.getResourceAsStream("configuration.properties")) {
				c.load(s);
				if (args.length > 0)
					c.load(Files.newInputStream(Path.of(args[0])));
			}
			a.configuration = c;
		}
		a.conduitBackend.get().getPersistence();

		var s = (CustomServer) a.getFactory().create(HttpServer.class);
		s.setExecutor(Executors.newFixedThreadPool(10));
		s.setIdleTimerPeriod(10 * 1000);
		s.setMaxIdleDuration(30 * 1000);
		s.setMaxMessageLength(512 * 1024);
		s.setPort(Integer.parseInt(a.configuration.getProperty("website.server.port")));
		{
			var p = a.configuration.getProperty("website.ssl.keystore.path");
			if (p != null && !p.isEmpty()) {
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				var q = a.configuration.getProperty("website.ssl.keystore.password");
				s.setSSLContext(Net.getSSLContext(Path.of(p), q.toCharArray()));
			}
		}
		s.setHandler(a.getHandler());
		s.run();
	}

	public Properties configuration;

	public JanillaWebsiteApp getApplication() {
		return this;
	}

	private Supplier<Factory> factory = Lazy.of(() -> {
		var f = new Factory();
		f.setTypes(Util.getPackageClasses(getClass().getPackageName()).toList());
		f.setSource(this);
		return f;
	});

	Supplier<AcmeStoreApp> acmeStore = Lazy.of(() -> {
		var a = new AcmeStoreApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<ConduitBackendApp> conduitBackend = Lazy.of(() -> {
		var a = new ConduitBackendApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<ConduitFrontendApp> conduitFrontend = Lazy.of(() -> {
		var a = new ConduitFrontendApp();
		a.configuration = configuration;
		return a;
	});

	Supplier<EShopApiApp> eShopApi = Lazy.of(() -> {
		var a = new EShopApiApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<EShopWebApp> eShopWeb = Lazy.of(() -> {
		var a = new EShopWebApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<FoodAdvisorApiApp> foodAdvisorApi = Lazy.of(() -> {
		var a = new FoodAdvisorApiApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<FoodAdvisorClientApp> foodAdvisorClient = Lazy.of(() -> {
		var a = new FoodAdvisorClientApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<MyStoreAdminApp> myStoreAdmin = Lazy.of(() -> {
		var a = new MyStoreAdminApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<MyStoreStorefrontApp> myStoreStorefront = Lazy.of(() -> {
		var a = new MyStoreStorefrontApp();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<PaymentCheckoutApp> paymentCheckout = Lazy.of(() -> {
		var a = new PaymentCheckoutApp();
		a.configuration = configuration;
		return a;
	});

	Supplier<PetClinicApplication> petClinic = Lazy.of(() -> {
		var a = new PetClinicApplication();
		a.configuration = configuration;
		a.getPersistence();
		return a;
	});

	Supplier<TodoMVCApp> todoMVC = Lazy.of(() -> new TodoMVCApp());

	Supplier<UXPatternsApp> uxPatterns = Lazy.of(() -> {
		var a = new UXPatternsApp();
		a.configuration = configuration;
		return a;
	});

	Supplier<WebHandler> handler = Lazy.of(() -> {
		var f = getFactory();
		var b = f.create(ApplicationHandlerBuilder.class);
		var h = b.build();

		var hh = Map.ofEntries(
				Map.entry(configuration.getProperty("website.acmestore.host"), getAcmeStore().getHandler()),
				Map.entry(configuration.getProperty("website.conduit.backend.host"), getConduitBackend().getHandler()),
				Map.entry(configuration.getProperty("website.conduit.frontend.host"),
						getConduitFrontend().getHandler()),
				Map.entry(configuration.getProperty("website.eshopweb.api.host"), getEShopApi().getHandler()),
				Map.entry(configuration.getProperty("website.eshopweb.web.host"), getEShopWeb().getHandler()),
				Map.entry(configuration.getProperty("website.foodadvisor.api.host"), getFoodAdvisorApi().getHandler()),
				Map.entry(configuration.getProperty("website.foodadvisor.client.host"),
						getFoodAdvisorClient().getHandler()),
				Map.entry(configuration.getProperty("website.mystore.admin.host"), getMyStoreAdmin().getHandler()),
				Map.entry(configuration.getProperty("website.mystore.storefront.host"),
						getMyStoreStorefront().getHandler()),
				Map.entry(configuration.getProperty("website.paymentcheckout.host"), getPaymentCheckout().getHandler()),
				Map.entry(configuration.getProperty("website.petclinic.host"), getPetClinic().getHandler()),
				Map.entry(configuration.getProperty("website.todomvc.host"), getTodoMVC().getHandler()),
				Map.entry(configuration.getProperty("website.uxpatterns.host"), getUxPatterns().getHandler()));

		return c -> {
			var l = c.getRequest().getHeaders();
			var i = l != null ? l.get("Host") : null;
			var j = i != null ? hh.get(i) : null;
			if (j == null)
				j = h;
			j.handle(c);
		};
	});

	public Factory getFactory() {
		return factory.get();
	}

	public AcmeStoreApp getAcmeStore() {
		return acmeStore.get();
	}

	public ConduitBackendApp getConduitBackend() {
		return conduitBackend.get();
	}

	public ConduitFrontendApp getConduitFrontend() {
		return conduitFrontend.get();
	}

	public EShopApiApp getEShopApi() {
		return eShopApi.get();
	}

	public EShopWebApp getEShopWeb() {
		return eShopWeb.get();
	}

	public FoodAdvisorApiApp getFoodAdvisorApi() {
		return foodAdvisorApi.get();
	}

	public FoodAdvisorClientApp getFoodAdvisorClient() {
		return foodAdvisorClient.get();
	}

	public MyStoreAdminApp getMyStoreAdmin() {
		return myStoreAdmin.get();
	}

	public MyStoreStorefrontApp getMyStoreStorefront() {
		return myStoreStorefront.get();
	}

	public PaymentCheckoutApp getPaymentCheckout() {
		return paymentCheckout.get();
	}

	public PetClinicApplication getPetClinic() {
		return petClinic.get();
	}

	public TodoMVCApp getTodoMVC() {
		return todoMVC.get();
	}

	public UXPatternsApp getUxPatterns() {
		return uxPatterns.get();
	}

	public WebHandler getHandler() {
		return handler.get();
	}

	@Handle(method = "GET", path = "/")
	public Home getHome() {
		return new Home(n -> configuration.getProperty("website." + n + ".url"));
	}

	@Render("home.html")
	public record Home(Function<String, String> demoUrl) {
	}
}
