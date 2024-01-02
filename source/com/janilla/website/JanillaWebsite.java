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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.janilla.conduit.backend.ConduitBackend;
import com.janilla.conduit.frontend.ConduitFrontend;
import com.janilla.http.ExchangeContext;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.io.IO;
import com.janilla.net.Net;
import com.janilla.util.Lazy;
import com.janilla.util.Util;
import com.janilla.web.DelegatingHandlerFactory;
import com.janilla.web.HandleException;
import com.janilla.web.Handler;
import com.janilla.web.HandlerFactory;
import com.janilla.web.MethodArgumentsResolver;
import com.janilla.web.MethodHandlerFactory;
import com.janilla.web.NotFoundException;
import com.janilla.web.Render;
import com.janilla.web.ResourceHandlerFactory;
import com.janilla.web.TemplateHandlerFactory;
import com.janilla.web.ToEndpointInvocation;
import com.janilla.web.ToResourceStream;
import com.janilla.web.ToTemplateReader;

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
//		c.list(System.out);
		w.backend.get().populate();
		w.serve();
	}

	Properties configuration;

	Supplier<ConduitBackend> backend = Lazy.of(() -> {
		var b = new ConduitBackend();
		b.setConfiguration(configuration);
		return b;
	});

	Supplier<ConduitFrontend> frontend = Lazy.of(() -> {
		var f = new ConduitFrontend();
		f.setConfiguration(configuration);
		return f;
	});

	Supplier<HttpServer> httpServer = Lazy.of(() -> {
		var s = new HttpServer();
//		s.setExecutor(Runnable::run);
		s.setExecutor(Executors.newFixedThreadPool(5));
		s.setIdleTimerPeriod(10 * 1000);
		s.setMaxIdleDuration(30 * 1000);
		s.setMaxMessageLength(512 * 1024);
		s.setPort(Integer.parseInt(configuration.getProperty("website.http.port")));
		{
			var p = configuration.getProperty("website.ssl.keystore.path");
			if (p != null && !p.isEmpty()) {
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				var q = configuration.getProperty("website.ssl.keystore.password");
				var c = Net.getSSLContext(Path.of(p), q.toCharArray());
				s.setSSLContext(c);
			}
		}
		return s;
	});

	Supplier<HandlerFactory> handlerFactory = Lazy.of(() -> {
		var b = new DelegatingHandlerFactory();
		b.setToHandler(o -> {
			var g = switch (o) {
			case HttpRequest q -> {
				var h = q.getHeaders().get("Host");
				if (h.equals(configuration.getProperty("website.demo.backend.host")))
					yield backend.get().getHandlerFactory();
				if (h.equals(configuration.getProperty("website.demo.frontend.host")))
					yield frontend.get().getHandlerFactory();
				yield null;
			}
			case Exception e -> backend.get().getHandlerFactory();
			default -> null;
			};
			return g != null ? g.createHandler(o) : null;
		});

		var l = Thread.currentThread().getContextClassLoader();
		var x = Stream.<Path>builder();
		var y = Stream.<Class<?>>builder();
		IO.packageFiles(JanillaWebsite.class.getPackageName(), l, f -> {
			x.add(f);
			var z = Util.getClass(f);
			if (z != null)
				y.add(z);
		});
		var f = x.build().toArray(Path[]::new);
		var c = y.build().toArray(Class[]::new);

		var i = new ToEndpointInvocation() {

			@Override
			protected Object getInstance(Class<?> c) {
				if (c == JanillaWebsite.class)
					return JanillaWebsite.this;
				return super.getInstance(c);
			}
		};
		i.setClasses(c);
		var f1 = new MethodHandlerFactory();
		f1.setToInvocation(i);
		f1.setArgumentsResolver(new MethodArgumentsResolver());
		var f2 = new TemplateHandlerFactory();
		{
			var s = new ToTemplateReader.Simple();
			s.setClass1(JanillaWebsite.class);
			s.setClasses(c);
			f2.setToReader(s);
		}
		var f3 = new ResourceHandlerFactory();
		{
			var s = new ToResourceStream.Simple();
			s.setPaths(f);
			f3.setToInputStream(s);
		}

		var f0 = new DelegatingHandlerFactory();
		{
			var a = new HandlerFactory[] { b, f1, f2, f3 };
			f0.setToHandler(o -> {
				if (a != null)
					for (var g : a) {
						var h = g.createHandler(o);
						if (h != null)
							return h;
					}
				return null;
			});
		}
		f1.setRenderFactory(f0);
		f2.setIncludeFactory(f0);
		return f0;
	});

	public Properties getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	@Handler(value = "/", method = "GET")
	public Home getHome() {
		var u = configuration.getProperty("website.demo.frontend.url");
		return new Home(u);
	}

	public void serve() throws IOException {
		httpServer.get().serve(c -> {
			var e = c.getException();
			var o = e != null ? e : c.getRequest();
			var h = handlerFactory.get().createHandler(o);
			if (h == null)
				throw new NotFoundException();
			h.handle(c);
		});
	}

	@Render("home.html")
	public record Home(String demoUrl) {
	}

	class HttpServer extends com.janilla.http.HttpServer {

		@Override
		protected void handle(HttpHandler handler, HttpRequest request, HttpResponse response) throws IOException {
			var h = request.getHeaders().get("Host");
			var c = h.equals(configuration.getProperty("website.demo.backend.host"))
					? backend.get().newExchangeContext()
					: new ExchangeContext();
			c.setRequest(request);
			c.setResponse(response);
			Exception e;
			try {
				handler.handle(c);
				e = null;
			} catch (UncheckedIOException x) {
				e = x.getCause();
			} catch (HandleException x) {
				e = x.getCause();
			} catch (Exception x) {
				e = x;
			}
			if (e != null) {
				e.printStackTrace();
				c.setException(e);
				handler.handle(c);
			}
		}

	}
}
