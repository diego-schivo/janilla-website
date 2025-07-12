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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import com.janilla.cms.Cms;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.json.Json;
import com.janilla.json.MapAndType;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.net.Net;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Factory;
import com.janilla.reflect.Reflection;
import com.janilla.util.Util;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;
import com.janilla.web.Renderer;

public class JanillaWebsite {

	public static JanillaWebsite INSTANCE;

	public static final Predicate<HttpExchange> DRAFTS = x -> ((CustomHttpExchange) x).sessionUser() != null;

	protected static final Pattern ADMIN = Pattern.compile("/admin(/.*)?");

	public static void main(String[] args) {
		try {
			var pp = new Properties();
			try (var s1 = JanillaWebsite.class.getResourceAsStream("configuration.properties")) {
				pp.load(s1);
				if (args.length > 0) {
					var p = args[0];
					if (p.startsWith("~"))
						p = System.getProperty("user.home") + p.substring(1);
					try (var s2 = Files.newInputStream(Path.of(p))) {
						pp.load(s2);
					}
				}
			}
			INSTANCE = new JanillaWebsite(pp);
			HttpServer s;
			{
				var kp = INSTANCE.configuration.getProperty("janilla-website.ssl.keystore.path");
				var kp2 = INSTANCE.configuration.getProperty("janilla-website.ssl.keystore.password");
				if (kp != null && kp.startsWith("~"))
					kp = System.getProperty("user.home") + kp.substring(1);
				SSLContext sc;
				try (var is = kp != null && kp.length() > 0 ? Files.newInputStream(Path.of(kp))
						: Net.class.getResourceAsStream("testkeys")) {
					sc = Net.getSSLContext(kp != null && kp.toLowerCase().endsWith(".p12") ? "PKCS12" : "JKS", is,
							(kp2 != null && kp2.length() > 0 ? kp2 : "passphrase").toCharArray());
				}
				s = INSTANCE.factory.create(HttpServer.class, Map.of("sslContext", sc, "handler", INSTANCE.handler));
			}
			var p = Integer.parseInt(INSTANCE.configuration.getProperty("janilla-website.server.port"));
			s.serve(new InetSocketAddress(p));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Properties configuration;

	public Path databaseFile;

	public Factory factory;

	public HttpHandler handler;

	public Persistence persistence;

	public RenderableFactory renderableFactory;

	public MapAndType.TypeResolver typeResolver;

	public Set<Class<?>> types;

	protected final Map<String, Object> hostExample = new ConcurrentHashMap<>();

	public JanillaWebsite(Properties configuration) {
		this.configuration = configuration;
		types = Util.getPackageClasses(getClass().getPackageName()).collect(Collectors.toSet());
		factory = new Factory(types, this);
		typeResolver = factory.create(MapAndType.DollarTypeResolver.class);
		{
			var p = configuration.getProperty("janilla-website.database.file");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			databaseFile = Path.of(p);
			var pb = factory.create(ApplicationPersistenceBuilder.class);
			persistence = pb.build();
		}
		renderableFactory = new RenderableFactory();
		{
			var h0 = factory.create(ApplicationHandlerBuilder.class).build();
			handler = x -> {
				var a = application(((HttpExchange) x).getRequest().getAuthority());
				var h = a == this ? h0 : (HttpHandler) Reflection.property(a.getClass(), "handler").get(a);
				return h.handle(x);
			};
		}
	}

	public JanillaWebsite application() {
		return this;
	}

	public Object application(String authority) {
		var s = "." + configuration.getProperty("janilla-website.authority");
		if (!authority.endsWith(s))
			return application();
		return hostExample.computeIfAbsent(authority.substring(0, authority.length() - s.length()), k -> {
			var c = persistence.crud(Example.class);
			var x = c.read(c.find("demo", k));
			if (x != null)
				try {
					return Class.forName(x.application()).getConstructor(Properties.class).newInstance(configuration);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			return this;
		});
	}

	@Handle(method = "GET", path = "((?!/api/)/[\\w\\d/-]*)")
	public Index index(String path, CustomHttpExchange exchange) {
		switch (path) {
		case "/admin":
			if (exchange.sessionEmail() == null) {
				var rs = exchange.getResponse();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/admin/login");
				return null;
			}
		case "/admin/login":
			if (persistence.crud(User.class).count() == 0) {
				var rs = exchange.getResponse();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/admin/create-first-user");
				return null;
			}
		}
		var m = ADMIN.matcher(path);
		if (m.matches())
			return new Index("/admin.css", Map.of());
		Map<String, Object> m3 = new LinkedHashMap<>();
		m3.put("authority", configuration.getProperty("janilla-website.authority"));
		m3.put("/api/header", persistence.crud(Header.class).read(1L));
		m3.put("/api/page", persistence.crud(Page.class).read(1L));
		m3.put("/api/footer", persistence.crud(Footer.class).read(1L));
		return new Index("/style.css", m3);
	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Map<String, Map<String, Object>>> schema() {
		return Cms.schema(Data.class);
	}

	@Render(template = "index.html")
	public record Index(String href, @Render(renderer = DataRenderer.class) Map<String, Object> data) {
	}

	public static class DataRenderer<T> extends Renderer<T> {

		@Override
		public String apply(T value) {
			var tt = INSTANCE.factory.create(ReflectionJsonIterator.class);
			tt.setObject(value);
			tt.setIncludeType(true);
			return Json.format(tt);
		}
	}
}
