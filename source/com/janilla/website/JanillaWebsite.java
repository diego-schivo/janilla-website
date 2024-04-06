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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.commerce.CommerceApp;
import com.janilla.conduit.backend.ConduitBackendApp;
import com.janilla.conduit.frontend.ConduitFrontendApp;
import com.janilla.eshopweb.api.EShopApiApp;
import com.janilla.eshopweb.web.EShopWebApp;
import com.janilla.http.HttpExchange;
import com.janilla.io.IO;
import com.janilla.net.Net;
import com.janilla.petclinic.PetClinicApplication;
import com.janilla.todomvc.TodoMVCApp;
import com.janilla.util.Lazy;
import com.janilla.uxpatterns.UXPatternsApp;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class JanillaWebsite {

	public static void main(String[] args) throws IOException {
		var a = new JanillaWebsite();
		{
			var c = new Properties();
			try (var s = JanillaWebsite.class.getResourceAsStream("configuration.properties")) {
				c.load(s);
				if (args.length > 0)
					c.load(Files.newInputStream(Path.of(args[0])));
			}
			a.setConfiguration(c);
		}
		a.conduitBackend.get().getPersistence();

		var s = new CustomHttpServer();
		s.setApplication(a);
		s.setExecutor(Executors.newFixedThreadPool(10));
		s.setIdleTimerPeriod(10 * 1000);
		s.setMaxIdleDuration(30 * 1000);
		s.setMaxMessageLength(512 * 1024);
		s.setPort(Integer.parseInt(a.getConfiguration().getProperty("website.server.port")));
		{
			var p = a.getConfiguration().getProperty("website.ssl.keystore.path");
			if (p != null && !p.isEmpty()) {
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				var q = a.getConfiguration().getProperty("website.ssl.keystore.password");
				s.setSSLContext(Net.getSSLContext(Path.of(p), q.toCharArray()));
			}
		}
		s.setHandler(a.getHandler());
		s.run();
	}

	Properties configuration;

	Supplier<CommerceApp> commerce = Lazy.of(() -> {
		var a = new CommerceApp();
		a.setConfiguration(configuration);
		a.getPersistence();
		return a;
	});

	Supplier<ConduitBackendApp> conduitBackend = Lazy.of(() -> {
		var a = new ConduitBackendApp();
		a.setConfiguration(configuration);
		return a;
	});

	Supplier<ConduitFrontendApp> conduitFrontend = Lazy.of(() -> {
		var a = new ConduitFrontendApp();
		a.setConfiguration(configuration);
		return a;
	});

	Supplier<EShopApiApp> eShopApi = Lazy.of(() -> {
		var a = new EShopApiApp();
		a.setConfiguration(configuration);
		a.getPersistence();
		return a;
	});

	Supplier<EShopWebApp> eShopWeb = Lazy.of(() -> {
		var a = new EShopWebApp();
		a.setConfiguration(configuration);
		a.getPersistence();
		return a;
	});

	Supplier<PetClinicApplication> petClinic = Lazy.of(() -> {
		var a = new PetClinicApplication();
		a.setConfiguration(configuration);
		a.getPersistence();
		return a;
	});

	Supplier<TodoMVCApp> todoMVC = Lazy.of(() -> new TodoMVCApp());

	Supplier<UXPatternsApp> uxPatterns = Lazy.of(() -> new UXPatternsApp());

	Supplier<IO.Consumer<HttpExchange>> handler = Lazy.of(() -> {
		var b = new ApplicationHandlerBuilder();
		b.setApplication(this);
		var h = b.build();

		var hh = Map.of(configuration.getProperty("website.commerce.host"), commerce.get().getHandler(),
				configuration.getProperty("website.conduit.backend.host"), conduitBackend.get().getHandler(),
				configuration.getProperty("website.conduit.frontend.host"), conduitFrontend.get().getHandler(),
				configuration.getProperty("website.eshopweb.api.host"), eShopApi.get().getHandler(),
				configuration.getProperty("website.eshopweb.web.host"), eShopWeb.get().getHandler(),
				configuration.getProperty("website.petclinic.host"), petClinic.get().getHandler(),
				configuration.getProperty("website.todomvc.host"), todoMVC.get().getHandler(),
				configuration.getProperty("website.uxpatterns.host"), uxPatterns.get().getHandler());

		return c -> {
			var l = c.getRequest().getHeaders();
			var i = l != null ? l.get("Host") : null;
			var j = i != null ? hh.get(i) : null;
			if (j == null)
				j = h;
			j.accept(c);
		};
	});

	public Properties getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public CommerceApp getCommerce() {
		return commerce.get();
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

	public PetClinicApplication getPetClinic() {
		return petClinic.get();
	}

	public TodoMVCApp getTodoMVC() {
		return todoMVC.get();
	}

	public UXPatternsApp getUxPatterns() {
		return uxPatterns.get();
	}

	public IO.Consumer<HttpExchange> getHandler() {
		return handler.get();
	}

	@Handle(method = "GET", path = "/")
	public Home getHome() {
		return new Home(n -> configuration.getProperty("website." + n + ".url"));
	}

	@Render(template = "home.html")
	public record Home(Function<String, String> demoUrl) {
	}
}
