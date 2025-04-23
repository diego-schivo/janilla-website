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
package com.janilla.website2;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpProtocol;
import com.janilla.http.HttpRequest;
import com.janilla.reflect.Factory;
import com.janilla.reflect.Reflection;

public class CustomHttpProtocol extends HttpProtocol {

	public JanillaWebsite application;
//	public AcmeDashboard acmeDashboard;

//	public Properties configuration;

//	public Factory factory;

//	public Persistence persistence;

	public CustomHttpProtocol(HttpHandler handler, SSLContext sslContext, boolean useClientMode) {
		super(handler, sslContext, useClientMode);
	}

	@Override
	protected HttpExchange createExchange(HttpRequest request) {
		var a = application.application(request.getAuthority());
		var f = a == application ? application.factory : (Factory) Reflection.property(a.getClass(), "factory").get(a);
		return f.create(HttpExchange.class);
	}
}
