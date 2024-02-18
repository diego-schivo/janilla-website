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
import java.util.function.Supplier;

import com.janilla.conduit.backend.ConduitBackend;
import com.janilla.conduit.frontend.ConduitFrontend;
import com.janilla.eshopweb.api.EShopApiApp;
import com.janilla.eshopweb.web.EShopWebApp;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.io.IO;
import com.janilla.net.Net;
import com.janilla.petclinic.PetClinicApplication;
import com.janilla.todomvc.TodoMVC;
import com.janilla.util.Lazy;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class JanillaWebsite {

	public static void main(String[] args) throws IOException {
		var c = new Properties();
		try (var s = JanillaWebsite.class.getResourceAsStream("configuration.properties")) {
			c.load(s);
			if (args.length > 0)
				c.load(Files.newInputStream(Path.of(args[0])));
		}

		var w = new JanillaWebsite();
		w.setConfiguration(c);
		w.conduitBackend.get().populate();

		var s = new CustomHttpServer();
		s.website = w;
		s.setExecutor(Executors.newFixedThreadPool(5));
		s.setIdleTimerPeriod(10 * 1000);
		s.setMaxIdleDuration(30 * 1000);
		s.setMaxMessageLength(512 * 1024);
		s.setPort(Integer.parseInt(c.getProperty("website.http.port")));
		{
			var p = c.getProperty("website.ssl.keystore.path");
			if (p != null && !p.isEmpty()) {
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				var q = c.getProperty("website.ssl.keystore.password");
				s.setSSLContext(Net.getSSLContext(Path.of(p), q.toCharArray()));
			}
		}
		s.setHandler(w.getHandler());
		s.run();
	}

	Properties configuration;

	Supplier<ConduitBackend> conduitBackend = Lazy.of(() -> {
		var a = new ConduitBackend();
		a.setConfiguration(configuration);
		return a;
	});

	Supplier<ConduitFrontend> conduitFrontend = Lazy.of(() -> {
		var a = new ConduitFrontend();
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

	Supplier<TodoMVC> todoMVC = Lazy.of(() -> new TodoMVC());

	Supplier<IO.Consumer<HttpExchange>> handler = Lazy.of(() -> {
		var b = new ApplicationHandlerBuilder();
		b.setApplication(JanillaWebsite.this);
		var w = b.build();

		var hh = Map.of(configuration.getProperty("website.conduit.backend.host"), conduitBackend.get().getHandler(),
				configuration.getProperty("website.conduit.frontend.host"), conduitFrontend.get().getHandler(),
				configuration.getProperty("website.eshopweb.api.host"), eShopApi.get().getHandler(),
				configuration.getProperty("website.eshopweb.web.host"), eShopWeb.get().getHandler(),
				configuration.getProperty("website.petclinic.host"), petClinic.get().getHandler(),
				configuration.getProperty("website.todomvc.host"), todoMVC.get().getHandler());

		return c -> {
			var o = c.getException() != null ? c.getException() : c.getRequest();
			var h = switch (o) {
			case HttpRequest q -> {
				var l = q.getHeaders();
				var m = l != null ? l.get("Host") : null;
				var z = m != null ? hh.get(m) : null;
				yield z != null ? z : w;
			}
			case Exception e -> conduitBackend.get().getHandler();
			default -> null;
			};
			h.accept(c);
		};
	});

	public Properties getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public ConduitBackend getConduitBackend() {
		return conduitBackend.get();
	}

	public ConduitFrontend getConduitFrontend() {
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

	public TodoMVC getTodoMVC() {
		return todoMVC.get();
	}

	public IO.Consumer<HttpExchange> getHandler() {
		return handler.get();
	}

	@Handle(method = "GET", uri = "/")
	public Home getHome() {
		var u = configuration.getProperty("website.demo.frontend.url");
		return new Home(u);
	}

	@Render(template = "home.html")
	public record Home(String demoUrl) {
	}
}
