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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.cms.CmsFrontend;
import com.janilla.frontend.Frontend;

public class IndexFactory {

	protected final Properties configuration;

	protected final DataFetching dataFetching;

	public IndexFactory(Properties configuration, DataFetching dataFetching) {
		this.configuration = configuration;
		this.dataFetching = dataFetching;
	}

	public Index index(CustomHttpExchange exchange) {
		return new Index(imports(), configuration.getProperty("website-template.api.url"), state(exchange));
	}

	protected Map<String, Object> state(CustomHttpExchange exchange) {
		var x = new LinkedHashMap<String, Object>();
		x.put("user", exchange.sessionUser());
		x.put("header", dataFetching.header());
		x.put("footer", dataFetching.footer());
		x.put("authority", configuration.getProperty("janilla-website.authority"));
		return x;
	}

	protected Map<String, String> imports() {
		class A {
			private static Map<String, String> m;
		}
		if (A.m == null)
			synchronized (this) {
				if (A.m == null) {
					A.m = new LinkedHashMap<String, String>();
					Frontend.putImports(A.m);
					CmsFrontend.putImports(A.m);
					Stream.of("admin").forEach(x -> A.m.put(x, "/custom-" + x + ".js"));
					Stream.of("app", "example-apps", "features", "hero", "lucide-icon", "not-found", "page")
							.forEach(x -> A.m.put(x, "/" + x + ".js"));
				}
			}
		return A.m;
	}
}
