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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.janilla.cms.Cms;
import com.janilla.http.HttpRequest;
import com.janilla.web.Handle;

@Handle(path = "/api/files")
public class FileApi {

	public Properties configuration;

	@Handle(method = "POST", path = "upload")
	public void create(HttpRequest request) throws IOException {
		for (var kv : Cms.files(request).entrySet()) {
			var ud = configuration.getProperty("janilla-website.upload.directory");
			if (ud.startsWith("~"))
				ud = System.getProperty("user.home") + ud.substring(1);
			var p = Path.of(ud);
			if (!Files.exists(p))
				Files.createDirectories(p);
			Files.write(p.resolve(kv.getKey()), kv.getValue());
		}
	}
}
