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

import java.util.Collection;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.json.DollarTypeResolver;
import com.janilla.json.TypeResolver;
import com.janilla.reflect.ClassAndMethod;
import com.janilla.web.HandleException;
import com.janilla.web.MethodHandlerFactory;
import com.janilla.web.RenderableFactory;

public class CustomMethodHandlerFactory extends MethodHandlerFactory {

	protected static final Set<String> GUEST_POST = Set.of("/api/users/first-register", "/api/users/forgot-password",
			"/api/users/login", "/api/users/reset-password");

	public Properties configuration;

	public DollarTypeResolver typeResolver;

	public CustomMethodHandlerFactory(Collection<ClassAndMethod> methods, Function<Class<?>, Object> targetResolver,
			Comparator<Invocation> invocationComparator, RenderableFactory renderableFactory,
			HttpHandlerFactory rootFactory) {
		super(methods, targetResolver, invocationComparator, renderableFactory, rootFactory);
	}

	@Override
	protected boolean handle(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.request();
		if (rq.getPath().startsWith("/api/") && !rq.getMethod().equals("GET")) {
			if (!GUEST_POST.contains(rq.getPath()))
				((CustomHttpExchange) exchange).requireSessionEmail();
		}

		if (Boolean.parseBoolean(configuration.getProperty("janilla-website.live-demo")))
			if (!rq.getMethod().equals("GET"))
				throw new HandleException(new MethodBlockedException());

//		if (rq.getPath().startsWith("/api/"))
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		return super.handle(invocation, exchange);
	}

	@Override
	protected TypeResolver resolver(Class<? extends TypeResolver> class0) {
		if (class0 == DollarTypeResolver.class)
			return typeResolver;
		return super.resolver(class0);
	}
}
