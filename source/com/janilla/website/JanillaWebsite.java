/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
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

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.cms.Cms;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.Java;
import com.janilla.java.TypeResolver;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.net.Net;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Handle;
import com.janilla.web.Invocable;
import com.janilla.web.NotFoundException;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;
import com.janilla.web.Renderer;

public class JanillaWebsite {

	public static final AtomicReference<JanillaWebsite> INSTANCE = new AtomicReference<>();

	public static final Predicate<HttpExchange> DRAFTS = x -> ((CustomHttpExchange) x).sessionUser() != null;

	protected static final Pattern ADMIN = Pattern.compile("/admin(/.*)?");

	public static void main(String[] args) {
		try {
			JanillaWebsite a;
			{
				var f = new DiFactory(Stream.of(JanillaWebsite.class.getPackageName(), "com.janilla.web")
						.flatMap(x -> Java.getPackageClasses(x).stream()).toList(), INSTANCE::get);
				a = f.create(JanillaWebsite.class,
						Java.hashMap("diFactory", f, "configurationFile",
								args.length > 0 ? Path.of(
										args[0].startsWith("~") ? System.getProperty("user.home") + args[0].substring(1)
												: args[0])
										: null));
			}

			HttpServer s;
			{
				var kp = a.configuration.getProperty("janilla-website.ssl.keystore.path");
				var kp2 = a.configuration.getProperty("janilla-website.ssl.keystore.password");
				if (kp != null && kp.startsWith("~"))
					kp = System.getProperty("user.home") + kp.substring(1);
				SSLContext c;
				try (var x = kp != null && !kp.isEmpty() ? Files.newInputStream(Path.of(kp))
						: Net.class.getResourceAsStream("localhost")) {
					c = Net.getSSLContext(
							Map.entry(kp != null && kp.toLowerCase().endsWith(".p12") ? "PKCS12" : "JKS", x),
							(kp2 != null && !kp2.isEmpty() ? kp2 : "passphrase").toCharArray());
				}
				var p = Integer.parseInt(a.configuration.getProperty("janilla-website.server.port"));
				s = a.diFactory.create(HttpServer.class,
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected final Properties configuration;

	protected final Path configurationFile;

	protected final DataFetching dataFetching;

	protected final Path databaseFile;

	protected final DiFactory diFactory;

	protected final Predicate<HttpExchange> drafts = x -> {
		var u = x instanceof CustomHttpExchange y ? y.sessionUser() : null;
		return u != null;
	};

	protected final List<Path> files;

	protected final HttpHandler handler;

	protected final Map<String, Object> hostExample = new ConcurrentHashMap<>();

	protected final IndexFactory indexFactory;

	protected final List<Invocable> invocables;

	protected final Persistence persistence;

	protected final RenderableFactory renderableFactory;

	protected final TypeResolver typeResolver;

	public JanillaWebsite(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		this.configurationFile = configurationFile;
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
		configuration = diFactory.create(Properties.class, Collections.singletonMap("file", configurationFile));
		typeResolver = diFactory.create(DollarTypeResolver.class);

		{
			var p = configuration.getProperty("janilla-website.database.file");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			databaseFile = Path.of(p);
			var pb = diFactory.create(ApplicationPersistenceBuilder.class);
			persistence = pb.build();
		}

		dataFetching = diFactory.create(DataFetching.class);
		indexFactory = diFactory.create(IndexFactory.class);

		invocables = types().stream()
				.flatMap(x -> Arrays.stream(x.getMethods())
						.filter(y -> !Modifier.isStatic(y.getModifiers()) && !y.isBridge())
						.map(y -> new Invocable(x, y)))
				.toList();
		files = Stream.of("com.janilla.frontend", "com.janilla.cms", JanillaWebsite.class.getPackageName())
				.flatMap(x -> Java.getPackagePaths(x).stream().filter(Files::isRegularFile)).toList();
		renderableFactory = diFactory.create(RenderableFactory.class);
		{
			var f = diFactory.create(ApplicationHandlerFactory.class);
			handler = x -> {
				var a = application(((HttpExchange) x).request().getAuthority());
				var h2 = a == this ? (HttpHandler) y -> {
					var h = f.createHandler(Objects.requireNonNullElse(y.exception(), y.request()));
					if (h == null)
						throw new NotFoundException(y.request().getMethod() + " " + y.request().getTarget());
					return h.handle(y);
				} : (HttpHandler) Reflection.property(a.getClass(), "handler").get(a);
				return h2.handle(x);
			};
		}
	}

	public JanillaWebsite application() {
		return this;
	}

	public Properties configuration() {
		return configuration;
	}

	public DataFetching dataFetching() {
		return dataFetching;
	}

	public Path databaseFile() {
		return databaseFile;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public Predicate<HttpExchange> drafts() {
		return drafts;
	}

	public List<Path> files() {
		return files;
	}

	public HttpHandler handler() {
		return handler;
	}

	public IndexFactory indexFactory() {
		return indexFactory;
	}

	public List<Invocable> invocables() {
		return invocables;
	}

	public Persistence persistence() {
		return persistence;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public TypeResolver typeResolver() {
		return typeResolver;
	}

	public Collection<Class<?>> types() {
		return diFactory.types();
	}

	public Object application(String authority) {
		var s = "." + configuration.getProperty("janilla-website.authority");
		if (!authority.endsWith(s))
			return application();
		return hostExample.computeIfAbsent(authority.substring(0, authority.length() - s.length()), k -> {
			Example x;
			{
				var c = persistence.crud(Example.class);
				x = c.read(c.find("demo", k));
			}
			if (x != null)
				try {
					var c = Class.forName(x.application());
					var f = new DiFactory(Java.getPackageClasses(c.getPackageName()),
							((AtomicReference<?>) c.getDeclaredField("INSTANCE").get(null))::get);
					return f.create(c, Java.hashMap("diFactory", f, "configurationFile",
							Optional.ofNullable(configurationFile).orElseGet(() -> {
								try {
									return Path
											.of(JanillaWebsite.class.getResource("configuration.properties").toURI());
								} catch (URISyntaxException e) {
									throw new RuntimeException(e);
								}
							})));
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			return this;
		});
	}

//	@Handle(method = "GET", path = "((?!/api/)/[\\w\\d/-]*)")
//	public Index index(String path, CustomHttpExchange exchange) {
//		switch (path) {
//		case "/admin":
//			if (exchange.sessionEmail() == null) {
//				var rs = exchange.response();
//				rs.setStatus(307);
//				rs.setHeaderValue("cache-control", "no-cache");
//				rs.setHeaderValue("location", "/admin/login");
//				return null;
//			}
//		case "/admin/login":
//			if (persistence.crud(User.class).count() == 0) {
//				var rs = exchange.response();
//				rs.setStatus(307);
//				rs.setHeaderValue("cache-control", "no-cache");
//				rs.setHeaderValue("location", "/admin/create-first-user");
//				return null;
//			}
//		}
//		var m = ADMIN.matcher(path);
//		if (m.matches())
//			return new Index("/admin.css", Map.of());
//		Map<String, Object> m3 = new LinkedHashMap<>();
//		m3.put("authority", configuration.getProperty("janilla-website.authority"));
//		m3.put("/api/header", persistence.crud(Header.class).read(1L));
//		m3.put("/api/page", persistence.crud(Page.class).read(1L));
//		m3.put("/api/footer", persistence.crud(Footer.class).read(1L));
//		return new Index("/style.css", m3);
//	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Map<String, Map<String, Object>>> schema() {
		return Cms.schema(Data.class);
	}

//	@Render(template = "index.html")
//	public record Index(String href, @Render(renderer = DataRenderer.class) Map<String, Object> data) {
//	}
//
//	public static class DataRenderer<T> extends Renderer<T> {
//
//		@Override
//		public String apply(T value) {
//			return Json.format(INSTANCE.get().diFactory.create(ReflectionJsonIterator.class,
//					Map.of("object", value, "includeType", true)));
//		}
//	}
}
