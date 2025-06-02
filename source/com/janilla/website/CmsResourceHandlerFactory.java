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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.janilla.http.HeaderField;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.web.WebHandlerFactory;

public class CmsResourceHandlerFactory implements WebHandlerFactory {

	public Properties configuration;

	@Override
	public HttpHandler createHandler(Object object, HttpExchange exchange) {
		var p = object instanceof HttpRequest x ? x.getPath() : null;
		var n = p != null && p.startsWith("/images/") ? p.substring("/images/".length()) : null;
		if (n == null)
			return null;

		var ud = configuration.getProperty("janilla-website.upload.directory");
		if (ud.startsWith("~"))
			ud = System.getProperty("user.home") + ud.substring(1);
		var f = Path.of(ud).resolve(n);
		return Files.exists(f) ? ex -> {
			handle(f, ex.getResponse());
			return true;
		} : null;
	}

	protected static void handle(Path file, HttpResponse response) {
		response.setStatus(200);

		var hh = response.getHeaders();
		hh.add(new HeaderField("cache-control", "max-age=3600"));
		var n = file.getFileName().toString();
		switch (n.substring(n.lastIndexOf('.') + 1)) {
		case "ico":
			hh.add(new HeaderField("content-type", "image/x-icon"));
			break;
		case "svg":
			hh.add(new HeaderField("content-type", "image/svg+xml"));
			break;
		}
		try {
			hh.add(new HeaderField("content-length", String.valueOf(Files.size(file))));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (var in = Files.newInputStream(file);
				var out = Channels.newOutputStream((WritableByteChannel) response.getBody())) {
			in.transferTo(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
